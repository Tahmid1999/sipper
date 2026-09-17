package io.github.tahmid1999.sipper.app.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tahmid1999.sipper.app.R
import io.github.tahmid1999.sipper.app.ui.LivePulseDot
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val colors = LocalSipperColors.current
    var visible by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        delay(2200)
        visible = false
        delay(300) // allow fade-out animation to complete
        onSplashFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // High-Tech Microchip Logo Badge with Pulse Scale
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(130.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceRaised)
                        .border(1.5.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val logoRes = if (colors.surface == androidx.compose.ui.graphics.Color(0xFF000000) || colors.surface == androidx.compose.ui.graphics.Color(0xFF0F1216)) {
                        R.drawable.app_logo
                    } else {
                        R.drawable.app_logo_light
                    }
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "Hardware & Power Audit Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Title
                Text(
                    text = "HARDWARE & POWER AUDIT",
                    style = SipperType.material.titleSmall.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle with Live Pulse Dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LivePulseDot()
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Strict-Purity Android Telemetry",
                        style = SipperType.monoChip.copy(fontSize = 11.sp),
                        color = colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Progress Indicator
                Box(modifier = Modifier.width(160.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape),
                        color = colors.accent,
                        trackColor = colors.surfaceSunken
                    )
                }
            }
        }
    }
}
