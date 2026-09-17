package io.github.tahmid1999.sipper.app.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.BarChartEntry
import io.github.tahmid1999.sipper.app.ui.DashboardCard
import io.github.tahmid1999.sipper.app.ui.Fmt
import io.github.tahmid1999.sipper.app.ui.GaugeBar
import io.github.tahmid1999.sipper.app.ui.LivePulseDot
import io.github.tahmid1999.sipper.app.ui.MultiBarChart
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

@Composable
public fun AppsScreen(
    onNavigateToDisclosure: () -> Unit = {},
) {
    val colors = LocalSipperColors.current
    var queryText by remember { mutableStateOf("") }

    val sampleApps = remember {
        listOf(
            Triple("com.google.android.youtube", "YouTube", 3600000L),
            Triple("com.android.chrome", "Chrome", 1240000L),
            Triple("com.spotify.music", "Spotify", 890000L),
            Triple("com.whatsapp", "WhatsApp", 450000L),
            Triple("io.github.tahmid1999.sipper", "sipper", 180000L),
        )
    }

    val maxFgMs = remember(sampleApps) { sampleApps.maxOfOrNull { it.third } ?: 1L }
    val totalFgMs = remember(sampleApps) { sampleApps.sumOf { it.third } }

    val filteredApps = sampleApps.filter {
        it.first.contains(queryText, ignoreCase = true) || it.second.contains(queryText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(14.dp)
    ) {
        // LIVE STATUS HEADER BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePulseDot()
                Spacer(modifier = Modifier.width(6.dp))
                Text("APP FOREGROUND ACTIVITY & POWER DRAIN", style = SipperType.monoChip, color = colors.textPrimary)
            }
            Text("WINDOW ACTIVE", style = SipperType.monoChip, color = colors.accent)
        }

        // Search Header
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            label = { Text("Filter package / app label", style = SipperType.monoChip) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textStyle = SipperType.mono
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardCard(title = "App Energy Impact Graph (mAh)", badge = "${filteredApps.size} Apps Tracked") {
                    val palette = listOf(
                        listOf(Color(0xFF3F51B5), Color(0xFF2196F3)),
                        listOf(Color(0xFF00BCD4), Color(0xFF009688)),
                        listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)),
                        listOf(Color(0xFFFF9800), Color(0xFFFFC107)),
                        listOf(Color(0xFF9C27B0), Color(0xFFE91E63))
                    )
                    val entries = filteredApps.mapIndexed { idx, (pkg, label, fgMs) ->
                        val mah = fgMs / 3600000.0 * 250.0
                        BarChartEntry(
                            label = label,
                            value = mah.toFloat(),
                            valueText = "%.1f mAh".format(mah),
                            gradientColors = palette[idx % palette.size]
                        )
                    }
                    MultiBarChart(items = entries)
                }
            }

            items(filteredApps) { (pkg, label, fgMs) ->
                val frac = fgMs.toFloat() / maxFgMs
                val mahEst = "%.1f".format(fgMs / 3600000.0 * 250.0) // estimate
                DashboardCard(title = label, badge = pkg) {
                    GaugeBar(
                        fraction = frac,
                        label = "Foreground Session Time",
                        valueText = Fmt.hm(fgMs),
                        gradientColors = listOf(colors.statePresent, colors.accent)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Framework FG Time: ${Fmt.hm(fgMs)}", style = SipperType.monoChip, color = colors.textSecondary)
                        Text("Est. Drain: $mahEst mAh", style = SipperType.monoChip, color = colors.accent)
                    }
                }
            }
        }
    }
}



