package io.github.tahmid1999.sipper.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType
import io.github.tahmid1999.sipper.collect.Temp

/**
 * Animated live pulsing dot indicator for real-time hardware telemetry.
 */
@Composable
fun LivePulseDot(
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val colors = LocalSipperColors.current
    val dotColor = color ?: colors.statePresent
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = alpha))
    )
}

/**
 * Prominent hero stat callout with large numerical typography.
 */
@Composable
fun StatCallout(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    val colors = LocalSipperColors.current
    Column(modifier = modifier) {
        Text(
            text = value,
            style = SipperType.material.headlineMedium,
            color = valueColor ?: colors.textNumeric
        )
        Text(
            text = label.uppercase(),
            style = SipperType.monoChip,
            color = colors.textSecondary
        )
    }
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    badge: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalSipperColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceRaised)
            .border(1.dp, colors.rule, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = SipperType.monoChip,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceSunken)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = SipperType.monoChip,
                            color = colors.accent
                        )
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun GaugeBar(
    fraction: Float,
    label: String,
    valueText: String,
    fillColor: Color? = null,
    gradientColors: List<Color>? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSipperColors.current
    val activeColor = fillColor ?: colors.accent
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "GaugeAnim"
    )

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = SipperType.mono, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Text(valueText, style = SipperType.mono, color = colors.textSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceSunken)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (gradientColors != null && gradientColors.size >= 2) {
                            Modifier.background(Brush.horizontalGradient(gradientColors))
                        } else {
                            Modifier.background(activeColor)
                        }
                    )
            )
        }
    }
}

@Composable
fun TempBadge(temp: Temp) {
    val colors = LocalSipperColors.current
    val (text, badgeColor) = when (temp) {
        is Temp.Celsius -> {
            val c = temp.c
            val color = when {
                c < 40.0 -> colors.statePresent
                c < 55.0 -> colors.stateExplicitZero
                else -> colors.stateAbsentZero
            }
            "%.1f°C".format(c) to color
        }
        is Temp.Sentinel -> "sentinel" to colors.textSecondary
        is Temp.Unclassified -> "${temp.raw}" to colors.textSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.2f))
            .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = SipperType.monoChip, color = badgeColor)
    }
}

@Composable
fun CoverageRing(
    percentage: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalSipperColors.current
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "RingAnim"
    )

    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            Canvas(modifier = Modifier.size(64.dp)) {
                drawArc(
                    color = colors.surfaceSunken,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(colors.statePresent, colors.accent, colors.statePresent)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedPercentage * 360f,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "${(animatedPercentage * 100).toInt()}%",
                style = SipperType.monoStrong,
                color = colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("HARDWARE PROFILE", style = SipperType.monoChip, color = colors.textSecondary)
            Text(label, style = SipperType.material.titleSmall, color = colors.textPrimary)
        }
    }
}

