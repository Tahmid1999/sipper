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
        assertEquals(listOf(3000.0), parseProfile(xml)["battery.capacity"])
    }

    @Test
    fun parsesScreenOn() {
        assertEquals(listOf(143.0), parseProfile(xml)["screen.on"])
    }

    @Test
    fun parsesArrayOfValues() {
        assertEquals(listOf(400000.0, 1200000.0), parseProfile(xml)["cpu.speeds.cluster0"])
    }

    @Test
    fun preservesDeclarationOrder() {
        assertEquals(
            listOf("battery.capacity", "screen.on", "cpu.speeds.cluster0"),
            parseProfile(xml).keys.toList(),
        )
    }
}
