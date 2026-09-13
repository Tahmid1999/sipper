package io.github.tahmid1999.sipper.app.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The one table primitive, CONTRACT.md §9.5, three rules:
 *
 * 1. One gesture node, above the header: `Modifier.scrollable(...)` on the Box that wraps the
 *    header and the LazyColumn. Rows and the header are layout-only inside it - scrolling
 *    triggers placement, not recomposition. `maxPx` is computed once from the known column
 *    widths, never from a row's measured size.
 * 2. No `weight` and no `fillMaxWidth` inside the shifted region: inside it the incoming
 *    maxWidth is Constraints.Infinity and a weighted child measures at zero. Every column
 *    width is intrinsic: [columnWidth], `chars x advance + 12dp`.
 * 3. The frozen column sits outside the shifted region in the same parent Row; the frozen edge
 *    is drawn, not composed - [Modifier.frozenEdge] reads `state.offsetPx` in draw scope, never
 *    in a row's composable scope, which would recompose every visible row per drag frame.
 *
 * `TableScrollState` persists as a single Int (FLOWS §6.2) and is the only scroll state; there is
 * no second one and no synchronisation code.
 */

enum class ColumnAlign { Left, Center, Right }

/** The single width source. `title` is the column header, text face, not mono (DESIGN §1). */
data class TableColumn(
    val id: String,
    val title: String,
    val chars: Int,
    val align: ColumnAlign,
)

/** Intrinsic cell width: chars x advance + 12dp (6dp padding each side, DESIGN §3). */
fun columnWidth(column: TableColumn, advance: Dp): Dp = advance * column.chars + 12.dp

@Stable
class TableScrollState internal constructor(val maxPx: Int) {
    var offsetPx: Int by mutableIntStateOf(0)
        internal set
}

/** One saveable Int for the horizontal offset (FLOWS §6.2), hoisted above header and rows. */
@Composable
fun rememberTableScrollState(maxPx: Int): TableScrollState {
    val saver = Saver<TableScrollState, Int>(
        save = { it.offsetPx },
        restore = { restored -> TableScrollState(maxPx).apply { offsetPx = restored } },
    )
    return rememberSaveable(maxPx, saver = saver) { TableScrollState(maxPx) }
}

/**
 * The gesture node. Attach to the Box that wraps the sticky header and the LazyColumn - the one
 * scrollable in the table. `maxPx` comes from the known column widths, never a measured row.
 */
fun Modifier.tableScroll(state: TableScrollState): Modifier {
    val scrollableState = ScrollableState { delta ->
        val old = state.offsetPx
        val new = (old - delta.roundToInt()).coerceIn(0, state.maxPx)
        state.offsetPx = new
        old - new.toFloat()
    }
    return this.scrollable(scrollableState, Orientation.Horizontal)
}

/**
 * A layout-only shifted region member: clips to bounds and places itself at `-offsetPx`, so
 * scrolling moves rows by placement without recomposition. Never reads the offset in
 * composition; the draw-scope read lives in [Modifier.frozenEdge] only.
 */
fun Modifier.shifted(state: TableScrollState): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(-state.offsetPx, 0)
        }
    }

/**
 * The frozen-column edge, drawn rather than composed (CONTRACT §9.5 rule 3): 2dp of `ruleStrong`
 * on the shifted side of the frozen column, present only once the table has scrolled.
 */
fun Modifier.frozenEdge(state: TableScrollState, color: Color): Modifier =
    drawWithContent {
        drawContent()
        if (state.offsetPx > 0) {
            val w = 2.dp.toPx()
            drawRect(
                color = color,
                topLeft = Offset(size.width - w, 0f),
                size = Size(w, size.height),
            )
        }
    }

/**
 * CONTRACT §9.5: the digit advance of the bundled face at the mono style, measured over a
 * 20-glyph run so the per-character ceiling error stays under 0.05px. Keyed on Density and the
 * TextStyle only - never LocalConfiguration - so a configuration change does not re-measure the
 * same font, and a font-scale change does.
 */
@Composable
fun rememberDigitAdvance(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(density, style) {
        with(density) {
            (measurer.measure("0".repeat(20), style).size.width / 20f).toDp()
        }
    }
}
