package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun keyTableHasThirtyFiveRows() {
        // screen.full 26-33 is split into 26-29 and 30-33 (different readers) - KEYTABLE-CORRECTIONS.md section 4.1.
        assertEquals(35, keyTable().size)
    }

    @Test
    fun keyTableBatteryCapacityRow() {
        val row = keyTable().first { it.key == "battery.capacity" }
        assertEquals(Effect.ZERO_DIVIDES, row.effect)
        // battery.capacity is read via PowerProfile.getBatteryCapacity() and stored in BatteryStatsImpl - KEYTABLE-CORRECTIONS.md section 2.
        assertEquals("BatteryStatsImpl", row.calculator)
        assertEquals(26..36, row.apis)
    }

    @Test
    fun keyTableContainsWifiOnAndActive() {
        val keys = keyTable().map { it.key }
        assertTrue("wifi.on" in keys)
        assertTrue("wifi.active" in keys)
    }

    @Test
    fun keyTableScreenOnBackFillRow() {
        val row = keyTable().first { it.key == "screen.on" && it.apis == 34..36 }
        assertEquals(Effect.NOT_READ, row.effect)
        assertTrue(row.cite.file.endsWith("os/PowerProfile.java"))
    }
}
