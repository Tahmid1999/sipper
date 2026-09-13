package io.github.tahmid1999.sipper.app.ui

import java.util.Locale

/**
 * The pure formatters, SCREENS-APPS-SELF-DEVICE §1. Everything is Locale.ROOT: these are machine
 * values, and Bengali digit shapes in a column a reviewer compares against its neighbour would be
 * a regression. No thousands separators anywhere, so every value pastes into a bug report
 * unedited.
 */
object Fmt {

    /** `H:MM` - hours unpadded, truncated toward zero, no unit suffix (the header carries it). */
    fun hm(ms: Long): String {
        val total = if (ms == Long.MIN_VALUE) throw IllegalArgumentException("Long.MIN_VALUE cannot be negated") else ms
        val h = total / 3_600_000
        val m = (total - h * 3_600_000) / 60_000
        return String.format(Locale.ROOT, "%d:%02d", h, Math.abs(m))
    }

    /**
     * The delta column: always signed except an exact zero, which renders `0:00`. A nonzero
     * magnitude under one minute renders `+<1m` / `-<1m` - rounding a disagreement to zero
     * destroys the row.
     */
    fun hmSigned(ms: Long): String {
        if (ms == 0L) return "0:00"
        if (ms in -59_999..59_999) return if (ms > 0) "+<1m" else "-<1m"
        val sign = if (ms > 0) "+" else "-"
        val abs = Math.abs(ms)
        val h = abs / 3_600_000
        val m = (abs - h * 3_600_000) / 60_000
        return sign + String.format(Locale.ROOT, "%d:%02d", h, m)
    }

    /** The exact integer with a ms suffix, expanded rows and clipboard only. */
    fun millis(ms: Long): String = String.format(Locale.ROOT, "%d ms", ms)

    /**
     * Binary units: `0 B`, `931 B`, `1023 B`, `1.0 KiB`, `61 KiB`, `2.3 GiB`. Largest unit where
     * the scaled value is >= 1; one decimal when < 10, zero decimals otherwise; `KiB`/`MiB`/
     * `GiB`/`TiB` so a network figure is not read against a carrier's decimal MB.
     */
    fun bytes(b: Long): String {
        if (b < 1024) return String.format(Locale.ROOT, "%d B", b)
        var value = b.toDouble()
        val units = listOf("KiB", "MiB", "GiB", "TiB")
        var unit = ""
        for (u in units) {
            value /= 1024.0
            unit = u
            if (value < 1024.0) break
        }
        return if (value < 10.0) String.format(Locale.ROOT, "%.1f %s", value, unit)
        else String.format(Locale.ROOT, "%.0f %s", value, unit)
    }
}
