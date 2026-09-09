package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileParserTest {

    private val xml = """
        <device name="Android">
            <item name="battery.capacity">3000</item>
            <item name="screen.on">143</item>
            <array name="cpu.speeds.cluster0"><value>400000</value><value>1200000</value></array>
        </device>
    """.trimIndent()

    @Test
    fun parsesBatteryCapacity() {
        assertEquals(listOf(3000.0), parseProfile(xml).declared["battery.capacity"])
    }

    @Test
    fun parsesScreenOn() {
        assertEquals(listOf(143.0), parseProfile(xml).declared["screen.on"])
    }

    @Test
    fun parsesArrayOfValues() {
        assertEquals(listOf(400000.0, 1200000.0), parseProfile(xml).declared["cpu.speeds.cluster0"])
    }

    @Test
    fun preservesDeclarationOrder() {
        assertEquals(
            listOf("battery.capacity", "screen.on", "cpu.speeds.cluster0"),
            parseProfile(xml).declared.keys.toList(),
        )
    }

    @Test
    fun capturesLineNumbers() {
        assertEquals(
            mapOf("battery.capacity" to 2, "screen.on" to 3, "cpu.speeds.cluster0" to 4),
            parseProfile(xml).lines,
        )
    }
}
