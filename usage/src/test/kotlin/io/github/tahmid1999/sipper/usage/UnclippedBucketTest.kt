package io.github.tahmid1999.sipper.usage

import kotlin.test.Test
import kotlin.test.assertEquals

private const val PKG = "com.example"
private const val CLS = "com.example.MainActivity"

private fun e(type: Int, at: Long): RawEvent = RawEvent(PKG, CLS, type, at)

/** What the framework returns: whole buckets that overlap the window, summed with no clipping. */
private fun frameworkSum(buckets: List<LongRange>, window: LongRange): Long =
    buckets.filter { it.first <= window.last && it.last >= window.first }
        .sumOf { it.last - it.first }

class UnclippedBucketTest {

    @Test
    fun theFrameworkOvercountsTheWindowByExactlyTheBucketOverhang() {
        // UsageStatsDatabase.queryUsageStats clamps startIndex to 0 and guards its loop with
        // beginTime < stats.endTime, which is a containment test and never an intersection, and
        // sUsageStatsCombiner adds whole UsageStats objects. frameworkSum sums each bucket's full
        // width, which models the worst case where the app was foreground for the whole bucket -
        // the case the README's "up to 48 hours" refers to.
        val window = 0L..10_000L
        val buckets = listOf(
            -5_000L..2_000L,
            4_000L..6_000L,
            9_000L..14_000L,
        )

        val events = listOf(
            e(1, -5_000L),
            e(2, 2_000L),
            e(1, 4_000L),
            e(2, 6_000L),
            e(1, 9_000L),
            e(2, 14_000L),
        )

        val w = reconstruct(events, window.first, window.last, 30).windows.getValue(PKG)

        // The clipped reconstruction is exact to the window: 2000 + 2000 + 1000.
        assertEquals(5_000L, w.foregroundMs + w.unclosedTailMs)

        // The framework sums the whole buckets with no clipping: 7000 + 2000 + 5000.
        assertEquals(14_000L, frameworkSum(buckets, window))

        // The two buckets that straddle the window edges overhang by exactly 9000.
        assertEquals(9_000L, frameworkSum(buckets, window) - (w.foregroundMs + w.unclosedTailMs))
    }
}
