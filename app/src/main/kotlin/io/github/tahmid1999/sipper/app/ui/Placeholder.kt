package io.github.tahmid1999.sipper.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/**
 * A named, honest placeholder: each is replaced by its screen's step in build order. FLOWS §1.1
 * says four of five screens work before any ask; these are the not-yet-built ones.
 */
@Composable
fun Placeholder(label: String) {
    val colors = LocalSipperColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = SipperType.material.bodyMedium, color = colors.textSecondary)
    }
}
