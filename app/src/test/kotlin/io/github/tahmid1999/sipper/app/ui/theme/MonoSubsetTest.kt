package io.github.tahmid1999.sipper.app.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * CONTRACT.md §6: the subset is a build input, so no device is needed to check it. The committed
 * codepoint list is asserted here exactly - 69 ASCII printables + no-break space + 10 named
 * codepoints = 80 - and the subset contains none of the banned icon glyphs.
 */
class MonoSubsetTest {

    @Test
    fun subsetIsExactlyThePinnedSet() {
        // 0x20..0x7E is 95 printable ASCII characters, plus 11 named codepoints: no-break space,
        // ° µ · Δ Σ — … ← → ≠ (CONTRACT §6, the complete list).
        assertEquals(95 + 11, MONO_SUBSET.size, "the pinned subset changed size")
    }

    @Test
    fun bannedGlyphsAreAbsent() {
        // ▲ ▼ ▸ ▾ ⋮ are icons (CONTRACT §9.4), not glyphs; a text arrow is the first thing
        // that turns into a box on a device I do not own.
        val banned = listOf(0x25B2, 0x25BC, 0x25B8, 0x25BE, 0x22EE)
        banned.forEach { cp ->
            assertEquals(false, cp in MONO_SUBSET, "banned glyph U+%04X is in the subset".format(cp))
        }
    }

    @Test
    fun theNamedCodepointsArePresent() {
        // The absence glyph, the disagreement glyph, the back-fill arrow and the strip separator
        // carry findings; if one drops out of the subset the table stops speaking.
        val required = listOf(0x2014, 0x2260, 0x2190, 0x2192, 0x00B7, 0x0394, 0x03A3, 0x00B0, 0x00B5)
        required.forEach { cp ->
            assertEquals(true, cp in MONO_SUBSET, "required codepoint U+%04X missing".format(cp))
        }
    }
}
