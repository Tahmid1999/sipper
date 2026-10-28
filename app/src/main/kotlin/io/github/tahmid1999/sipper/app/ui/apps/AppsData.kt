package io.github.tahmid1999.sipper.app.ui.apps

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.collect.readRawEvents
import io.github.tahmid1999.sipper.collect.usageAccessGranted
import io.github.tahmid1999.sipper.usage.reconstruct

sealed interface AppsLoad {
    data object NoAccess : AppsLoad
    data class Loaded(
        val rows: List<AppsRow>,
        val windowMs: Long,
        val degradedBelowApi: Set<String>,
    ) : AppsLoad
}

/**
 * Both foreground numbers for every package that had any foreground time in the window: the
 * framework's own sum of daily buckets, and the reconstruction from the event stream. They are
 * read side by side because they disagree, and the disagreement is what the screen is for.
 */
fun loadApps(context: Context, window: AppsWindow, nowMs: Long = System.currentTimeMillis()): AppsLoad {
    val access = usageAccessGranted(context)
    if (access !is Reading.Value || !access.value) return AppsLoad.NoAccess

    val windowMs = window.hours * 3_600_000L
    val startMs = nowMs - windowMs
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // queryUsageStats hands back whole daily buckets, never clipped to startMs, and can repeat a
    // package across buckets. Summing them is what every screen-time app does, so sipper does too.
    // It returns null rather than empty while the user is locked.
    val fw = HashMap<String, Long>()
    usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMs, nowMs)?.forEach { s ->
        fw[s.packageName] = (fw[s.packageName] ?: 0L) + s.totalTimeInForeground
    }

    val events = readRawEvents(context, startMs, nowMs)
    if (events is Reading.Denied) return AppsLoad.NoAccess
    val rec = reconstruct(
        events = (events as? Reading.Value)?.value.orEmpty(),
        windowStartMs = startMs,
        windowEndMs = nowMs,
        apiLevel = Build.VERSION.SDK_INT,
    )
    val tracksVisible = "visible" !in rec.degradedBelowApi

    val pm = context.packageManager
    val rows = (fw.keys + rec.windows.keys)
        .filter { it.isNotEmpty() }
        .mapNotNull { pkg ->
            val w = rec.windows[pkg]
            val fgFw = fw[pkg]
            val fgEvt = w?.foregroundMs ?: 0L
            val tailOpen = (w?.unclosedTailMs ?: 0L) > 0L
            // An app in the foreground right now has only an open tail, so it is kept on that alone.
            if ((fgFw ?: 0L) == 0L && fgEvt == 0L && !tailOpen) return@mapNotNull null
            AppsRow(
                pkg = pkg,
                label = labelOf(pm, pkg),
                hasLauncher = pm.getLaunchIntentForPackage(pkg) != null,
                fgFwMs = fgFw,
                fgEvtMs = fgEvt,
                visibleMs = if (tracksVisible) w?.visibleMs ?: 0L else null,
                fgsMs = if (tracksVisible) w?.fgsMs ?: 0L else null,
                tailOpen = tailOpen,
                rxFgBytes = null, txFgBytes = null, rxBgBytes = null, txBgBytes = null,
                bucket = null,
            )
        }
    return AppsLoad.Loaded(rows, windowMs, rec.degradedBelowApi)
}

// Below API 30 UsageStatsManager does no visibility filtering, so it can name packages this app
// cannot resolve. Those keep their numbers and get a null label, which renders `not visible`.
private fun labelOf(pm: PackageManager, pkg: String): String? = try {
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
} catch (e: PackageManager.NameNotFoundException) {
    null
}
