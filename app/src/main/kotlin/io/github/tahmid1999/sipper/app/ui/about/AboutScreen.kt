package io.github.tahmid1999.sipper.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.DashboardCard
import io.github.tahmid1999.sipper.app.ui.LivePulseDot
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import io.github.tahmid1999.sipper.app.R

@Composable
fun AboutScreen(
    onDismiss: (() -> Unit)? = null,
    themeMode: io.github.tahmid1999.sipper.app.ui.theme.ThemeMode = io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK,
    onToggleTheme: () -> Unit = {}
) {
    val colors = LocalSipperColors.current
    val scrollState = rememberScrollState()

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = io.github.tahmid1999.sipper.app.ui.theme.isDarkMode(themeMode, systemDark)
    val logoRes = if (isDark) R.drawable.app_logo else R.drawable.app_logo_light

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Banner with Dynamic Logo & Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hardware & Power Audit",
                    style = SipperType.material.headlineSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LivePulseDot()
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v1.0.0",
                        style = SipperType.monoChip,
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Strict-Purity Telemetry",
                        style = SipperType.monoChip,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Appearance & Theme Switcher Card with Live Logo Preview
        DashboardCard(
            modifier = Modifier.clickable(onClick = onToggleTheme),
            title = "Appearance & Theme Mode",
            badge = if (isDark) "OLED DARK" else "CLEAN LIGHT"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceRaised)
                            .border(1.5.dp, colors.accent, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = "Dynamic App Logo Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDark) "OLED Pitch Black Theme" else "Clean Light Theme",
                            style = SipperType.material.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "App logo, splash screen, and dashboard colors adapt dynamically.",
                            style = SipperType.monoBlock,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onToggleTheme) {
                    Text(
                        text = if (isDark) "☀️ Light" else "🌙 Dark",
                        style = SipperType.monoChip,
                        color = colors.accent
                    )
                }
            }
        }

        // Developer Credit Card
        DashboardCard(title = "Developer Credit & Purpose", badge = "HOBBY PROJECT") {
            Text(
                text = "Developed by Tahmid Alavi Ishmam as a hobby project",
                style = SipperType.material.titleMedium,
                color = colors.accent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Built out of passion for low-level Android framework telemetry, hardware audit purity, and clean Jetpack Compose UI architecture.",
                style = SipperType.monoBlock,
                color = colors.textSecondary
            )
        }

        // Dashboard Tab Navigation Breakdown
        DashboardCard(title = "Tab Navigation Guide & Features", badge = "5 CORE TABS") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // AUDIT
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AUDIT", style = SipperType.monoStrong, color = colors.accent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• OEM Power Profile", style = SipperType.monoChip, color = colors.statePresent)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Audits framework-res.apk power_profile.xml across 3 system routes to verify hardware power constants and check profile completeness.",
                        style = SipperType.monoBlock,
                        color = colors.textSecondary
                    )
                }

                // APPS
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("APPS", style = SipperType.monoStrong, color = colors.accent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• Per-App Energy Impact", style = SipperType.monoChip, color = colors.statePresent)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ranks installed applications by estimated battery energy consumption (mAh), foreground execution time, and wake lock activity.",
                        style = SipperType.monoBlock,
                        color = colors.textSecondary
                    )
                }

                // SELF
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SELF", style = SipperType.monoStrong, color = colors.accent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• First-Party Vitals", style = SipperType.monoChip, color = colors.statePresent)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Monitors first-party zero-drain runtime vitals, WorkManager periodic sampling status, process identity (UID), and exit stability.",
                        style = SipperType.monoBlock,
                        color = colors.textSecondary
                    )
                }

                // PROBES
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PROBES", style = SipperType.monoStrong, color = colors.accent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• CPU & Kernel State", style = SipperType.monoChip, color = colors.statePresent)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real-time frequency scaling readouts per CPU core cluster, RAM & ZRAM swap pressure, and active Android kernel subsystems.",
                        style = SipperType.monoBlock,
                        color = colors.textSecondary
                    )
                }

                // DEVICE
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DEVICE", style = SipperType.monoStrong, color = colors.accent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• Hardware & Thermal Grid", style = SipperType.monoChip, color = colors.statePresent)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Live battery discharge sparklines, voltage/capacity statistics, and a 14-zone thermal sensor heat map grid.",
                        style = SipperType.monoBlock,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Key Principles & Architecture
        DashboardCard(title = "Core Technical Principles", badge = "PURITY CONTRACT") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("1. Strict Data Purity (Reading<T>)", style = SipperType.monoStrong, color = colors.statePresent)
                    Text("Never manufactures fake zero values or artificial numbers. Unpopulated system values render as hatched cells.", style = SipperType.monoBlock, color = colors.textSecondary)
                }
                Column {
                    Text("2. OEM Power Profile Audit", style = SipperType.monoStrong, color = colors.statePresent)
                    Text("Audits framework-res.apk power_profile.xml across 3 system routes to uncover OEM profile gaps.", style = SipperType.monoBlock, color = colors.textSecondary)
                }
                Column {
                    Text("3. Real-Time Hardware Telemetry", style = SipperType.monoStrong, color = colors.statePresent)
                    Text("Live CPU cluster frequency scaling, RAM & ZRAM swap utilization, 14-zone thermal heat map, and battery discharge sparklines.", style = SipperType.monoBlock, color = colors.textSecondary)
                }
            }
        }

        // Built With Stack
        DashboardCard(title = "Technology Stack", badge = "MODERN KOTLIN") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TechChip(label = "Language", value = "Kotlin 2.0")
                TechChip(label = "UI Toolkit", value = "Compose")
                TechChip(label = "Database", value = "SQLDelight")
            }
        }

        if (onDismiss != null) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Telemetry Dashboard", style = SipperType.monoChip)
            }
        }
    }
}

@Composable
private fun TechChip(label: String, value: String) {
    val colors = LocalSipperColors.current
    Column {
        Text(text = value, style = SipperType.material.titleMedium, color = colors.textPrimary)
        Text(text = label.uppercase(), style = SipperType.monoChip, color = colors.textSecondary)
    }
}
