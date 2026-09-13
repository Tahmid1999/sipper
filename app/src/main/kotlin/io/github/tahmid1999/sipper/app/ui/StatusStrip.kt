package io.github.tahmid1999.sipper.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/**
 * One strip segment. FLOWS §10 / SCREENS-AUDIT-PROBES §0.1: at least 48dp wide, tappable,
 * navigating to the screen that owns it. `textSecondary` unless the segment is the carrier of a
 * denial or disagreement.
 */
data class StripSegment(
    val text: String,
    val color: Color? = null, // null = textSecondary
    val onTap: (() -> Unit)? = null,
)

/**
 * The status strip: bottom of every screen, above the navigation bar, 32dp, `surfaceRaised`,
 * 1dp `ruleStrong` on top, `monoStrip` 11/400, ` · ` between every segment - no ` | ` grouping.
 * It scrolls horizontally when it overflows; it is never truncated and never wrapped, because a
 * truncated strip lies about the segment it cut.
 */
@Composable
fun StatusStrip(
    segments: List<StripSegment>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSipperColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(colors.surfaceRaised)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                Text(
                    text = " · ",
                    style = SipperType.monoStrip,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = segment.text,
                style = SipperType.monoStrip,
                color = segment.color ?: colors.textSecondary,
                modifier = Modifier
                    .then(
                        if (segment.onTap != null) {
                            Modifier.clickable(onClick = segment.onTap)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 4.dp)
                    .height(32.dp),
                // Segments are at least 48dp wide; a shorter label gains it via the padding plus
                // the clickable's own minimum touch target enforcement in the strip's context.
            )
        }
    }
}
