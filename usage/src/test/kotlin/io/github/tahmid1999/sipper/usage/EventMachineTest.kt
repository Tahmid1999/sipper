package io.github.tahmid1999.sipper.usage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PKG = "com.example"
private const val CLS = "com.example.MainActivity"

/** One row, package [PKG] and class [cls] unless a test overrides the class. */
private fun e(type: Int, at: Long, cls: String = CLS): RawEvent = RawEvent(PKG, cls, type, at)

/** Window 0..10_000, API 30 unless a test overrides the level. */
private fun reconstructWindow(events: List<RawEvent>, apiLevel: Int = 30): Reconstruction =
    reconstruct(events, 0L, 10_000L, apiLevel)

class EventMachineTest {

    @Test
    fun resumeThenPauseIsForegroundOnly() {
        val w = reconstructWindow(listOf(e(1, 1000), e(2, 3000))).windows.getValue(PKG)
        assertEquals(2000L, w.foregroundMs)
    }

    @Test
    fun visibleSpansResumeToStopForegroundSpansResumeToPause() {
        val w = reconstructWindow(listOf(e(1, 1000), e(2, 3000), e(23, 4000))).windows.getValue(PKG)
        assertEquals(2000L, w.foregroundMs)
        assertEquals(3000L, w.visibleMs)
    }

    @Test
    fun secondResumeDoesNotRestartTheClock() {
        val w = reconstructWindow(listOf(e(1, 1000), e(1, 2000), e(2, 3000))).windows.getValue(PKG)
        assertEquals(2000L, w.foregroundMs)
        assertEquals(1, w.dupResumes)
    }

    @Test
    fun destroyDropsTheKeySoTheStrayPauseIsNotOpenAtStart() {
        val w = reconstructWindow(listOf(e(1, 1000), e(24, 2000), e(2, 3000))).windows.getValue(PKG)
        assertEquals(1000L, w.foregroundMs)
        assertEquals(0, w.openAtStart)
    }

    @Test
    fun configurationChangeIsIgnored() {
        val w = reconstructWindow(listOf(e(1, 1000), e(5, 2000), e(2, 3000))).windows.getValue(PKG)
        assertEquals(2000L, w.foregroundMs)
        assertEquals(0, w.dupResumes)
    }

    @Test
    fun unknownTypeIsCountedNotDropped() {
        val r = reconstructWindow(listOf(e(99, 1000)))
        assertEquals(1, r.unknownEventTypes[99])
    }

    @Test
    fun bucketChangeAndDayHintsChangeNothingAndAreNotUnknown() {
        val r = reconstructWindow(listOf(e(11, 1000), e(3, 2000), e(4, 3000)))
        assertTrue(r.unknownEventTypes.isEmpty())
        assertTrue(r.windows.isEmpty())
    }

    @Test
    fun fgsIsKeyedOnPackageNotClass() {
        val w = reconstructWindow(listOf(e(19, 1000, cls = "A"), e(20, 3000, cls = "B")))
            .windows.getValue(PKG)
        assertEquals(2000L, w.fgsMs)
    }

    @Test
    fun continuingFgsOpensAtWindowStart() {
        val w = reconstructWindow(listOf(e(21, 4000), e(20, 6000))).windows.getValue(PKG)
        assertEquals(6000L, w.fgsMs)
    }

    @Test
    fun shutdownClosesTheOpenIntervalAndCountsIt() {
        val w = reconstructWindow(listOf(e(1, 1000), e(26, 5000))).windows.getValue(PKG)
        assertEquals(4000L, w.foregroundMs)
        assertEquals(1, w.closedByShutdown)
        assertEquals(0L, w.unclosedTailMs)
    }

    @Test
    fun startupClearsStateAndCountsTheLostTail() {
        val w = reconstructWindow(listOf(e(1, 1000), e(27, 5000))).windows.getValue(PKG)
        assertEquals(1, w.lostTails)
    }
}
