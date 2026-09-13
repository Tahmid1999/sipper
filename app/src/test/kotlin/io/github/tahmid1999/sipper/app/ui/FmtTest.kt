package io.github.tahmid1999.sipper.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** SCREENS-APPS §1: table-driven, one row per function, all Locale.ROOT, all pure. */
class FmtTest {

    // hm: `0:41`, `12:47`, `312:00` - six characters covers the 10-day retention window.
    private val hmCases = listOf(
        41 * 60_000L to "0:41",
        12 * 3_600_000L + 47 * 60_000L to "12:47",
        312 * 3_600_000L to "312:00",
        0L to "0:00",
        59_999L to "0:00", // truncated toward zero, never rounded up
    )

    @Test
    fun hm() {
        hmCases.forEach { (ms, expected) -> assertEquals(expected, Fmt.hm(ms), "hm($ms)") }
    }

    // hmSigned: `+1:10`, `-0:04`, `+<1m`, `0:00`; never `+0:00`.
    private val hmSignedCases = listOf(
        70 * 60_000L to "+1:10",
        -4 * 60_000L to "-0:04",
        1L to "+<1m",
        -59_999L to "-<1m",
        0L to "0:00",
        3_600_000L to "+1:00",
    )

    @Test
    fun hmSigned() {
        hmSignedCases.forEach { (ms, expected) -> assertEquals(expected, Fmt.hmSigned(ms), "hmSigned($ms)") }
    }

    @Test
    fun millisHasNoSeparators() {
        assertEquals("86400000 ms", Fmt.millis(86_400_000L))
    }

    // bytes: `0 B`, `931 B`, `1023 B`, `1.0 KiB`, `61 KiB`, `2.3 GiB` (SCREENS-APPS §1.2's list).
    private val bytesCases = listOf(
        0L to "0 B",
        931L to "931 B",
        1023L to "1023 B",
        1024L to "1.0 KiB",
        62_464L to "61 KiB",          // 61.0 KiB: >= 10, zero decimals
        2_472_232_832L to "2.3 GiB",  // 2.30 GiB: < 10, one decimal
        9_490_000_000L to "8.8 GiB",  // 9.49 GiB rounds once, not twice
    )

    @Test
    fun bytes() {
        bytesCases.forEach { (b, expected) -> assertEquals(expected, Fmt.bytes(b), "bytes($b)") }
    }

    @Test
    fun longMinValueThrows() {
        // It cannot be negated and no platform read produces it; the formatter says so loudly.
        assertFailsWith<IllegalArgumentException> { Fmt.hm(Long.MIN_VALUE) }
    }
}
