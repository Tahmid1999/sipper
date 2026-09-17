package io.github.tahmid1999.sipper.app.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/**
 * A smooth Compose Canvas Sparkline line chart for telemetry trends (battery level %, temperatures).
 */
@Composable
fun SparklineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
    gradientColor: Color? = null,
    minY: Float = 0f,
    maxY: Float = 100f,
) {
    val colors = LocalSipperColors.current
    val strokeColor = lineColor ?: colors.accent
    val fillColor = gradientColor ?: strokeColor.copy(alpha = 0.2f)

    if (dataPoints.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceSunken)
            .border(1.dp, colors.rule, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(84.dp)) {
            val width = size.width
            val height = size.height
            val rangeY = (maxY - minY).coerceAtLeast(1f)
            val stepX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

            val path = Path()
            val fillPath = Path()

            dataPoints.forEachIndexed { index, value ->
                val x = index * stepX
                val normalizedY = ((value - minY) / rangeY).coerceIn(0f, 1f)
                val y = height - (normalizedY * height)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevNormY = ((dataPoints[index - 1] - minY) / rangeY).coerceIn(0f, 1f)
                    val prevY = height - (prevNormY * height)

                    // Smooth cubic bezier segment
                    val controlX1 = prevX + (x - prevX) / 2f
                    val controlY1 = prevY
                    val controlX2 = prevX + (x - prevX) / 2f
                    val controlY2 = y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw gradient area below curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw sparkline stroke
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data point dots
            dataPoints.forEachIndexed { index, value ->
                val x = index * stepX
                val normalizedY = ((value - minY) / rangeY).coerceIn(0f, 1f)
                val y = height - (normalizedY * height)
                drawCircle(color = strokeColor, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

/**
 * A horizontal multi-bar chart displaying relative values (CPU frequencies, mAh drain comparisons).
 */
@Composable
fun MultiBarChart(
    items: List<BarChartEntry>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSipperColors.current
    val maxVal = items.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        items.forEach { entry ->
            val fraction = (entry.value / maxVal).coerceIn(0f, 1f)
            val animatedFrac by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600),
                label = "BarAnim"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.label, style = SipperType.mono, color = colors.textPrimary)
                    Text(entry.valueText, style = SipperType.monoStrong, color = entry.barColor ?: colors.accent)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.surfaceSunken)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFrac)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .then(
                                if (entry.gradientColors != null && entry.gradientColors.size >= 2) {
                                    Modifier.background(Brush.horizontalGradient(entry.gradientColors))
                                } else {
                                    Modifier.background(entry.barColor ?: colors.accent)
                                }
                            )
                    )
                }
            }
        }
    }
}

data class BarChartEntry(
    val label: String,
    val value: Float,
    val valueText: String,
    val barColor: Color? = null,
    val gradientColors: List<Color>? = null,
)

