package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals

class VerdictTest {

    @Test
    fun valueNonZeroFromSystemResourcesIsPresent() {
        assertEquals(
            Verdict.PRESENT,
            verdictFor(Reading.Value(1.5, Route(RouteKind.SYSTEM_RESOURCES, "x"))),
        )
    }

    @Test
    fun valueZeroFromSystemResourcesIsExplicitZero() {
        assertEquals(
            Verdict.ZERO_BY_EXPLICIT_VALUE,
            verdictFor(Reading.Value(0.0, Route(RouteKind.SYSTEM_RESOURCES, "x"))),
        )
    }

    @Test
    fun silentDefaultWithKeyAbsentIsZeroByAbsence() {
        assertEquals(
            Verdict.ZERO_BY_ABSENCE,
            verdictFor(
                Reading.SilentDefault(
                    0.0,
                    DefaultCause.KEY_ABSENT_FROM_PROFILE,
                    Route(RouteKind.SYSTEM_RESOURCES, "x"),
                ),
            ),
        )
    }

    @Test
    fun valueFromBackFillIsBackFilled() {
        assertEquals(
            Verdict.BACK_FILLED,
            verdictFor(Reading.Value(0.1, Route(RouteKind.BACK_FILL, "screen.on"))),
        )
    }
}
