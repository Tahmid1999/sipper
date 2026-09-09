package io.github.tahmid1999.sipper.usage

// Event codes read as literal ints. 21, 22, 24 and 25 are @hide with no public field to reference.
// 23 is public only from API 29 and 26/27 from API 30, so at minSdk 28 half of these have no
// constant to compile against anyway; one literal form for all of them keeps the table readable.
//
// Dropping 24 is not conservative: an activity destroyed without a preceding STOPPED - process
// death, finish() inside onCreate - leaves its key resumed forever, and the leak lands in
// unclosedTail where it reads as real usage.
private const val ACTIVITY_RESUMED = 1
private const val ACTIVITY_PAUSED = 2
private const val END_OF_DAY = 3
private const val CONTINUE_PREVIOUS_DAY = 4
private const val CONFIGURATION_CHANGE = 5
private const val STANDBY_BUCKET_CHANGED = 11
private const val FOREGROUND_SERVICE_START = 19
private const val FOREGROUND_SERVICE_STOP = 20
private const val CONTINUING_FOREGROUND_SERVICE = 21
private const val ROLLOVER_FOREGROUND_SERVICE = 22
private const val ACTIVITY_STOPPED = 23
private const val ACTIVITY_DESTROYED = 24
private const val FLUSH_TO_DISK = 25
private const val DEVICE_SHUTDOWN = 26
private const val DEVICE_STARTUP = 27

/** ACTIVITY_STOPPED and ACTIVITY_DESTROYED arrive here, which is why mTotalTimeVisible is API 29. */
private const val VISIBLE_FROM_API = 29

/** DEVICE_SHUTDOWN and DEVICE_STARTUP arrive here. */
private const val SHUTDOWN_FROM_API = 30

private class Totals {
    var foregroundMs: Long = 0
    var visibleMs: Long = 0
    var fgsMs: Long = 0
    var unclosedTailMs: Long = 0
    var openAtStart: Int = 0
    var dupResumes: Int = 0
    var closedByShutdown: Int = 0
    var lostTails: Int = 0
}

/** One activity key's open intervals. Keyed on (package, class): instanceId is not exposed to apps. */
private class KeyState {
    var resumedSince: Long? = null
    var visibleSince: Long? = null
}

/** The part of [from]..[to] that falls inside the window. Never negative. */
private fun clipped(from: Long, to: Long, windowStartMs: Long, windowEndMs: Long): Long {
    val lo = if (from > windowStartMs) from else windowStartMs
    val hi = if (to < windowEndMs) to else windowEndMs
    return if (hi > lo) hi - lo else 0L
}

/**
 * Reconstructs foreground, visible and foreground-service time from the raw event stream, clipped
 * exactly to [windowStartMs]..[windowEndMs].
 *
 * The framework's own path does not clip: `UsageStatsDatabase.queryUsageStats` clamps its start
 * index to 0 and guards its loop with `beginTime < stats.endTime`, which is a containment test and
 * never an intersection, then `sUsageStatsCombiner` adds whole `UsageStats` objects whose
 * `add()` sums `mTotalTimeInForeground` with no clipping term. This function exists so the app has
 * a number that does clip, and so the difference can be a column rather than an assertion.
 *
 * Residuals are published, never absorbed. `unclosedTailMs` holds intervals still open at the end of
 * the window, which makes the reconstruction a bound rather than a point estimate:
 *
 *     foregroundMs <= true foreground in window <= foregroundMs + unclosedTailMs
 *
 * Degraded modes are declared from [apiLevel] rather than discovered, because the events that would
 * reveal them are the ones that do not exist.
 */
