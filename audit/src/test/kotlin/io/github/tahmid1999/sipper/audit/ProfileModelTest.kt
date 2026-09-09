package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileModelTest {

    private val xml = """
        <device name="Android">
            <item name="screen.on">0.1</item>
            <item name="wifi.controller.rx">0.0</item>
            <item name="battery.capacity">3000</item>
        </device>
    """.trimIndent()

    private val model36 = ProfileModel(parseProfile(xml), 36)
    private val model28 = ProfileModel(parseProfile(xml), 28)

    @Test
    fun batteryCapacityIsPresentAtApi36() {
        assertEquals(3000.0, model36.averagePower("battery.capacity"))
        assertEquals(Verdict.PRESENT, verdictOf(model36.provenance("battery.capacity")))
    }

    @Test
    fun declaredZeroIsExplicitZero() {
        assertEquals(0.0, model36.averagePower("wifi.controller.rx"))
        assertEquals(Verdict.ZERO_BY_EXPLICIT_VALUE, verdictOf(model36.provenance("wifi.controller.rx")))
    }

    @Test
    fun absentKeyIsZeroByAbsence() {
        assertEquals(0.0, model36.averagePower("camera.avg"))
        assertEquals(Verdict.ZERO_BY_ABSENCE, verdictOf(model36.provenance("camera.avg")))
    }

    @Test
    fun displayKeyBackFillsAtApi36() {
        assertEquals(0.1, model36.averagePower("screen.on.display0"))
        assertEquals(Verdict.BACK_FILLED, verdictOf(model36.provenance("screen.on.display0")))
    }

    @Test
    fun noBackFillAtApi28() {
        assertEquals(0.0, model28.averagePower("screen.on.display0"))
        assertEquals(Verdict.ZERO_BY_ABSENCE, verdictOf(model28.provenance("screen.on.display0")))
    }

    @Test
    fun auditProducesVerdicts() {
        val audit = model36.audit()
        assertTrue(audit.verdicts.isNotEmpty())
        assertTrue(audit.count(Verdict.ZERO_BY_ABSENCE) > 0)
    }
}
