package io.github.tahmid1999.sipper.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.tahmid1999.sipper.app.R

/**
 * DESIGN.md §2, every size/line/weight verbatim. Three weights only - 400, 500, 600.
 *
 * M3 slots overridden (everything else stays at the M3 default because nothing uses it):
 * titleSmall 15/20/600, bodyMedium 13/18/400 (M3's 14/20 is the density change and the only
 * one), bodySmall 12/16/400, labelMedium 11/14/600 letterSpacing 0.02em (column headers, tab
 * labels), labelSmall 11/14/400 (captions, the `n of m` counters).
 *
 * Roles outside M3, in SipperType: mono 12/16/400 (every table value cell, every identifier),
 * monoStrong 12/16/500 (the one value a row is about), monoChip 10/12/500 (chip labels and
 * nothing else), monoStrip 11/14/400 (the status strip), monoBlock 11/15/400 (verbatim runtime
 * output on PROBES; the multi-line mono).
 *
 * Machine values use mono/monoStrong with colour `textNumeric`; there is no `monoNumeric` role.
 * No size above 15sp exists in the app.
 */
object SipperType {

    /** DESIGN.md §1: bundled Roboto Mono 400 and 500, never `FontFamily.Monospace`. */
    val monoFamily: FontFamily = FontFamily(
        Font(R.font.roboto_mono_regular, FontWeight.W400),
        Font(R.font.roboto_mono_medium, FontWeight.W500),
    )

    val mono: TextStyle = TextStyle(
        fontFamily = monoFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400,
    )
    val monoStrong: TextStyle = TextStyle(
        fontFamily = monoFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W500,
    )
    val monoChip: TextStyle = TextStyle(
        fontFamily = monoFamily,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.W500,
    )
    val monoStrip: TextStyle = TextStyle(
        fontFamily = monoFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.W400,
    )
    val monoBlock: TextStyle = TextStyle(
        fontFamily = monoFamily,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.W400,
    )

    /** The M3 typography with the five overridden slots, face `FontFamily.Default` throughout. */
    val material: Typography = Typography(
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.W600,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.W400,
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.W400,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = 0.02.em,
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.W400,
        ),
    )
}
