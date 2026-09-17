package io.github.tahmid1999.sipper.app.ui.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import io.github.tahmid1999.sipper.app.ui.CoverageRing
import io.github.tahmid1999.sipper.app.ui.DashboardCard
import io.github.tahmid1999.sipper.app.ui.GaugeBar
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType
import io.github.tahmid1999.sipper.audit.AuditOnly
import io.github.tahmid1999.sipper.audit.Verdict

/**
 * The AUDIT screen: top hardware coverage ring hero, route provenance card, verdict breakdown card,
 * and key breakdown table.
 */
@OptIn(AuditOnly::class)
@Composable
fun AuditScreen(state: AuditUiState) {
    val colors = LocalSipperColors.current
    when (state) {
        AuditUiState.Loading -> Box(Modifier.fillMaxWidth().height(120.dp))
        AuditUiState.NoProfile -> Text("No power_profile resource on this device.", style = SipperType.material.bodyMedium)
        is AuditUiState.Ready -> LazyColumn(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CoverageHeroCard(state) }
            item { ProvenanceCard(state) }
            item { VerdictSummaryCard(state) }
            item { CapacityRow(state) }
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
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
fun CoverageHeroCard(state: AuditUiState.Ready) {
    val total = state.rows.size
    val presentCount = state.rows.count { it.verdict == Verdict.PRESENT }
    val explicitZeroCount = state.rows.count { it.verdict == Verdict.ZERO_BY_EXPLICIT_VALUE }
    val coverageFrac = if (total > 0) (presentCount + explicitZeroCount).toFloat() / total else 0f

    DashboardCard(title = "Hardware Profile Coverage", badge = "$presentCount / $total Keys") {
        CoverageRing(
            percentage = coverageFrac,
            label = "$presentCount Populated XML Keys out of $total Total Keys"
        )
    }
}

/** The route provenance block with visual agreement card. */
@Composable
fun ProvenanceCard(state: AuditUiState.Ready) {
    val colors = LocalSipperColors.current
    DashboardCard(title = "Route Agreement & Provenance", badge = state.agreement.label) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.routes.forEach { route ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceSunken)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(route.name, style = SipperType.monoStrong, color = colors.textPrimary)
                        Text(route.resolved, style = SipperType.monoChip, color = colors.textSecondary)
                    }
                    Text(
                        text = if (route.digest != null) route.digest else (route.failed ?: "—"),
                        style = SipperType.monoChip,
                        color = if (route.failed != null) colors.stateAbsentZero else colors.accent,
                    )
                }
            }
        }
    }
}

/** Visual verdict breakdown card with color-coded gauge bars. */
@Composable
fun VerdictSummaryCard(state: AuditUiState.Ready) {
    val colors = LocalSipperColors.current
    val total = state.rows.size.toFloat().coerceAtLeast(1f)
    val present = state.rows.count { it.verdict == Verdict.PRESENT }
    val explicitZero = state.rows.count { it.verdict == Verdict.ZERO_BY_EXPLICIT_VALUE }
    val absentZero = state.rows.count { it.verdict == Verdict.ZERO_BY_ABSENCE }
    val backFilled = state.rows.count { it.verdict == Verdict.BACK_FILLED }

    DashboardCard(title = "Keys Verdict Breakdown") {
        GaugeBar(fraction = present / total, label = "Present (Explicit XML Value)", valueText = "$present", fillColor = colors.statePresent)
        GaugeBar(fraction = explicitZero / total, label = "Explicit Zero (xml value = 0)", valueText = "$explicitZero", fillColor = colors.stateExplicitZero)
        GaugeBar(fraction = absentZero / total, label = "Absent (Zero by absence)", valueText = "$absentZero", fillColor = colors.stateAbsentZero)
        if (backFilled > 0) {
            GaugeBar(fraction = backFilled / total, label = "Back-filled Defaults", valueText = "$backFilled", fillColor = colors.stateBackFilled)
        }
    }
}

/** battery.capacity pinned above the first group. */
@Composable
fun CapacityRow(state: AuditUiState.Ready) {
    val colors = LocalSipperColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfacePinned)
            .padding(horizontal = 8.dp)
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("battery.capacity", style = SipperType.mono, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text("pinned", style = SipperType.monoStrip, color = colors.textSecondary)
    }
}

/** The sticky table header. */
@Composable
fun StickyHeader() {
    val colors = LocalSipperColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("key", style = SipperType.material.labelMedium, color = colors.textSecondary, modifier = Modifier.width(170.dp))
        Text("verdict", style = SipperType.material.labelMedium, color = colors.textSecondary, modifier = Modifier.width(100.dp))
        Text("value", style = SipperType.material.labelMedium, color = colors.textSecondary, modifier = Modifier.width(70.dp))
    }
}

/** The 24dp group header. */
@Composable
fun GroupHeader(text: String) {
    val colors = LocalSipperColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).background(colors.surfaceRaised).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = SipperType.monoStrip, color = colors.textSecondary)
    }
}

/** Key row with expandable platform quote value. */
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
            modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 4.dp),
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
            Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)) {
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


