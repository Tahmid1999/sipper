package io.github.tahmid1999.sipper.app.ui.apps

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.tahmid1999.sipper.app.ui.AppsBuiltColumns
import io.github.tahmid1999.sipper.app.ui.ColumnAlign
import io.github.tahmid1999.sipper.app.ui.Fmt
import io.github.tahmid1999.sipper.app.ui.TableColumn
import io.github.tahmid1999.sipper.app.ui.columnWidth
import io.github.tahmid1999.sipper.app.ui.frozenEdge
import io.github.tahmid1999.sipper.app.ui.rememberDigitAdvance
import io.github.tahmid1999.sipper.app.ui.rememberTableScrollState
import io.github.tahmid1999.sipper.app.ui.shifted
import io.github.tahmid1999.sipper.app.ui.tableScroll
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val HOUR_MS = 3_600_000L

private val sortFor = mapOf(
    "package" to AppsSort.Package,
    "fgfw" to AppsSort.FgFw,
    "fgevt" to AppsSort.FgEvt,
    "delta" to AppsSort.Delta,
    "label" to AppsSort.Label,
    "visible" to AppsSort.Visible,
    "fgs" to AppsSort.Fgs,
)

private fun windowLabel(w: AppsWindow) = when (w) {
    AppsWindow.H6 -> "6h"
    AppsWindow.H24 -> "24h"
    AppsWindow.H168 -> "7d"
}

@Composable
public fun AppsScreen(
    onNavigateToDisclosure: () -> Unit = {},
) {
    val colors = LocalSipperColors.current
    val context = LocalContext.current

    var windowName by rememberSaveable { mutableStateOf(AppsWindow.H24.name) }
    var sortName by rememberSaveable { mutableStateOf(AppsSort.FgEvt.name) }
    var desc by rememberSaveable { mutableStateOf(true) }
    var text by rememberSaveable { mutableStateOf("") }
    val window = AppsWindow.valueOf(windowName)

    // Re-read on every resume: the user comes back here from the usage access page.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }

    var load by remember { mutableStateOf<AppsLoad?>(null) }
    LaunchedEffect(window, resumes) {
        load = withContext(Dispatchers.IO) { loadApps(context, window) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppsWindow.entries.forEach { w ->
                FilterChip(
                    selected = w == window,
                    onClick = { windowName = w.name },
                    label = { Text(windowLabel(w), style = SipperType.monoChip) },
                )
            }
        }

        when (val l = load) {
            null -> Unit
            AppsLoad.NoAccess -> NoAccess(onNavigateToDisclosure)
            is AppsLoad.Loaded -> {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("filter package or label", style = SipperType.monoChip) },
                    singleLine = true,
                    textStyle = SipperType.mono,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
                val rows = projectApps(
                    l.rows,
                    AppsQuery(sort = AppsSort.valueOf(sortName), desc = desc, text = text, window = window),
                )
                Summary(l, rows, window)
                AppsTable(
                    rows = rows,
                    windowMs = l.windowMs,
                    sort = AppsSort.valueOf(sortName),
                    desc = desc,
                    onSort = { s ->
                        if (s.name == sortName) desc = !desc else { sortName = s.name; desc = true }
                    },
                )
            }
        }
    }
}

@Composable
private fun NoAccess(onOpen: () -> Unit) {
    val colors = LocalSipperColors.current
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "usage access is off, so there are no timelines to show. PACKAGE_USAGE_STATS · " +
                "Settings → Apps → Special access → Usage access → sipper",
            style = SipperType.monoBlock,
            color = colors.textSecondary,
        )
        OutlinedButton(onClick = onOpen, modifier = Modifier.padding(top = 6.dp)) {
            Text("open", style = SipperType.monoChip)
        }
    }
}

