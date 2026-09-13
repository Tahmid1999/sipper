package io.github.tahmid1999.sipper.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * CONTRACT.md §4/§5: the committed column lists are the width source, and the contract's totals
 * are arithmetic over them. 7.2dp is the measured advance for "0" at 12sp, fontScale 1.0
 * (CONTRACT §6). These tests are the gate that keeps the documents and the code from drifting;
 * :app:columnTableDoc (M8-17) regenerates the tables from these same lists.
 */
class ColumnsTest {

    private val advanceDp = 7.2

    // The contract's dp figures are each column rounded to the nearest dp - 6 chars is 55.2 -> 55,
    // 18 chars is 141.6 -> 142 - and their sums are the §4/§5 totals. Rounding is per column,
    // never at the end, so the widths a row actually lays out with are the ones that sum.
    private fun widthDp(column: TableColumn): Int = when (column.id) {
        "verdict" -> 100 // the one fixed-width column: a chip, not a character count
        else -> kotlin.math.round(column.chars * advanceDp + 12.0).toInt()
    }

    @Test
    fun auditTableIsSixColumnsOneFrozen() {
        assertEquals(6, AuditColumns.size, "CONTRACT §4: six columns")
        assertEquals("key", AuditColumns.first().id, "the frozen column is key")
    }

    @Test
    fun auditScrollingRegionIs508dp() {
        // CONTRACT §4: frozen 170 + scrolling 508 + 10 lead + 2 edge + 10 trailing = 700dp.
        val scrolling = AuditColumns.drop(1).sumOf { widthDp(it) }
        assertEquals(508, scrolling, "AUDIT scrolling region")
    }

    @Test
    fun auditTotalIs700dp() {
        val frozen = widthDp(AuditColumns.first())
        val scrolling = AuditColumns.drop(1).sumOf { widthDp(it) }
        assertEquals(700, frozen + scrolling + 10 + 2 + 10, "AUDIT total")
    }

    @Test
    fun appsTableIsFourteenColumnsOneFrozen() {
        assertEquals(14, AppsColumns.size, "CONTRACT §5: fourteen columns")
        assertEquals("package", AppsColumns.first().id, "the frozen column is package")
    }

    @Test
    fun appsScrollingRegionIs895dp() {
        // CONTRACT §5: frozen 142 + scrolling 895 + 10 + 2 + 10 = 1059dp.
        val scrolling = AppsColumns.drop(1).sumOf { widthDp(it) }
        assertEquals(895, scrolling, "APPS scrolling region")
    }

    @Test
    fun appsTotalIs1059dp() {
        val frozen = widthDp(AppsColumns.first())
        val scrolling = AppsColumns.drop(1).sumOf { widthDp(it) }
        assertEquals(1059, frozen + scrolling + 10 + 2 + 10, "APPS total")
    }

    @Test
    fun everyColumnTitleIsInMonoSubset() {
        // Headers are text face, but a header may still reach the mono face via a11y speech.
        // This guards the Δ fg and Σ columns in particular.
        AuditColumns.forEach { c ->
            c.title.forEach { cp -> assertEquals(true, cp.code in io.github.tahmid1999.sipper.app.ui.theme.MONO_SUBSET, "header ${c.title}") }
        }
        AppsColumns.forEach { c ->
            c.title.forEach { cp -> assertEquals(true, cp.code in io.github.tahmid1999.sipper.app.ui.theme.MONO_SUBSET, "header ${c.title}") }
        }
    }
}
