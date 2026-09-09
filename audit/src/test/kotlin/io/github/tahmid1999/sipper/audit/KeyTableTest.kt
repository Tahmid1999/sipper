package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyTableTest {

    private val tsv = listOf(
        listOf("key","apiLo","apiHi","calculator","effect","citeTag","citeFile","citeLine","citeAnchor"),
        listOf("battery.capacity","26","36","","ZERO_DIVIDES","android-16.0.0_r1","frameworks/base/core/java/com/android/internal/os/PowerProfile.java","254",""),
        listOf("cpu.suspend","28","36","CpuPowerCalculator","ZERO_ZEROES_TERM","android-16.0.0_r1","frameworks/base/services/core/java/com/android/server/power/stats/CpuPowerCalculator.java","212",""),
    ).joinToString("\n") { it.joinToString("\t") }

    @Test
    fun dropsHeaderRow() {
        assertEquals(2, parseKeyTable(tsv).size)
    }

    @Test
    fun parsesBatteryCapacityRow() {
        val row = parseKeyTable(tsv)[0]
        assertEquals("battery.capacity", row.key)
        assertEquals(26..36, row.apis)
        assertNull(row.calculator)
        assertEquals(Effect.ZERO_DIVIDES, row.effect)
        assertEquals(254, row.cite.line)
        assertEquals("android-16.0.0_r1", row.cite.tag)
    }

    @Test
    fun parsesCpuSuspendRow() {
        val row = parseKeyTable(tsv)[1]
        assertEquals("cpu.suspend", row.key)
        assertEquals(28..36, row.apis)
        assertEquals("CpuPowerCalculator", row.calculator)
        assertEquals(Effect.ZERO_ZEROES_TERM, row.effect)
        assertEquals(212, row.cite.line)
    }
}
