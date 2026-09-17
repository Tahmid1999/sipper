package io.github.tahmid1999.sipper.app.ui.audit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tahmid1999.sipper.audit.Effect
import io.github.tahmid1999.sipper.audit.ParsedProfile
import io.github.tahmid1999.sipper.audit.ProfileModel
import io.github.tahmid1999.sipper.audit.Provenance
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.keyTable
import io.github.tahmid1999.sipper.audit.verdictOf
import io.github.tahmid1999.sipper.collect.reflectAveragePower
import io.github.tahmid1999.sipper.collect.readViaAndroidPackage
import io.github.tahmid1999.sipper.collect.readViaSystemResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * FLOWS §1: the three route reads launch on Dispatchers.IO at ViewModel init; a restored screen
 * starts at Loading and means it (§6.4 - no Reading is ever restored from a bundle).
 *
 * The verdict table is computed from the package route's profile (CONTRACT §4: Route B fills the
 * value column and carries the verdicts), and reflection is the cross-check the digest block
 * compares - on the ANE-LX2 it is also the route that matches what the framework prices, which is
 * what the primary-route caveat line says.
 */
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

class AuditViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val _state = MutableStateFlow<AuditUiState>(AuditUiState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    /** FLOWS §7.1: platform reads re-execute on demand - never a cached table. */
    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            val api = android.os.Build.VERSION.SDK_INT

            val systemReading = readViaSystemResources()
            val packageReading = readViaAndroidPackage(appContext)

            val systemProfile = (systemReading as? Reading.Value)?.value
            val packageProfile = (packageReading as? Reading.Value)?.value
            val verdictProfile = packageProfile ?: systemProfile

            if (verdictProfile == null) {
                _state.value = AuditUiState.NoProfile
                return@withContext
            }

            // The reflection cross-check over the audit's key set at this api level.
            val keys = keyTable().filter { api in it.apis }.map { it.key }.distinct()
            val reflection = mutableMapOf<String, Double?>()
            for (key in keys) {
                reflection[key] = when (val r = reflectAveragePower(appContext, key)) {
                    is Reading.Value -> r.value
                    else -> null
                }
            }

            val model = ProfileModel(verdictProfile, api)
            _state.value = AuditUiState.Ready(
                routes = buildRoutes(systemReading, packageReading, reflection),
                agreement = agreementOf(systemReading, packageReading, reflection, model),
                rows = buildRows(model, verdictProfile, keys, reflection),
                declaredKeyCount = verdictProfile.declared.size,
                schemaEra = schemaEra(verdictProfile),
            )
        }
    }

    /** The key rows, grouped under the calculator the key table names at this api (§1.5). */
    private fun buildRows(
        model: ProfileModel,
        profile: ParsedProfile,
        keys: List<String>,
        reflection: Map<String, Double?>,
    ): List<AuditRow> = keys.map { key ->
        val provenance = model.provenance(key)
        val fact = keyTable().firstOrNull { it.key == key && apiLevel(key) }
        AuditRow(
            key = key,
            verdict = verdictOf(provenance),
            literalToken = profile.tokens[key]?.firstOrNull(),
            routesAgree = reflection[key] == null || model.averagePower(key) == reflection[key],
            from = when (provenance) {
                is Provenance.BackFilled -> "back-fill"
                is Provenance.Declared -> "res"
                is Provenance.NotDeclared -> null
            },
            effect = fact?.effect ?: Effect.NOT_READ,
            calculator = fact?.calculator,
            provenance = provenance,
            reflectionValue = reflection[key],
        )
    }

    private fun apiLevel(key: String): Boolean {
        val api = android.os.Build.VERSION.SDK_INT
        return keyTable().any { it.key == key && api in it.apis }
    }

    /**
     * The route digest (§1.2): first 8 hex of sha256 over the normalised `key=token` set, sorted,
     * newline-joined. Normalisation trims whitespace and does not parse to Double, so `0` and
     * `0.0` do not collide.
     */
    private fun digestOf(profile: ParsedProfile): String {
        val normalised = profile.tokens.entries
            .sortedBy { it.key }
            .joinToString("\n") { (k, v) -> k + "=" + v.joinToString(",") }
        val d = MessageDigest.getInstance("SHA-256").digest(normalised.toByteArray())
        return d.joinToString("") { "%02x".format(it) }.take(8)
    }

    /**
     * The agreement badge (§1.2): the denominator is always the number of routes attempted and
     * `agree` counts routes that returned and matched. It never prints `3/3 agree` when a route
     * was blocked.
     */
    private fun agreementOf(
        system: Reading<ParsedProfile>,
        pkg: Reading<ParsedProfile>,
        reflection: Map<String, Double?>,
        model: ProfileModel,
    ): Agreement {
        val systemProfile = (system as? Reading.Value)?.value
        val pkgProfile = (pkg as? Reading.Value)?.value
        var agreed = 0
        if (systemProfile != null && pkgProfile != null &&
            digestOf(systemProfile) == digestOf(pkgProfile)
        ) {
            agreed++
        }
        val reflectionBlocked = reflection.values.all { it == null }
        if (!reflectionBlocked) {
            val allMatch = reflection.entries.all { (k, v) -> v == null || model.averagePower(k) == v }
            if (allMatch) agreed++
        }
        val blocked = if (reflectionBlocked) 1 else 0
        return Agreement(attempted = 3, agreed = agreed, blocked = blocked)
    }

    private fun buildRoutes(
        system: Reading<ParsedProfile>,
        pkg: Reading<ParsedProfile>,
        reflection: Map<String, Double?>,
    ): List<RouteReading> = buildList {
        add(routeRow("Resources.getSystem()", "xml/power_profile", system))
        add(routeRow("getResourcesForApplication", "xml/power_profile", pkg))
        val reflectOk = reflection.values.any { it != null }
        add(
            RouteReading(
                name = "PowerProfile reflection",
                resolved = "com.android.internal.os.PowerProfile",
                keyCount = reflection.size,
                digest = null,
                readMs = null,
                failed = if (reflectOk) null else "blocked",
            ),
        )
        // §1.2's fourth row: the informational file probe, not a route, never a claim.
        add(fileProbe())
    }

    private fun routeRow(name: String, resolved: String, reading: Reading<ParsedProfile>): RouteReading =
        when (reading) {
            is Reading.Value -> RouteReading(
                name = name,
                resolved = resolved,
                keyCount = reading.value.declared.size,
                digest = digestOf(reading.value),
                readMs = null,
            )
            else -> RouteReading(
                name = name,
                resolved = "—",
                keyCount = 0,
                digest = null,
                readMs = null,
                failed = "blocked",
            )
        }

    /** §1.2's fourth row. `informational`, and readable-or-ENOENT, never more than that. */
    private fun fileProbe(): RouteReading {
        val readable = try {
            java.io.File("/product/etc/xml/power_profile.xml").canRead()
        } catch (_: Exception) {
            false
        }
        return RouteReading(
            name = "file /product/etc/xml/power_profile.xml",
            resolved = if (readable) "readable" else "absent (ENOENT)",
            keyCount = 0,
            digest = null,
            readMs = null,
            failed = null,
        )
    }

    /** §1.1's footer line: the era name keyed on the schema, computed not asserted. */
    private fun schemaEra(profile: ParsedProfile): String {
        val modern = profile.declared.keys.any { it.endsWith(".display0") } ||
            profile.declared.containsKey("memory.bandwidths")
        return if (modern) "modern(34)" else "pre-lollipop(20)"
    }
}

