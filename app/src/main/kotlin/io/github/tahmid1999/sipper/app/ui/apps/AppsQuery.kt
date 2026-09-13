package io.github.tahmid1999.sipper.app.ui.apps

import io.github.tahmid1999.sipper.usage.PackageWindow

/**
 * The APPS grid's pure state, SCREENS-APPS-SELF-DEVICE §2.6. `projectApps` is a pure function
 * tested on the JVM; the projection never reads scroll state, so scrolling does not re-sort.
 */

enum class AppsSort { Package, FgFw, FgEvt, Delta, Label, Visible, Fgs, RxFg, TxFg, RxBg, TxBg, Bucket, Mah }

enum class AppsWindow(val hours: Long) { H6(6), H24(24), H168(168) }

data class AppsQuery(
    val sort: AppsSort = AppsSort.FgEvt,
    val desc: Boolean = true,
    val text: String = "",
    val minFgMs: Long = 0,
    val buckets: IntArray = IntArray(0),
    val onlyDisagreement: Boolean = false,
    val onlyNoLauncher: Boolean = false,
    val onlyWithNetwork: Boolean = false,
    val window: AppsWindow = AppsWindow.H24,
) {
    override fun equals(other: Any?): Boolean = other is AppsQuery &&
        sort == other.sort && desc == other.desc && text == other.text &&
        minFgMs == other.minFgMs && buckets.contentEquals(other.buckets) &&
        onlyDisagreement == other.onlyDisagreement && onlyNoLauncher == other.onlyNoLauncher &&
        onlyWithNetwork == other.onlyWithNetwork && window == other.window

    override fun hashCode(): Int {
        var h = sort.hashCode()
        h = 31 * h + desc.hashCode()
        h = 31 * h + text.hashCode()
        h = 31 * h + minFgMs.hashCode()
        h = 31 * h + buckets.contentHashCode()
        h = 31 * h + onlyDisagreement.hashCode()
        h = 31 * h + onlyNoLauncher.hashCode()
        h = 31 * h + onlyWithNetwork.hashCode()
        h = 31 * h + window.hashCode()
        return h
    }
}

/** One visible package's row: both foreground numbers side by side, because the delta is the finding. */
data class AppsRow(
    val pkg: String,
    val label: String?,        // null renders `not visible` (§2.3); "" would be a bug
    val hasLauncher: Boolean,
    val fgFwMs: Long?,         // framework number; null when the query returned nothing
    val fgEvtMs: Long,        // the reconstruction, clipped
    val visibleMs: Long?,     // Absent(29) below API 29 - carried as null + degradedBelowApi
    val fgsMs: Long?,
    val tailOpen: Boolean,
    val rxFgBytes: Long?, val txFgBytes: Long?, val rxBgBytes: Long?, val txBgBytes: Long?,
    val bucket: Int?,         // null renders `unknown`
    val mah: Double?,         // modelled total; null when the sampler has not run
)

/** The projection: filter, then sort. Pure; the JVM tests pin §2.6's default and the filters. */
fun projectApps(rows: List<AppsRow>, query: AppsQuery): List<AppsRow> {
    val text = query.text.trim().lowercase()
    var projected = rows.filter { row ->
        (text.isEmpty() || row.pkg.lowercase().contains(text) ||
            (row.label?.lowercase()?.contains(text) == true)) &&
            (query.minFgMs == 0L || row.fgFwMs != null && row.fgFwMs > query.minFgMs) &&
            (query.buckets.isEmpty() || row.bucket != null && row.bucket in query.buckets) &&
            (!query.onlyDisagreement || row.fgFwMs != null && delta(row) > 3_600_000L) &&
            (!query.onlyNoLauncher || !row.hasLauncher) &&
            (!query.onlyWithNetwork || (row.rxFgBytes ?: 0) + (row.rxBgBytes ?: 0) > 0)
    }
    val comparator: Comparator<AppsRow> = when (query.sort) {
        AppsSort.Package -> compareBy { it.pkg }
        AppsSort.Label -> compareBy { it.label ?: it.pkg }
        AppsSort.Delta -> compareBy { delta(it) }
        AppsSort.Visible -> compareBy { it.visibleMs ?: 0L }
        AppsSort.Fgs -> compareBy { it.fgsMs ?: 0L }
        AppsSort.RxFg -> compareBy { it.rxFgBytes ?: 0L }
        AppsSort.TxFg -> compareBy { it.txFgBytes ?: 0L }
        AppsSort.RxBg -> compareBy { it.rxBgBytes ?: 0L }
        AppsSort.TxBg -> compareBy { it.txBgBytes ?: 0L }
        AppsSort.Bucket -> compareBy { it.bucket ?: -1 }
        AppsSort.Mah -> compareBy { it.mah ?: 0.0 }
        AppsSort.FgFw -> compareBy { it.fgFwMs ?: 0L }
        AppsSort.FgEvt -> compareBy { it.fgEvtMs }
    }
    val tieBreak = comparator.thenBy { it.pkg }
    return if (query.desc) projected.sortedWith(tieBreak.reversed())
    else projected.sortedWith(tieBreak)
}

/** The delta column: col 1 - col 2. A null framework number makes no delta (§2.2's marker cases). */
fun delta(row: AppsRow): Long = (row.fgFwMs ?: return 0L) - row.fgEvtMs
