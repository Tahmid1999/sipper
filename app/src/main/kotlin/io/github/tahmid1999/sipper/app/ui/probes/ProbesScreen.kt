package io.github.tahmid1999.sipper.app.ui.probes

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.audit.DefaultCause
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.Route
import io.github.tahmid1999.sipper.audit.RouteKind
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/** One capability-table row, §2.4: the call, its result, value, detail, since, and run button. */
data class ProbeRow(
    val call: String,
    val minApi: Int,
    val behaviourSinceApi: Int,
    val run: (Context) -> Reading<*>,
)

enum class ProbeState { Pending, Ok, Denied, Absent, Threw }

/**
 * PROBES, SCREENS-AUDIT-PROBES §2: the four paired demonstrations pinned above the capability
 * table, then sections A-D. Rows appear in declaration order and are never reordered as they
 * resolve. Denials print the verbatim runtime line, never a paraphrase.
 */
@Composable
fun ProbesScreen(context: Context) {
    val colors = LocalSipperColors.current
    val api = android.os.Build.VERSION.SDK_INT

    // The run starts immediately with no tap (§2.5); pair 1's two reflect calls run live and
    // their results are what renders, never a remembered value.
    var pair1 by remember { mutableStateOf<List<Reading<Double>>?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        pair1 = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            listOf(
                io.github.tahmid1999.sipper.collect.reflectAveragePower(context, "cpu.cluster_power.cluster0"),
                io.github.tahmid1999.sipper.collect.reflectAveragePower(context, "sipper.nonsense.key"),
            )
        }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { PairHeader() }

        // Pair 1: getAveragePower on a real absent key and a key that does not exist. Fires on
        // every API level; this is the pair that fires on the ANE-LX2. Until the reads land the
        // pair renders its title row and a secondary line (§4.5's below-behaviour shape).
        item {
            if (pair1 == null) {
                Text(
                    text = "reading…",
                    style = SipperType.material.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            } else {
                SilentDefaultDemo(
                    caption = "getAveragePower on a real absent key, and on a key that does not exist",
                    left = pair1!![0].asSide("cpu.cluster_power.cluster0"),
                    right = pair1!![1].asSide("sipper.nonsense.key"),
                    sinceApi = 28,
                    behaviourApi = 28,
                )
            }
        }
    }
}

/** The platform's return rendered as the pair's quote: 0.0 for both Value and SilentDefault. */
private fun Reading<Double>.asSide(label: String): ProbeSide = when (this) {
    is Reading.Value -> ProbeSide(label, value.toString(), this)
    is Reading.SilentDefault ->
        @OptIn(io.github.tahmid1999.sipper.audit.AuditOnly::class)
        ProbeSide(label, shown.toString(), this)
    else -> ProbeSide(label, "—", this)
}


@Composable
private fun PairHeader() {
    val colors = LocalSipperColors.current
    Text(
        text = "paired demonstrations",
        style = SipperType.material.titleSmall,
        color = colors.textPrimary,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