@Composable
private fun Summary(l: AppsLoad.Loaded, shown: List<AppsRow>, window: AppsWindow) {
    val colors = LocalSipperColors.current
    val sumFw = shown.sumOf { it.fgFwMs ?: 0L }
    val sumEvt = shown.sumOf { it.fgEvtMs }
    val over = shown.count { (it.fgFwMs ?: 0L) > l.windowMs }
    Text(
        "${shown.size} packages · Σ fg fw ${Fmt.hm(sumFw)} · Σ fg evt ${Fmt.hm(sumEvt)} · " +
            "$over report more foreground than the ${windowLabel(window)} window",
        style = SipperType.monoStrip,
        color = colors.textPrimary,
        modifier = Modifier.padding(vertical = 2.dp),
    )
    // visible and fgs need the API 29 stop events; shutdown needs API 30. One line, each with its own level.
    val degraded = buildList {
        if ("visible" in l.degradedBelowApi) add("visible and fgs need API 29")
        if ("shutdown" in l.degradedBelowApi) add("shutdown events need API 30, so a reboot loses the open session")
    }
    if (degraded.isNotEmpty()) {
        Text(
            degraded.joinToString("; ") + ". This device is ${Build.VERSION.SDK_INT}.",
            style = SipperType.monoStrip,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun AppsTable(
    rows: List<AppsRow>,
    windowMs: Long,
    sort: AppsSort,
    desc: Boolean,
    onSort: (AppsSort) -> Unit,
) {
    val colors = LocalSipperColors.current
    val density = LocalDensity.current
    val advance = rememberDigitAdvance(SipperType.mono)
    val frozen = AppsBuiltColumns.first()
    val scrolling = AppsBuiltColumns.drop(1)
    val frozenW = columnWidth(frozen, advance)
    val scrollW = scrolling.fold(0.dp) { acc, c -> acc + columnWidth(c, advance) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxPx = with(density) { (scrollW - (maxWidth - frozenW)).roundToPx() }.coerceAtLeast(0)
        val state = rememberTableScrollState(maxPx)

        Column(modifier = Modifier.tableScroll(state)) {
            TableLine(frozenW, state) {
                scrolling.forEach { c ->
                    HeaderCell(c, columnWidth(c, advance), sortFor[c.id], sort, desc, onSort)
                }
            }
            HorizontalDivider(color = colors.ruleStrong)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = { it.pkg }) { row ->
                    Row(modifier = Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.width(frozenW).frozenEdge(state, colors.ruleStrong),
                        ) {
                            Text(
                                row.pkg,
                                style = SipperType.mono,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.MiddleEllipsis,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).clipToBounds()) {
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth(Alignment.Start, unbounded = true)
                                    .shifted(state),
                            ) {
                                scrolling.forEach { c -> BodyCell(c, columnWidth(c, advance), row, windowMs) }
                            }
                        }
                    }
                    HorizontalDivider(color = colors.rule)
                }
            }
        }
    }
}

@Composable
private fun TableLine(
    frozenW: Dp,
    state: io.github.tahmid1999.sipper.app.ui.TableScrollState,
    cells: @Composable () -> Unit,
) {
    val colors = LocalSipperColors.current
    Row(modifier = Modifier.height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(frozenW).frozenEdge(state, colors.ruleStrong)) {
            Text(
                "package",
                style = SipperType.material.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        Box(modifier = Modifier.weight(1f).clipToBounds()) {
            Row(
                modifier = Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .shifted(state),
                verticalAlignment = Alignment.CenterVertically,
            ) { cells() }
        }
    }
}

@Composable
private fun HeaderCell(
    c: TableColumn,
    width: Dp,
    sortsBy: AppsSort?,
    sort: AppsSort,
    desc: Boolean,
    onSort: (AppsSort) -> Unit,
) {
    val colors = LocalSipperColors.current
    val clickable = if (sortsBy != null) Modifier.clickable { onSort(sortsBy) } else Modifier
    Row(
        modifier = Modifier.width(width).then(clickable).padding(horizontal = 6.dp),
        horizontalArrangement = if (c.align == ColumnAlign.Right) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.title, style = SipperType.material.labelMedium, color = colors.textSecondary, maxLines = 1)
        if (sortsBy == sort) {
            Icon(
                // material-icons-core has no ArrowDropUp, so ascending is the same glyph turned over.
                Icons.Filled.ArrowDropDown,
                contentDescription = if (desc) "sorted descending" else "sorted ascending",
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp).rotate(if (desc) 0f else 180f),
            )
        }
    }
}

@Composable
private fun BodyCell(c: TableColumn, width: Dp, row: AppsRow, windowMs: Long) {
    val colors = LocalSipperColors.current
    val dash = "—" to colors.textSecondary
    val (value, color) = when (c.id) {
        "fgfw" -> row.fgFwMs?.let {
            // A package cannot be in the foreground for longer than the window it was asked about.
            Fmt.hm(it) to if (it > windowMs) colors.stateAbsentZero else colors.textNumeric
        } ?: dash
        "fgevt" -> Fmt.hm(row.fgEvtMs) to colors.textNumeric
        "delta" -> if (row.fgFwMs == null) dash else {
            val d = delta(row)
            when {
                d == 0L -> Fmt.hmSigned(d) to colors.textSecondary
                kotlin.math.abs(d) > HOUR_MS -> Fmt.hmSigned(d) to colors.stateAbsentZero
                else -> Fmt.hmSigned(d) to colors.textNumeric
            }
        }
        "label" -> when {
            row.label == null -> "not visible" to colors.textSecondary
            !row.hasLauncher -> "${row.label} · no launcher" to colors.textPrimary
            else -> row.label to colors.textPrimary
        }
        "visible" -> row.visibleMs?.let { Fmt.hm(it) to colors.textNumeric } ?: dash
        "fgs" -> row.fgsMs?.let { Fmt.hm(it) to colors.textNumeric } ?: dash
        "tail" -> (if (row.tailOpen) "open" else "") to colors.textSecondary
        else -> dash
    }
    Box(
        modifier = Modifier.width(width).padding(horizontal = 6.dp),
        contentAlignment = if (c.align == ColumnAlign.Right) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            value,
            style = if (c.id == "label") SipperType.material.bodySmall else SipperType.mono,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