public fun reconstruct(
    events: List<RawEvent>,
    windowStartMs: Long,
    windowEndMs: Long,
    apiLevel: Int,
): Reconstruction {
    val tracksVisible = apiLevel >= VISIBLE_FROM_API
    val tracksShutdown = apiLevel >= SHUTDOWN_FROM_API

    val totals = LinkedHashMap<String, Totals>()
    val activity = LinkedHashMap<Pair<String, String>, KeyState>()
    val fgsOpenSince = LinkedHashMap<String, Long>()
    val unknownEventTypes = LinkedHashMap<Int, Int>()

    // A key that has already been observed in this window cannot be "open at start": a close with no
    // matching open is then a stray after a destroy, not a session that began before windowStartMs.
    val observed = HashSet<Pair<String, String>>()

    fun totalsOf(packageName: String): Totals = totals.getOrPut(packageName) { Totals() }

    // sortedBy is stable, so events sharing a timestamp keep their input order.
    for (event in events.sortedBy { it.timestampMs }) {
        val packageName = event.packageName
        val at = event.timestampMs
        val key = packageName to event.className

        when (event.eventType) {
            ACTIVITY_RESUMED -> {
                observed.add(key)
                val state = activity.getOrPut(key) { KeyState() }
                if (state.resumedSince != null) {
                    // No clock restart. This is the multi-instance residual: without instanceId a
                    // second instance of the same activity is indistinguishable from a duplicate.
                    totalsOf(packageName).dupResumes++
                } else {
                    state.resumedSince = at
                    if (tracksVisible && state.visibleSince == null) state.visibleSince = at
                }
            }

            ACTIVITY_PAUSED -> {
                val firstSighting = observed.add(key)
                val state = activity.getOrPut(key) { KeyState() }
                val totalsForPackage = totalsOf(packageName)
                val since = state.resumedSince
                if (since != null) {
                    totalsForPackage.foregroundMs += clipped(since, at, windowStartMs, windowEndMs)
                    state.resumedSince = null
                } else if (firstSighting) {
                    // The session began before the window. Count its in-window part rather than
                    // dropping it, which would be an undercount with the opposite sign to the
                    // framework's overcount.
                    totalsForPackage.foregroundMs +=
                        clipped(windowStartMs, at, windowStartMs, windowEndMs)
                    totalsForPackage.openAtStart++
                    if (tracksVisible && state.visibleSince == null) state.visibleSince = windowStartMs
                }
                // Below API 29 there is no Visible state: a pause returns the key to Idle.
                if (!tracksVisible) state.visibleSince = null
            }

            ACTIVITY_STOPPED -> {
                val firstSighting = observed.add(key)
                val state = activity.getOrPut(key) { KeyState() }
                val totalsForPackage = totalsOf(packageName)
                val since = state.resumedSince
                if (since != null) {
                    totalsForPackage.foregroundMs += clipped(since, at, windowStartMs, windowEndMs)
                    state.resumedSince = null
                } else if (firstSighting) {
                    totalsForPackage.foregroundMs +=
                        clipped(windowStartMs, at, windowStartMs, windowEndMs)
                    totalsForPackage.openAtStart++
                }
                if (tracksVisible) {
                    val visibleSince = state.visibleSince
                    if (visibleSince != null) {
                        totalsForPackage.visibleMs +=
                            clipped(visibleSince, at, windowStartMs, windowEndMs)
                    } else if (firstSighting) {
                        totalsForPackage.visibleMs +=
                            clipped(windowStartMs, at, windowStartMs, windowEndMs)
                    }
                }
                state.visibleSince = null
            }

            ACTIVITY_DESTROYED -> {
                observed.add(key)
                val state = activity.remove(key)
                if (state != null) {
                    val totalsForPackage = totalsOf(packageName)
                    val since = state.resumedSince
                    if (since != null) {
                        totalsForPackage.foregroundMs +=
                            clipped(since, at, windowStartMs, windowEndMs)
                    }
                    val visibleSince = state.visibleSince
                    if (tracksVisible && visibleSince != null) {
                        totalsForPackage.visibleMs +=
                            clipped(visibleSince, at, windowStartMs, windowEndMs)
                    }
                }
            }

            FOREGROUND_SERVICE_START -> {
                // Keyed on package alone: services have no class in this stream worth trusting.
                fgsOpenSince.getOrPut(packageName) { at }
            }

            FOREGROUND_SERVICE_STOP -> {
                val since = fgsOpenSince.remove(packageName)
                if (since != null) {
                    totalsOf(packageName).fgsMs += clipped(since, at, windowStartMs, windowEndMs)
                }
            }

            CONTINUING_FOREGROUND_SERVICE -> {
                // The service was already running when the window opened.
                fgsOpenSince.getOrPut(packageName) { windowStartMs }
            }

            ROLLOVER_FOREGROUND_SERVICE -> {
                val since = fgsOpenSince.remove(packageName)
                if (since != null) {
                    totalsOf(packageName).fgsMs += clipped(since, at, windowStartMs, windowEndMs)
                }
                fgsOpenSince[packageName] = at
            }

            DEVICE_SHUTDOWN -> {
                for ((activityKey, state) in activity) {
                    val totalsForPackage = totalsOf(activityKey.first)
                    val since = state.resumedSince
                    if (since != null) {
                        totalsForPackage.foregroundMs +=
                            clipped(since, at, windowStartMs, windowEndMs)
                        totalsForPackage.closedByShutdown++
                        state.resumedSince = null
                    }
                    val visibleSince = state.visibleSince
                    if (tracksVisible && visibleSince != null) {
                        totalsForPackage.visibleMs +=
                            clipped(visibleSince, at, windowStartMs, windowEndMs)
                    }
                    state.visibleSince = null
                }
                for ((servicePackage, since) in fgsOpenSince) {
                    totalsOf(servicePackage).fgsMs +=
                        clipped(since, at, windowStartMs, windowEndMs)
                }
                fgsOpenSince.clear()
            }

            DEVICE_STARTUP -> {
                // Anything still open never got an end. Its time is not recoverable, so it is
                // counted as lost rather than guessed at.
                for ((activityKey, state) in activity) {
                    if (state.resumedSince != null) totalsOf(activityKey.first).lostTails++
                }
                activity.clear()
                for (servicePackage in fgsOpenSince.keys) totalsOf(servicePackage).lostTails++
                fgsOpenSince.clear()
            }

            // Window hints and routed events. Never state, and never unknown.
            // CONFIGURATION_CHANGE is ignored deliberately: a rotation already emits its own
            // PAUSED/RESUMED pair, so counting the config change double-counts it.
            // FLUSH_TO_DISK changes nothing, but it is a known code and must not be counted below.
            // STANDBY_BUCKET_CHANGED is routed to bucket_change by :collect, never into this machine.
            END_OF_DAY,
            CONTINUE_PREVIOUS_DAY,
            CONFIGURATION_CHANGE,
            STANDBY_BUCKET_CHANGED,
            FLUSH_TO_DISK,
            -> Unit

            else -> {
                // Published rather than logged: a new event integer in a future release is a fact
                // about this device, and a count in a returned map stays recoverable from the row.
                unknownEventTypes[event.eventType] = (unknownEventTypes[event.eventType] ?: 0) + 1
            }
        }
    }

    // Intervals still open when the window closed. Summed separately, never folded into the total.
    // An open visible interval contributes to neither visibleMs nor the tail: visible has no
    // residual column, and silently adding it would be the absorption this module refuses to do.
    for ((activityKey, state) in activity) {
        val since = state.resumedSince
        if (since != null) {
            totalsOf(activityKey.first).unclosedTailMs +=
                clipped(since, windowEndMs, windowStartMs, windowEndMs)
        }
    }

    val degradedBelowApi = buildSet {
        if (!tracksVisible) {
            add("visible")
            add("fgs")
        }
        if (!tracksShutdown) add("shutdown")
    }

    val windows = totals.mapValues { (_, t) ->
        PackageWindow(
            foregroundMs = t.foregroundMs,
            visibleMs = if (tracksVisible) t.visibleMs else 0L,
            fgsMs = if (tracksVisible) t.fgsMs else 0L,
            unclosedTailMs = t.unclosedTailMs,
            openAtStart = t.openAtStart,
            dupResumes = t.dupResumes,
            closedByShutdown = t.closedByShutdown,
            lostTails = t.lostTails,
        )
    }

    return Reconstruction(
        windows = windows,
        degradedBelowApi = degradedBelowApi,
        unknownEventTypes = unknownEventTypes,
    )
}
