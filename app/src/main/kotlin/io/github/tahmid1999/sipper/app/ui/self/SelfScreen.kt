package io.github.tahmid1999.sipper.app.ui.self

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.DashboardCard
import io.github.tahmid1999.sipper.app.ui.Fmt
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

@Composable
public fun SelfScreen(
    onNavigateToAbout: () -> Unit = {},
) {
    val colors = LocalSipperColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SELF — FIRST-PARTY READBACK & VITALS", style = SipperType.monoChip, color = colors.textSecondary)

        // 0. ABOUT & DEVELOPER CREDITS CARD
        DashboardCard(title = "About This App & Developer Credits", badge = "HOBBY PROJECT") {
            Text(
                text = "Developed by Tahmid Alavi Ishmam as a hobby project",
                style = SipperType.material.titleSmall,
                color = colors.accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "sipper is a strict-purity Android hardware & power telemetry dashboard with zero manufactured numbers.",
                style = SipperType.monoBlock,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNavigateToAbout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Full About & Technical Architecture", style = SipperType.monoChip)
            }
        }

        // 1. WHAT RUNS IN THE BACKGROUND
        DashboardCard(title = "Background work") {
            Text(
                "sipper declares no foreground service and starts none. Its only background work is " +
                    "the WorkManager sampler below.",
                style = SipperType.monoBlock,
                color = colors.textSecondary,
            )
        }

        // 2. SAMPLER STATUS
        DashboardCard(title = "Background Sampler Service") {
            Text("WorkManager Periodic Task", style = SipperType.monoStrong, color = colors.textPrimary)
            Text("Schedule: Every 6 hours · Non-blocking background worker", style = SipperType.monoBlock, color = colors.textSecondary)
        }

        // 3. IDENTITY CARD
        DashboardCard(title = "1st Party Identity & Package", badge = "UID ${Process.myUid()}") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Package Name", style = SipperType.mono, color = colors.textSecondary)
                Text(context.packageName, style = SipperType.mono, color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Target SDK", style = SipperType.mono, color = colors.textSecondary)
                Text("${context.applicationInfo.targetSdkVersion}", style = SipperType.mono, color = colors.textPrimary)
            }
        }

        // 4. PROCESS EXIT RECORDS CARD
        DashboardCard(title = "Exit records", badge = "API ${Build.VERSION.SDK_INT}") {
            if (Build.VERSION.SDK_INT >= 30) {
                ExitRecords()
            } else {
                Text(
                    "absent · getHistoricalProcessExitReasons requires API 30; this device is ${Build.VERSION.SDK_INT}",
                    style = SipperType.monoBlock,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@RequiresApi(30)
@Composable
private fun ExitRecords() {
    val colors = LocalSipperColors.current
    val context = LocalContext.current
    val records = remember {
        val am = context.getSystemService(ActivityManager::class.java)
        am.getHistoricalProcessExitReasons(context.packageName, 0, 16)
    }
    if (records.isEmpty()) {
        Text(
            "no exit records for this package yet",
            style = SipperType.monoBlock,
            color = colors.textSecondary,
        )
        return
    }
    records.forEach { r ->
        Text(
            "${Fmt.timestamp(r.timestamp)}  ${exitReasonName(r.reason)} (${r.reason})  " +
                "status ${r.status}  pss ${Fmt.bytes(r.pss * 1024)}",
            style = SipperType.monoBlock,
            color = colors.textPrimary,
        )
        r.description?.let {
            Text(it, style = SipperType.monoBlock, color = colors.textSecondary)
        }
    }
}

// Names for the REASON_* constants at compileSdk 36. A newer platform can return an int this
// table does not know, so the integer is always printed beside the name.
private fun exitReasonName(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_UNKNOWN -> "REASON_UNKNOWN"
    ApplicationExitInfo.REASON_EXIT_SELF -> "REASON_EXIT_SELF"
    ApplicationExitInfo.REASON_SIGNALED -> "REASON_SIGNALED"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "REASON_LOW_MEMORY"
    ApplicationExitInfo.REASON_CRASH -> "REASON_CRASH"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "REASON_CRASH_NATIVE"
    ApplicationExitInfo.REASON_ANR -> "REASON_ANR"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "REASON_INITIALIZATION_FAILURE"
    ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "REASON_PERMISSION_CHANGE"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "REASON_EXCESSIVE_RESOURCE_USAGE"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "REASON_USER_REQUESTED"
    ApplicationExitInfo.REASON_USER_STOPPED -> "REASON_USER_STOPPED"
    ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "REASON_DEPENDENCY_DIED"
    ApplicationExitInfo.REASON_OTHER -> "REASON_OTHER"
    ApplicationExitInfo.REASON_FREEZER -> "REASON_FREEZER"
    ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "REASON_PACKAGE_STATE_CHANGE"
    ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "REASON_PACKAGE_UPDATED"
    else -> "unnamed"
}
