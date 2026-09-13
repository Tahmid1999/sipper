package io.github.tahmid1999.sipper.app.ui.audit

import io.github.tahmid1999.sipper.audit.Effect
import io.github.tahmid1999.sipper.audit.Provenance
import io.github.tahmid1999.sipper.audit.Verdict

/**
 * AUDIT's screen state types. SCREENS-AUDIT-PROBES §3 says these live in docs/ARCHITECTURE.md;
 * they do not (verified - reported in plan-m8.md), so they are declared here from the behaviour
 * the screen docs specify.
 */

/** One read route's outcome. The digest is sha256 over the normalised key=token set (§1.2). */
data class RouteReading(
    val name: String,           // "Resources.getSystem()" | "getResourcesForApplication" | "PowerProfile reflection"
    val resolved: String,       // resource id as 8-digit hex + type/name, or the class, or the path
    val keyCount: Int,
    val digest: String?,        // first 8 hex of sha256; null when the route did not return
    val readMs: Long?,          // null on failure; the cell prints the failure word instead
    val failed: String? = null, // "blocked" | "absent" | "throw"
)

/** The agreement badge's inputs. The denominator is always routes attempted (§1.2). */
data class Agreement(
    val attempted: Int,
    val agreed: Int,
    val blocked: Int,
) {
    val label: String = "$attempted routes · $agreed agree · $blocked blocked"
}

/** One key row: what the AUDIT table renders, verdict computed by :audit. */
data class AuditRow(
    val key: String,
    val verdict: Verdict,
    val literalToken: String?,    // the XML token as written; null when absent
    val routesAgree: Boolean,     // drives `=` / `≠`
    val from: String?,           // "res" | "back-fill" | null -> `—`
    val effect: Effect,
    val calculator: String?,      // group header; null when NOT_READ
    val provenance: Provenance,
    val reflectionValue: Double?, // the cross-check, recorded, never authoritative
)

/** The whole screen. Loading is real: no Reading is ever restored from a bundle (FLOWS §6.4). */
sealed interface AuditUiState {
    data object Loading : AuditUiState
    data class Ready(
        val routes: List<RouteReading>,
        val agreement: Agreement,
        val rows: List<AuditRow>,
        val declaredKeyCount: Int,
        val schemaEra: String,          // "pre-lollipop(20) / modern(34)" per §1.1 footer
    ) : AuditUiState
    data object NoProfile : AuditUiState
}
