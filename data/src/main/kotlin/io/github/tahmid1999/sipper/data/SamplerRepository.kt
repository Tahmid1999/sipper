package io.github.tahmid1999.sipper.`data`

public data class SamplerRunRecord(
    val id: Long,
    val sampledAt: Long,
    val intervalMs: Long,
    val bucket: String,
    val eventsCount: Long,
)

public class SamplerRepository(private val database: SipperDatabase) {

    public fun recordRun(
        sampledAt: Long,
        intervalMs: Long,
        bucket: String,
        eventsCount: Long,
    ) {
        database.samplerQueriesQueries.insertSamplerRun(
            sampled_at = sampledAt,
            interval_ms = intervalMs,
            bucket = bucket,
            events_count = eventsCount,
        )
    }

    public fun recentRuns(limit: Long = 10): List<SamplerRunRecord> =
        database.samplerQueriesQueries.recentSamplerRuns(limit) { id, sampledAt, intervalMs, bucket, eventsCount ->
            SamplerRunRecord(id, sampledAt, intervalMs, bucket, eventsCount)
        }.executeAsList()
}
