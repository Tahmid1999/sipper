package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(AuditOnly::class)
class ReadingTest {

    private val route = Route(RouteKind.SYSTEM_RESOURCES, "power_profile.xml")

    @Test
    fun silentDefaultWithEqualFieldsAreEqualAndShareHashCode() {
        val a = Reading.SilentDefault<Double>(1.5, DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
        val b = Reading.SilentDefault<Double>(1.5, DefaultCause.KEY_ABSENT_FROM_PROFILE, route)

        assertEquals(a.shown, b.shown)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun silentDefaultDifferingOnlyInCauseAreNotEqual() {
        val a = Reading.SilentDefault<Double>(1.5, DefaultCause.KEY_ABSENT_FROM_PROFILE, route)
        val b = Reading.SilentDefault<Double>(1.5, DefaultCause.NO_USAGE_ACCESS, route)

        assertNotEquals(a, b)
    }

    @Test
    fun silentDefaultToStringHidesShownValue() {
        val shown = 42.5
        val silent = Reading.SilentDefault<Double>(shown, DefaultCause.KEY_ABSENT_FROM_PROFILE, route)

        val text = silent.toString()
        assertTrue(text.contains("cause="))
        assertTrue(text.contains("route="))
        assertFalse(text.contains(shown.toString()))
    }

    @Test
    fun valueDeniedAbsentAreDataClassesEqualByFields() {
        val routeA = Route(RouteKind.SYSTEM_RESOURCES, "power_profile.xml")
        val routeB = Route(RouteKind.SYSTEM_RESOURCES, "power_profile.xml")

        assertEquals(Reading.Value(1.5, routeA), Reading.Value(1.5, routeB))

        assertEquals(
            Reading.Denied("android.permission.PACKAGE_USAGE_STATS", Grant.Unreachable),
            Reading.Denied("android.permission.PACKAGE_USAGE_STATS", Grant.Unreachable),
        )

        assertEquals(Reading.Absent(23), Reading.Absent(23))
    }
}
