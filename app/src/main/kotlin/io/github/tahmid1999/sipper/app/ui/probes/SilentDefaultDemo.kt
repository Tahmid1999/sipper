@file:OptIn(AuditOnly::class)

package io.github.tahmid1999.sipper.app.ui.probes

import io.github.tahmid1999.sipper.audit.AuditOnly
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/**
 * The PROBES paired demonstrations, SCREENS-AUDIT-PROBES §2.3. The manufactured number appears
 * here and nowhere else outside the expanded row: both halves render the platform's return
 * identically at the same size and colour so the quote cannot be read as sipper's own number.
 * This is the file-level `@OptIn(AuditOnly::class)` ARCHITECTURE §4 names, and the `shown` value
 * beside its cause is the demo.
 *
 * The layout rule that matters: the returned value is never coloured, never hatched and never
 * marked, on either side. Everything that differs sits below the value - the `sipper stores`
 * lines, and on a manufactured side the cause strip, the only hatched region in the block.
 */
@Composable
fun SilentDefaultDemo(
    caption: String,
    left: ProbeSide,
    right: ProbeSide,
    sinceApi: Int,
    behaviourApi: Int,
) {
    val colors = LocalSipperColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        Text(caption, style = SipperType.material.bodySmall, color = colors.textSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.rule)
                .height(140.dp),
        ) {
            DemoHalf(side = left, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).fillMaxWidth().background(colors.ruleStrong))
            DemoHalf(side = right, modifier = Modifier.weight(1f))
        }
        // The `since` line renders both numbers when they differ: `23 · filtered 31+`.
        Text(
            text = if (sinceApi == behaviourApi) "since $sinceApi" else "$sinceApi · filtered $behaviourApi+",
            style = SipperType.monoStrip,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * One half. Four lines: arg label, platform returned (identical both sides), the rule, and the
 * `sipper stores` constructor line - the cause on its own line, because
 * `SilentDefault(false, cause = PACKAGE_NOT_VISIBLE)` does not fit a half-width cell.
 */
@Composable
private fun DemoHalf(side: ProbeSide, modifier: Modifier = Modifier) {
    val colors = LocalSipperColors.current
    Column(modifier = modifier.padding(8.dp)) {
        Text("arg", style = SipperType.monoStrip, color = colors.textSecondary)
        Text(side.label, style = SipperType.mono, color = colors.textPrimary, maxLines = 1)
        // The platform's return: one style, both sides, never coloured, never hatched.
        Text(
            text = "platform returned: " + side.platformReturned,
            style = SipperType.monoStrong,
            color = colors.textPrimary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.rule)
                .padding(vertical = 2.dp),
        )
        Text("sipper stores", style = SipperType.monoStrip, color = colors.textSecondary)
        when (val reading = side.reading) {
            is Reading.Value ->
                Text("Value", style = SipperType.mono, color = colors.textNumeric)
            is Reading.SilentDefault -> Column {
                Text("SilentDefault", style = SipperType.mono, color = colors.textSecondary)
                // The cause strip - the only hatched region in the block.
                Text(
                    text = "cause " + reading.cause.name,
                    style = SipperType.monoChip,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .background(colors.hatchGround)
                        .padding(2.dp),
                )
            }
            is Reading.Denied ->
                Text("Denied", style = SipperType.mono, color = colors.stateDenied)
            is Reading.Absent ->
                Text("Absent", style = SipperType.mono, color = colors.textSecondary)
        }
    }
}

/** One side of a pair: the argument, the platform's verbatim return, and what sipper stores. */
data class ProbeSide(
    val label: String,
    val platformReturned: String,
    val reading: Reading<*>,
)
