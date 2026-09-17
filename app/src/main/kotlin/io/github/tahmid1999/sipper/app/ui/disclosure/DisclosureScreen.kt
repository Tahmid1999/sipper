package io.github.tahmid1999.sipper.app.ui.disclosure

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

@Composable
public fun DisclosureScreen(
    onDismiss: () -> Unit = {},
) {
    val colors = LocalSipperColors.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "USAGE ACCESS DISCLOSURE",
            style = SipperType.monoChip,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "sipper reads per-app foreground time and network activity to reconstruct resource usage windows on your device.\n\n" +
                    "• Data collected never leaves this device.\n" +
                    "• No network servers, no analytics, no third-party SDKs.\n" +
                    "• You can revoke this permission anytime in Android Settings.",
            style = SipperType.monoBlock,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("Not now", style = SipperType.monoChip)
            }

            Button(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                    onDismiss()
                }
            ) {
                Text("Open Settings", style = SipperType.monoChip)
            }
        }
    }
}
