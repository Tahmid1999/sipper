package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BackFillTest {

    private val onlyDeprecated = parseProfile(
        """
        <device name="Android">
            <item name="screen.on">0.1</item>
        </device>
        """.trimIndent()
    )

    private val perDisplay = parseProfile(
        """
        <device name="Android">
            <item name="screen.on">0.1</item>
            <item name="screen.on.display0">0.2</item>
            <item name="screen.on.display1">0.3</item>
        </device>
        """.trimIndent()
    )

    private val mixed = parseProfile(
        """
        <device name="Android">
            <item name="ambient.on">0.5</item>
            <item name="screen.on.display0">0.2</item>
        </device>
        """.trimIndent()
    )

    @Test
    fun declaredKeyIsDeclared() {
        assertEquals(
            Provenance.Declared("screen.on", 0.1, 2),
            provenanceOf("screen.on", onlyDeprecated, 36),
        )
    }

    @Test
    fun deprecatedSingularBackFillsDisplayZeroAtApi36() {
        assertEquals(
            Provenance.BackFilled("screen.on.display0", "screen.on", 0.1, "PowerProfile.initDisplays"),
            provenanceOf("screen.on.display0", onlyDeprecated, 36),
        )
    }

    @Test
    fun noBackFillBelowApi34() {
        assertEquals(
            Provenance.NotDeclared("screen.on.display0"),
            provenanceOf("screen.on.display0", onlyDeprecated, 28),
        )
    }

    @Test
    fun onlyOrdinalZeroIsBackFilled() {
        assertEquals(
            Provenance.NotDeclared("screen.on.display1"),
            provenanceOf("screen.on.display1", onlyDeprecated, 36),
        )
    }

    @Test
    fun unrelatedKeyIsNotDeclared() {
        assertEquals(
            Provenance.NotDeclared("camera.avg"),
            provenanceOf("camera.avg", onlyDeprecated, 36),
        )
    }

    @Test
    fun displayCounts() {
        assertEquals(0, declaredDisplayCount(onlyDeprecated.declared))
        assertEquals(2, declaredDisplayCount(perDisplay.declared))
        assertEquals(1, displayCount(onlyDeprecated.declared, 36))
        assertEquals(0, displayCount(onlyDeprecated.declared, 28))
    }

    @Test
    fun declaredPerDisplayKeyIsNeverOverwritten() {
        val p = assertIs<Provenance.Declared>(provenanceOf("screen.on.display0", perDisplay, 36))
        assertEquals(0.2, p.value)
    }

    @Test
    fun displayBackFillsOnlyDeprecatedSingulars() {
        val fills = displayBackFills(onlyDeprecated.declared)
        assertEquals(1, fills.size)
        assertEquals("screen.on.display0", fills[0].synthesised)
        assertEquals("screen.on", fills[0].from)
        assertEquals(Transform.PerDisplay(0), fills[0].transform)
        assertTrue(displayBackFills(perDisplay.declared).isEmpty())
    }

    @Test
    fun singlePerDisplayKeySuppressesLegacyBackFill() {
        assertEquals(
            Provenance.NotDeclared("ambient.on.display0"),
            provenanceOf("ambient.on.display0", mixed, 36),
        )
    }
}
