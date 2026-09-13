package io.github.tahmid1999.sipper.app.ui.apps

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `projectApps` is a pure function tested on the JVM (SCREENS-APPS §2.1). These tests pin §2.6's
 * default sort, the filters, and the tie-break; none of them touch Compose or Android.
 */
class AppsQueryTest {

    private fun row(
        pkg: String,
        fgFw: Long? = null,
        fgEvt: Long = 0,
        label: String? = pkg,
        bucket: Int? = null,
        rxFg: Long? = null,
    ) = AppsRow(
        pkg = pkg, label = label, hasLauncher = true,
        fgFwMs = fgFw, fgEvtMs = fgEvt, visibleMs = 0, fgsMs = 0, tailOpen = false,
        rxFgBytes = rxFg, txFgBytes = null, rxBgBytes = null, txBgBytes = null,
        bucket = bucket, mah = null,
    )

    private val rows = listOf(
        row("com.alpha", fgFw = 3_600_000, fgEvt = 1_000_000),   // delta exactly 1h: NOT > 1h
        row("com.beta", fgFw = 7_200_000, fgEvt = 2_000_000),   // delta 5h
        row("com.gamma", fgFw = null, fgEvt = 5_000_000),
        row("com.delta", fgFw = 5_400_000, fgEvt = 1_000_000),  // delta 4.4h: > 1h
    )

    @Test
    fun defaultSortIsFgEvtDescendingWithPackageTies() {
        val out = projectApps(rows, AppsQuery())
        assertEquals(listOf("com.gamma", "com.beta", "com.delta", "com.alpha"), out.map { it.pkg })
    }

    @Test
    fun textFilterMatchesPackageCaseInsensitive() {
        val out = projectApps(rows, AppsQuery(text = "COM.AL"))
        assertEquals(listOf("com.alpha"), out.map { it.pkg })
    }

    @Test
    fun minFgFiltersNullFrameworkNumbersOut() {
        // A package with no framework number cannot pass an fg > 0 filter: the cell is absence,
        // not zero, and a filter must not manufacture a zero into the comparison.
        val out = projectApps(rows, AppsQuery(minFgMs = 1))
        assertEquals(listOf("com.beta", "com.delta", "com.alpha"), out.map { it.pkg })
    }

    @Test
    fun bucketFilterKeepsOnlyListedBuckets() {
        val b = listOf(
            row("a", bucket = 10),
            row("b", bucket = 45),
            row("c", bucket = null),
        )
        val out = projectApps(b, AppsQuery(buckets = intArrayOf(45)))
        assertEquals(listOf("b"), out.map { it.pkg })
    }

    @Test
    fun onlyDisagreementKeepsDeltaOverAnHour() {
        // The demo filter: the delta must exceed one hour to be a disagreement worth showing,
        // and exactly one hour does not qualify (SCREENS-APPS §2.6's `Δ > 1h`).
        val out = projectApps(rows, AppsQuery(onlyDisagreement = true))
        assertEquals(listOf("com.beta", "com.delta"), out.map { it.pkg })
    }

    @Test
    fun onlyWithNetworkKeepsRowsWithBytes() {
        val n = listOf(row("a", rxFg = 100), row("b"))
        val out = projectApps(n, AppsQuery(onlyWithNetwork = true))
        assertEquals(listOf("a"), out.map { it.pkg })
    }

    @Test
    fun deltaIsFrameworkMinusReconstruction() {
        assertEquals(2_600_000, delta(rows[0]))
        // A null framework number is the no-delta case, rendered by §2.2's markers, never 0+fg.
        assertEquals(0L, delta(rows[2]))
    }

    @Test
    fun ascendingDirectionReversesTheSort() {
        val out = projectApps(rows, AppsQuery(desc = false))
        assertEquals(
            listOf("com.alpha", "com.delta", "com.beta", "com.gamma"),
            out.map { it.pkg },
        )
    }
}
