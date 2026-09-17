package io.github.tahmid1999.sipper.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun transform(c: Float): Double {
            return if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = transform(color.red)
        val g = transform(color.green)
        val b = transform(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val max = maxOf(l1, l2)
        val min = minOf(l1, l2)
        return (max + 0.05) / (min + 0.05)
    }

    @Test
    fun testDarkThemeContrastRatios() {
        val colors = sipperColors(dark = true)
        val ratioPrimary = contrastRatio(colors.textPrimary, colors.surface)
        assertTrue(ratioPrimary >= 4.5, "TextPrimary on dark surface contrast ratio $ratioPrimary < 4.5")

        val ratioSecondary = contrastRatio(colors.textSecondary, colors.surface)
        assertTrue(ratioSecondary >= 4.5, "TextSecondary on dark surface contrast ratio $ratioSecondary < 4.5")
    }

    @Test
    fun testLightThemeContrastRatios() {
        val colors = sipperColors(dark = false)
        val ratioPrimary = contrastRatio(colors.textPrimary, colors.surface)
        assertTrue(ratioPrimary >= 4.5, "TextPrimary on light surface contrast ratio $ratioPrimary < 4.5")

        val ratioSecondary = contrastRatio(colors.textSecondary, colors.surface)
        assertTrue(ratioSecondary >= 4.5, "TextSecondary on light surface contrast ratio $ratioSecondary < 4.5")
    }
}
