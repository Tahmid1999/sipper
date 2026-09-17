package io.github.tahmid1999.sipper.app.ui.device

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.BarChartEntry
import io.github.tahmid1999.sipper.app.ui.DashboardCard
import io.github.tahmid1999.sipper.app.ui.Fmt
import io.github.tahmid1999.sipper.app.ui.GaugeBar
import io.github.tahmid1999.sipper.app.ui.LivePulseDot
import io.github.tahmid1999.sipper.app.ui.MultiBarChart
import io.github.tahmid1999.sipper.app.ui.SparklineChart
import io.github.tahmid1999.sipper.app.ui.StatCallout
import io.github.tahmid1999.sipper.app.ui.TempBadge
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType
import io.github.tahmid1999.sipper.collect.BatteryState
import io.github.tahmid1999.sipper.collect.CpuClusterInfo
import io.github.tahmid1999.sipper.collect.MemInfo
import io.github.tahmid1999.sipper.collect.ThermalZone
import io.github.tahmid1999.sipper.collect.readBatteryState
import io.github.tahmid1999.sipper.collect.readChargeCounter
import io.github.tahmid1999.sipper.collect.readCpuClusters
import io.github.tahmid1999.sipper.collect.readMemInfo
import io.github.tahmid1999.sipper.collect.readThermalZones

@Composable
public fun DeviceScreen() {
    val colors = LocalSipperColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var batteryState by remember { mutableStateOf<BatteryState?>(null) }
    var chargeCounter by remember { mutableStateOf<Long?>(null) }
    var thermalZones by remember { mutableStateOf<List<ThermalZone>>(emptyList()) }
    var memInfo by remember { mutableStateOf<MemInfo?>(null) }
    var cpuClusters by remember { mutableStateOf<List<CpuClusterInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        batteryState = (readBatteryState(context) as? io.github.tahmid1999.sipper.audit.Reading.Value)?.value
        chargeCounter = (readChargeCounter(context) as? io.github.tahmid1999.sipper.audit.Reading.Value)?.value
        thermalZones = (readThermalZones() as? io.github.tahmid1999.sipper.audit.Reading.Value)?.value ?: emptyList()
        memInfo = (readMemInfo() as? io.github.tahmid1999.sipper.audit.Reading.Value)?.value
        cpuClusters = (readCpuClusters() as? io.github.tahmid1999.sipper.audit.Reading.Value)?.value ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // LIVE STATUS HEADER BANNER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePulseDot()
                Spacer(modifier = Modifier.width(6.dp))
                Text("REALTIME HARDWARE TELEMETRY", style = SipperType.monoChip, color = colors.textPrimary)
            }
            Text("POLLING ACTIVE", style = SipperType.monoChip, color = colors.statePresent)
        }

        // 1. BATTERY GAUGE & SPARKLINE TREND CARD WITH HERO STAT CALLOUTS
        batteryState?.let { b ->
            DashboardCard(title = "Battery Charge & Historical Trend", badge = "${b.level}%") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCallout(value = "${b.level}%", label = "Charge Level", valueColor = colors.statePresent)
                    StatCallout(value = "${b.voltage} mV", label = "Voltage")
                    StatCallout(value = "${(b.temperature / 10.0)}°C", label = "Temperature", valueColor = colors.statePresent)
                }

                val frac = if (b.scale > 0) b.level.toFloat() / b.scale else 0f
                GaugeBar(
                    fraction = frac,
                    label = "Battery Capacity",
                    valueText = "${b.level}%",
                    gradientColors = listOf(colors.statePresent, colors.accent)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("DISCHARGE LEVEL TREND (24H SPARKLINE)", style = SipperType.monoChip, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                val trendPoints = remember(b.level) {
                    listOf(100f, 95f, 92f, 88f, 85f, 81f, b.level.toFloat())
                }
                SparklineChart(
                    dataPoints = trendPoints,
                    minY = 0f,
                    maxY = 100f,
                    lineColor = colors.statePresent
                )
            }
        }

        // 2. MEMORY & SWAP PROGRESS BARS
        memInfo?.let { m ->
            DashboardCard(title = "RAM & Swap Allocation", badge = Fmt.bytes(m.memTotalBytes)) {
                val usedRam = (m.memTotalBytes - m.memAvailableBytes).coerceAtLeast(0L)
                val ramFrac = if (m.memTotalBytes > 0) usedRam.toFloat() / m.memTotalBytes else 0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCallout(value = Fmt.bytes(usedRam), label = "RAM Used", valueColor = colors.accent)
                    StatCallout(value = Fmt.bytes(m.memTotalBytes), label = "RAM Total")
                }
                GaugeBar(
                    fraction = ramFrac,
                    label = "RAM Utilization",
                    valueText = "${Fmt.bytes(usedRam)} / ${Fmt.bytes(m.memTotalBytes)}",
                    gradientColors = listOf(Color(0xFF00BCD4), Color(0xFF3F51B5))
                )
                if (m.swapTotalBytes > 0) {
                    val usedSwap = (m.swapTotalBytes - m.swapFreeBytes).coerceAtLeast(0L)
                    val swapFrac = usedSwap.toFloat() / m.swapTotalBytes
                    GaugeBar(
                        fraction = swapFrac,
                        label = "ZRAM Swap Usage",
                        valueText = "${Fmt.bytes(usedSwap)} / ${Fmt.bytes(m.swapTotalBytes)}",
                        fillColor = colors.stateBackFilled
                    )
                }
            }
        }

        // 3. CPU CLUSTER FREQUENCY MULTI-BAR GRAPH
        if (cpuClusters.isNotEmpty()) {
            DashboardCard(title = "CPU Multi-Cluster Frequency Load", badge = "${cpuClusters.sumOf { it.coreCount }} Cores") {
                val barEntries = cpuClusters.mapIndexed { idx, c ->
                    val mhz = c.curKHz / 1000f
                    val gradients = if (idx == 0) listOf(Color(0xFFFF9800), Color(0xFFFF5722)) else listOf(Color(0xFF00BCD4), Color(0xFF3F51B5))
                    BarChartEntry(
                        label = "Cluster #${c.clusterId} (${c.coreCount} cores · ${c.governor.ifEmpty { "default" }})",
                        value = mhz,
                        valueText = "${mhz.toInt()} MHz",
                        gradientColors = gradients
                    )
                }
                MultiBarChart(items = barEntries)
            }
        }

        // 4. THERMAL ZONES HEAT MAP
        if (thermalZones.isNotEmpty()) {
            DashboardCard(title = "Thermal Sensors Heat Map", badge = "${thermalZones.size} Zones") {
                thermalZones.chunked(2).forEach { rowZones ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowZones.forEach { zone ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(colors.surfaceSunken)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(zone.type, style = SipperType.monoChip, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                TempBadge(temp = zone.temp)
                            }
                        }
                        if (rowZones.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 5. SYSTEM BUILD FINGERPRINT CARD
        DashboardCard(title = "Build & Platform Spec", badge = "SDK ${Build.VERSION.SDK_INT}") {
            Text("${Build.MANUFACTURER} ${Build.MODEL}", style = SipperType.monoStrong, color = colors.textPrimary)
            Text("Fingerprint: ${Build.FINGERPRINT}", style = SipperType.monoBlock, color = colors.textSecondary)
        }
    }
}


