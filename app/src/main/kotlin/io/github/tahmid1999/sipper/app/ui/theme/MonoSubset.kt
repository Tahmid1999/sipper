package io.github.tahmid1999.sipper.app.ui.theme

/**
 * CONTRACT.md §6, the pinned subset. Anything outside this list is a build failure, not a tofu
 * box: a FontFamily built from a single bundled resource does not reliably fall back to the
 * system chain below API 29, so a codepoint outside the subset renders as a box, not a
 * substituted glyph. `▲ ▼ ▸ ▾ ⋮` are deliberately absent - they are icons (§9.4), not glyphs.
 */
val MONO_SUBSET: Set<Int> = buildSet {
    (0x20..0x7E).forEach(::add) // ASCII
    add(0x00A0) // no-break space
    add(0x00B0) // ° degrees
    add(0x00B5) // µ microamp-hours
    add(0x00B7) // · status-strip and reason-chip separator
    add(0x0394) // Δ the APPS delta column header and its speech form
    add(0x03A3) // Σ the APPS summary line
    add(0x2014) // — the absence glyph
    add(0x2026) // … TextOverflow.Ellipsis and MiddleEllipsis
    add(0x2190) // ← back-fill provenance, `0.1 ← screen.on`
    add(0x2192) // → DEVICE cross-check lines
    add(0x2260) // ≠ the AUDIT disagreement glyph
}
