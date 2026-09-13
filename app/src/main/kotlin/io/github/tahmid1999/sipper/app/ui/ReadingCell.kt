package io.github.tahmid1999.sipper.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.audit.Grant
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/**
 * The composable that takes a Reading, ARCHITECTURE.md §4's shape. `format` is reachable from the
 * Value branch and nowhere else; the when is exhaustive with no else; a manufactured answer
 * cannot reach a formatter without this file changing shape.
 *
 * Per-branch behaviour (SCREENS-APPS-SELF-DEVICE §0): Value prints the formatted number in
 * textNumeric; Denied prints the permission name as a tappable chip opening the one-line grant
 * sheet; Absent prints the em dash in textSecondary; SilentDefault prints no number at all -
 * a hatched cell (CONTRACT §9.3) with the cause chip immediately to its right.
 */

/** CONTRACT.md §9.3, verbatim body: 45 degrees, 1dp stroke, 5dp pitch along x, no state hue. */
private fun DrawScope.hatch(line: Color) {
    val pitch = 5.dp.toPx()
    val stroke = 1.dp.toPx()
    val h = size.height
    var x = -h
    while (x < size.width) {
        drawLine(line, Offset(x, h), Offset(x + h, 0f), strokeWidth = stroke)
        x += pitch
    }
}

/** The hatch over `hatchGround`, clipped so no stripe bleeds into neighbouring cells. */
fun Modifier.hatch(ground: Color, line: Color): Modifier =
    clipToBounds().drawBehind {
        drawRect(ground)
        hatch(line)
    }

/** A Value cell: the formatted machine value in textNumeric. `format`/`speak` live here alone. */
@Composable
fun <T> ValueText(
    value: T,
    format: (T) -> String,
    align: ColumnAlign,
    routeChip: String? = null,
) {
    val colors = LocalSipperColors.current
    Text(
        text = format(value),
        style = SipperType.mono.copy(color = colors.textNumeric),
        textAlign = when (align) {
            ColumnAlign.Left -> TextAlign.Left
            ColumnAlign.Center -> TextAlign.Center
            ColumnAlign.Right -> TextAlign.Right
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
    if (routeChip != null) {
        Text(
            text = routeChip,
            style = SipperType.monoChip,
            color = colors.textSecondary,
        )
    }
}

/**
 * The hatched cell: no number, no dash, no glyph - CONTRACT §9.3. The cause chip sits immediately
 * right in textSecondary. This cell never carries a state hue and never a formatter.
 */
@Composable
fun HatchCell(cause: String, align: ColumnAlign) {
    val colors = LocalSipperColors.current
    Row(
        modifier = Modifier
            .hatch(colors.hatchGround, colors.hatchLine)
            .padding(horizontal = 6.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = cause,
            style = SipperType.monoChip,
            color = colors.textSecondary,
        )
    }
}

/** A chip cell for the non-value outcomes. `Denied` is tappable; `Absent` is the em dash. */
sealed interface CellChip {
    data class DeniedChip(val permission: String, val howToGrant: Grant) : CellChip
    data class AbsentChip(val minApi: Int) : CellChip
}

@Composable
fun ChipCell(chip: CellChip, onGrant: (CellChip.DeniedChip) -> Unit) {
    val colors = LocalSipperColors.current
    when (chip) {
        is CellChip.DeniedChip -> Text(
            text = chip.permission,
            style = SipperType.monoChip,
            color = colors.stateDenied,
            modifier = Modifier
                .clickable { onGrant(chip) }
                .padding(horizontal = 6.dp),
        )
        is CellChip.AbsentChip -> Text(
            text = "—",
            style = SipperType.mono,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

/** ARCHITECTURE.md §4's render contract: the one composable that takes a Reading. */
@Composable
fun <T> ReadingCell(
    reading: Reading<T>,
    column: TableColumn,
    format: (T) -> String,
    speak: (T) -> String = format,
    onDenied: (CellChip.DeniedChip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (reading) {
            is Reading.Value -> ValueText(
                value = reading.value,
                format = format,
                align = column.align,
            )
            is Reading.SilentDefault -> HatchCell(cause = reading.cause.name, align = column.align)
            is Reading.Denied -> ChipCell(
                chip = CellChip.DeniedChip(reading.permission, reading.howToGrant),
                onGrant = onDenied,
            )
            is Reading.Absent -> ChipCell(
                chip = CellChip.AbsentChip(reading.minApi),
                onGrant = onDenied,
            )
        }
    }
}

/** FLOWS §9: the spoken form comes from the same file that renders it, always naming the column. */
fun <T> describeReading(
    reading: Reading<T>,
    columnTitle: String,
    format: (T) -> String,
): String = when (reading) {
    is Reading.Value -> "$columnTitle, ${format(reading.value)}"
    is Reading.SilentDefault -> "manufactured value, cause ${reading.cause.name.lowercase()}"
    is Reading.Denied -> "$columnTitle, denied, ${reading.permission}"
    is Reading.Absent -> "$columnTitle, requires API ${reading.minApi}"
}

