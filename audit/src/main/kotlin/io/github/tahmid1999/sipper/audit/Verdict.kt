package io.github.tahmid1999.sipper.audit

public enum class Verdict { PRESENT, ZERO_BY_EXPLICIT_VALUE, ZERO_BY_ABSENCE, BACK_FILLED }

public fun verdictFor(reading: Reading<Double>): Verdict = when (reading) {
    is Reading.Value -> when (reading.route.kind) {
        RouteKind.BACK_FILL -> Verdict.BACK_FILLED
        RouteKind.SYSTEM_RESOURCES, RouteKind.ANDROID_PACKAGE_RESOURCES ->
            if (reading.value != 0.0) Verdict.PRESENT else Verdict.ZERO_BY_EXPLICIT_VALUE
        else -> throw IllegalArgumentException("not a profile-key route: ${reading.route.kind}")
    }
    is Reading.SilentDefault ->
        if (reading.cause == DefaultCause.KEY_ABSENT_FROM_PROFILE) Verdict.ZERO_BY_ABSENCE
        else throw IllegalArgumentException("not a profile-key SilentDefault: ${reading.cause}")
    is Reading.Denied, is Reading.Absent ->
        throw IllegalArgumentException("profile-key reading cannot be $reading")
}
