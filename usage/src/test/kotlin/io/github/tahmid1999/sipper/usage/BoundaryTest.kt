package io.github.tahmid1999.sipper.usage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PKG = "com.example"
private const val CLS = "com.example.MainActivity"

private fun e(type: Int, at: Long, cls: String = CLS): RawEvent = RawEvent(PKG, cls, type, at)

/** Window 1000..5000 throughout, API 30 unless a test overrides the level. */
private fun reconstructWindow(events: List<RawEvent>, apiLevel: Int = 30): Reconstruction =
    reconstruct(events, 1000L, 5000L, apiLevel)

class BoundaryTest {

    @Test
    fun lonePauseIsOpenAtStartClippedFromWindowStart() {
        val w = reconstructWindow(listOf(e(2, 3000))).windows.getValue(PKG)
        assertEquals(2000L, w.foregroundMs)
        assertEquals(1, w.openAtStart)
    }

    @Test
    fun unclosedTailIsReportedNotFoldedIntoForeground() {
        val w = reconstructWindow(listOf(e(1, 2000))).windows.getValue(PKG)
        assertEquals(0L, w.foregroundMs)
        assertEquals(3000L, w.unclosedTailMs)
    }

    @Test
    fun theBoundHoldsForTheSameStream() {
        val w = reconstructWindow(listOf(e(1, 2000))).windows.getValue(PKG)
        assertTrue(w.foregroundMs <= w.foregroundMs + w.unclosedTailMs)
        assertTrue(w.unclosedTailMs > 0L)
    }

    @Test
    fun clippingBeforeTheWindow() {
        val w = reconstructWindow(listOf(e(1, 0), e(2, 2000))).windows.getValue(PKG)
        assertEquals(1000L, w.foregroundMs)
    }

    @Test
    fun clippingAfterTheWindow() {
        val w = reconstructWindow(listOf(e(1, 4000), e(2, 9000))).windows.getValue(PKG)
        assertEquals(1000L, w.foregroundMs)
        assertEquals(0L, w.unclosedTailMs)
    }

    @Test
    fun degradedAtApi28() {
        val r = reconstructWindow(listOf(e(1, 2000), e(2, 3000), e(23, 4000)), apiLevel = 28)
        val w = r.windows.getValue(PKG)
        assertEquals(0L, w.visibleMs)
        assertEquals(0L, w.fgsMs)
        assertEquals(setOf("visible", "fgs", "shutdown"), r.degradedBelowApi)
    }

    @Test
    fun degradedAtApi29() {
        val r = reconstructWindow(emptyList(), apiLevel = 29)
        assertEquals(setOf("shutdown"), r.degradedBelowApi)
    }

    @Test
    fun notDegradedAtApi30() {
        val r = reconstructWindow(emptyList(), apiLevel = 30)
        assertTrue(r.degradedBelowApi.isEmpty())
    }
}
