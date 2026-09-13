package io.github.tahmid1999.sipper.app.ui.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.audit.AuditOnly
import io.github.tahmid1999.sipper.audit.Verdict
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/**
 * The AUDIT screen, SCREENS-AUDIT-PROBES §1 top to bottom: provenance block, pinned capacity
 * row, counts line, sticky table header, grouped key rows, footer digest line. Zero permissions,
 * no gate, no onboarding state.
 */
@OptIn(AuditOnly::class)
@Composable
fun AuditScreen(state: AuditUiState) {
    val colors = LocalSipperColors.current
    when (state) {
        AuditUiState.Loading -> Box(Modifier.fillMaxWidth().height(120.dp))
        AuditUiState.NoProfile -> Text("No power_profile resource on this device.", style = SipperType.material.bodyMedium)
        is AuditUiState.Ready -> LazyColumn {
            item { ProvenanceBlock(state) }
            item { CapacityRow(state) }
            item { CountsLine(state) }
            item { StickyHeader() }
            val grouped = state.rows.groupBy { it.calculator ?: "not read" }
            grouped.forEach { (calculator, rows) ->
                item(key = "group-$calculator") {
                    GroupHeader("$calculator · ${rows.size} keys · ${rows.count { it.verdict == Verdict.ZERO_BY_EXPLICIT_VALUE || it.verdict == Verdict.ZERO_BY_ABSENCE }} at 0")
                }
                items(rows, key = { it.key }) { row ->
                    AuditKeyRow(row)
                }
            }
            item {
                Text(
                    text = "verdicts below are computed from framework-res.apk.",
                    style = SipperType.material.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }
    }
}

/** §1.2: the three route rows plus the informational file probe, and the agreement badge. */
@Composable
fun ProvenanceBlock(state: AuditUiState.Ready) {
    val colors = LocalSipperColors.current
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("provenance", style = SipperType.material.titleSmall, modifier = Modifier.weight(1f))
            Text(state.agreement.label, style = SipperType.monoStrip, color = colors.textSecondary)
        }
        state.routes.forEach { route ->
            Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(route.name, style = SipperType.mono, color = colors.textPrimary, modifier = Modifier.width(148.dp))
                Text(route.resolved, style = SipperType.mono, color = colors.textSecondary, modifier = Modifier.weight(1f))
                Text(
                    text = if (route.digest != null) route.digest else (route.failed ?: "—"),
                    style = SipperType.mono,
                    color = if (route.failed != null) colors.stateDenied else colors.textSecondary,
                    modifier = Modifier.width(68.dp),
                )
            }
        }
    }
}

/** §1.3: battery.capacity pinned above the first group, no ratio computed between sources. */
@Composable
fun CapacityRow(state: AuditUiState.Ready) {
    val colors = LocalSipperColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfacePinned)
            .padding(start = 2.dp)
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("battery.capacity", style = SipperType.mono, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text("pinned", style = SipperType.monoStrip, color = colors.textSecondary, modifier = Modifier.padding(end = 10.dp))
    }
}

/** §1.4: one line, each count tappable to filter, no cards, no tiles. */
@Composable
fun CountsLine(state: AuditUiState.Ready) {
    val colors = LocalSipperColors.current
    val counts = listOf(
        "present" to state.rows.count { it.verdict == Verdict.PRESENT },
        "zero in file" to state.rows.count { it.verdict == Verdict.ZERO_BY_EXPLICIT_VALUE },
        "absent" to state.rows.count { it.verdict == Verdict.ZERO_BY_ABSENCE },
        "back-filled" to state.rows.count { it.verdict == Verdict.BACK_FILLED },
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${state.rows.size} keys", style = SipperType.mono, color = colors.textPrimary)
        counts.forEach { (name, n) ->
            Text(" · $n $name", style = SipperType.mono, color = colors.textSecondary)
        }
    }
}

/** The sticky table header, §1.5's column titles in text face. */
@Composable
fun StickyHeader() {
    val colors = LocalSipperColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("key", style = SipperType.material.labelMedium, color = colors.textSecondary, modifier = Modifier.width(170.dp))
        Text("verdict", style = SipperType.material.labelMedium, color = colors.textSecondary, modifier = Modifier.width(100.dp))
        Text("value", style = SipperType.material.labelMedium, color = colors.textSecondary, modifier = Modifier.width(70.dp))
    }
}

/** The 24dp sticky group header, §1.5: `Calculator · n keys · n at 0`. */
@Composable
fun GroupHeader(text: String) {
    val colors = LocalSipperColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).background(colors.surfaceRaised).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = SipperType.monoStrip, color = colors.textSecondary)
    }
}

/**
 * A 32dp key row. The value cell is the literal XML token (CONTRACT §4); `=` / `≠` per route
 * agreement. Tapping expands in place (§1.6) - and the expanded row is one of the two places
 * the manufactured number renders, prefixed `platform returned:` so it reads as the platform's
 * quote, never as sipper's own number. This file's `@OptIn(AuditOnly::class)` covers it.
 */
@Composable
fun AuditKeyRow(row: AuditRow) {
    val colors = LocalSipperColors.current
    var expanded by rememberSaveable(row.key) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .then(if (expanded) Modifier.background(colors.surfacePinned) else Modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.key, style = SipperType.mono, color = colors.textPrimary, modifier = Modifier.width(170.dp), maxLines = 1)
            Text(row.verdict.name, style = SipperType.monoChip, color = verdictColor(row.verdict), modifier = Modifier.width(100.dp))
            Text(
                text = row.literalToken ?: "—",
                style = SipperType.monoStrong,
                color = if (row.literalToken != null) colors.textNumeric else colors.textSecondary,
                modifier = Modifier.width(70.dp),
            )
            Text(
                text = if (row.routesAgree) "=" else "≠",
                style = SipperType.mono,
                color = if (row.routesAgree) colors.textSecondary else colors.stateAbsentZero,
                modifier = Modifier.width(26.dp),
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)) {
                if (row.reflectionValue != null) {
                    Text(
                        text = "reflection      platform returned: " + row.reflectionValue,
                        style = SipperType.monoBlock,
                        color = colors.textSecondary,
                    )
                }
                Text(
                    text = "read by         " + (row.calculator ?: "nothing at this level"),
                    style = SipperType.monoBlock,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun verdictColor(verdict: Verdict): androidx.compose.ui.graphics.Color =
    LocalSipperColors.current.let {
        when (verdict) {
            Verdict.PRESENT -> it.statePresent
            Verdict.ZERO_BY_EXPLICIT_VALUE -> it.stateExplicitZero
            Verdict.ZERO_BY_ABSENCE -> it.stateAbsentZero
            Verdict.BACK_FILLED -> it.stateBackFilled
        }
    }

