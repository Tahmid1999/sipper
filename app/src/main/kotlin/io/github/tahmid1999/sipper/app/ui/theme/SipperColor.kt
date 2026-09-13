package io.github.tahmid1999.sipper.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CONTRACT.md §1, every hex verbatim. No dynamic colour and no in-app theme toggle -
 * `isSystemInDarkTheme()` and nothing else, because the wallpaper would otherwise pick the
 * hues that carry the verdicts.
 *
 * Six state hues, no two shared: green PRESENT, amber EXPLICIT-ZERO, red ABSENCE, indigo
 * BACK-FILLED, teal DENIED, warm grey STALE. `Reading.Absent` has no token; it renders
 * `textSecondary`. `SilentDefault` has no hue at all: a coloured cell reads as a value, and
 * the one thing the hatch must not read as is a value.
 */
@Immutable
class SipperColors(
    // §1.1 Surfaces, rules, text
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val surfacePinned: Color,
    val rule: Color,
    val ruleStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textNumeric: Color,
    val accent: Color,
    // §1.2 State
    val statePresent: Color,
    val stateExplicitZero: Color,
    val stateAbsentZero: Color,
    val stateBackFilled: Color,
    val stateDenied: Color,
    val stateStale: Color,
    // §1.3 Hatch
    val hatchGround: Color,
    val hatchLine: Color,
)

private val LightPalette = SipperColors(
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF3F4F6),
    surfaceSunken = Color(0xFFE9EBEF),
    surfacePinned = Color(0xFFEDF1F7),
    rule = Color(0xFFDDE1E6),
    ruleStrong = Color(0xFF8A9199),
    textPrimary = Color(0xFF1A1F26),
    textSecondary = Color(0xFF5B646F),
    textNumeric = Color(0xFF0E1216),
    accent = Color(0xFF0B5FCC),
    statePresent = Color(0xFF14713A),
    stateExplicitZero = Color(0xFF8A5200),
    stateAbsentZero = Color(0xFFA62015),
    stateBackFilled = Color(0xFF3B36A6),
    stateDenied = Color(0xFF0F5468),
    stateStale = Color(0xFF6B5F4A),
    hatchGround = Color(0xFFE9EBEF),
    hatchLine = Color(0xFFBBBEC3), // textPrimary at 22%
)

private val DarkPalette = SipperColors(
    surface = Color(0xFF0F1216),
    surfaceRaised = Color(0xFF171B21),
    surfaceSunken = Color(0xFF0A0C0F),
    surfacePinned = Color(0xFF1B222B),
    rule = Color(0xFF262B33),
    ruleStrong = Color(0xFF5C6774),
    textPrimary = Color(0xFFE3E7EC),
    textSecondary = Color(0xFF98A1AC),
    textNumeric = Color(0xFFF4F7FA),
    accent = Color(0xFF6FA8F5),
    statePresent = Color(0xFF5BC27E),
    stateExplicitZero = Color(0xFFDFA43C),
    stateAbsentZero = Color(0xFFF0796B),
    stateBackFilled = Color(0xFFA6A2F2),
    stateDenied = Color(0xFF63C3E0),
    stateStale = Color(0xFFB4A78E),
    hatchGround = Color(0xFF0A0C0F),
    hatchLine = Color(0xFF424548), // textPrimary at 26%
)

val LocalSipperColors = staticCompositionLocalOf<SipperColors> {
    error("SipperColors not provided; SipperTheme is missing")
}

fun sipperColors(dark: Boolean): SipperColors = if (dark) DarkPalette else LightPalette
