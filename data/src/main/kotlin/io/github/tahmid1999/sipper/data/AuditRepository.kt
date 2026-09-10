package io.github.tahmid1999.sipper.`data`

import io.github.tahmid1999.sipper.audit.ProfileModel
import io.github.tahmid1999.sipper.audit.Provenance
import io.github.tahmid1999.sipper.audit.Verdict
import io.github.tahmid1999.sipper.audit.keyTable

public class AuditRepository(private val database: SipperDatabase) {

    public fun save(
        model: ProfileModel,
        capturedAt: Long,
        bootCount: Long,
        fingerprint: String,
        routesAgree: Boolean,
        routeReflectionOk: Boolean,
    ): Long {
        val queries = database.keyResultQueries

        // The four route/readback columns are :collect's device facts and :collect does not exist,
        // so they are null: "not recorded", never a placeholder.
        queries.insertAudit(
            captured_at = capturedAt,
            boot_count = bootCount,
            fingerprint = fingerprint,
            api_level = model.apiLevel.toLong(),
            route_system_sha = null,
            route_package_sha = null,
            route_reflection_ok = if (routeReflectionOk) 1L else 0L,
            routes_agree = if (routesAgree) 1L else 0L,
            disagreement = null,
            declared_key_count = model.declared.size.toLong(),
            battery_capacity_declared = model.declared["battery.capacity"]?.first(),
            charge_counter_uah = null,
        )

        val auditId = queries.lastAuditId().executeAsOne()

        val table = keyTable()
        for ((key, verdict) in model.audit().verdicts) {
            queries.insertKeyResult(
                audit_id = auditId,
                key = key,
                verdict = verdict.name,
                declared_value = model.declared[key]?.first(),
                effective_value = model.averagePower(key),
                back_filled_from = (model.provenance(key) as? Provenance.BackFilled)?.from,
                reflection_value = null,
                calculator = table.firstOrNull { it.key == key && model.apiLevel in it.apis }?.calculator,
            )
        }

        return auditId
    }

    public fun verdictCounts(auditId: Long): Map<Verdict, Long> =
        database.keyResultQueries
            .verdictCountsFor(auditId) { verdict, n -> Verdict.valueOf(verdict) to n }
            .executeAsList()
            .toMap()
}
