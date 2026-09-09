package io.github.tahmid1999.sipper.usage

/**
 * One package's reconstructed window. The residuals are columns, not corrections: swallowing
 * unclosedTailMs into foregroundMs would make the number look better and the claim unfalsifiable.
 *
 *     foregroundMs <= true foreground in window <= foregroundMs + unclosedTailMs
 */
public data class PackageWindow(
    val foregroundMs: Long,
    val visibleMs: Long,
    val fgsMs: Long,
    val unclosedTailMs: Long,
    val openAtStart: Int,
    val dupResumes: Int,
    val closedByShutdown: Int,
    val lostTails: Int,
)

/**
 * The whole reconstruction. degradedBelowApi names the columns that are structurally unavailable at
 * the running API level, so the APPS header can say which rather than showing an empty column with
 * no explanation. unknownEventTypes is published rather than logged: a new event integer in a future
 * release is a fact about this device, and a count in a returned map stays recoverable.
 */
public data class Reconstruction(
    val windows: Map<String, PackageWindow>,
    val degradedBelowApi: Set<String>,
    val unknownEventTypes: Map<Int, Int>,
)
