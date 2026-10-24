package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RubyError
import com.example.ui.theme.VioletGlow
import com.example.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedOrb(
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    audioRms: Float = 0f,
    pulsationRate: Float = 1.0f,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val reduceMotion = rememberReducedMotion()
    val safeRate = pulsationRate.takeIf { it.isFinite() }?.coerceIn(0.5f, 2.0f) ?: 1.0f
    val infiniteTransition = if (reduceMotion) null else rememberInfiniteTransition(label = "orb_infinite")

    // Under reduced motion, omit the infinite transition entirely rather than looping at 1 ms.
    val rotation = if (infiniteTransition == null) {
        0f
    } else {
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween((10000f / safeRate).toInt().coerceIn(1000, 20000), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orb_rotation"
        )
        animated
    }
    val breathing = if (infiniteTransition == null) {
        1f
    } else {
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween((2400f / safeRate).toInt().coerceIn(1200, 4800), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_breathing"
        )
        animated
    }

    // Dynamic scale driven by audio RMS when speaking or listening
    val dynamicAudioScale = remember { Animatable(1f) }
    LaunchedEffect(audioRms, voiceState, reduceMotion) {
        val safeRms = audioRms.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        val target = if (reduceMotion) 1f else when (voiceState) {
            VoiceState.LISTENING -> (1f + safeRms * 0.45f).coerceIn(1f, 1.35f)
            VoiceState.SPEAKING -> (1f + (safeRms * 0.6f).coerceAtLeast(0.15f)).coerceIn(1f, 1.6f)
            VoiceState.PROCESSING -> 1.05f
            else -> 1f
        }
        if (reduceMotion) {
            dynamicAudioScale.snapTo(1f)
        } else {
            dynamicAudioScale.animateTo(target, animationSpec = tween(80, easing = LinearEasing))
        }
    }

    // Determine colors based on visual theme and voice state
    val (primaryGlow, secondaryGlow) = when (voiceState) {
        VoiceState.ERROR -> Pair(RubyError, Color(0xFFF43F5E))
        VoiceState.PROCESSING -> Pair(VioletGlow, NeonCyan)
        VoiceState.LISTENING -> Pair(NeonCyan, CyanGlow)
        VoiceState.SPEAKING -> Pair(CyanGlow, ElectricViolet)
        VoiceState.IDLE -> Pair(NeonCyan, ElectricViolet)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("kiaraa_animated_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (this.size.minDimension / 2) * 0.65f
            val currentScale = (if (reduceMotion) 1f else breathing) * dynamicAudioScale.value
            val effectiveRadius = baseRadius * currentScale

            // Layer 1: Ambient Outer Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.35f),
                        secondaryGlow.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.55f
                ),
                radius = effectiveRadius * 1.55f,
                center = center
            )

            // Layer 2: Rotating Orbital Ring 1
            val ringRadius1 = effectiveRadius * 1.25f
            drawCircle(
                color = primaryGlow.copy(alpha = 0.4f),
                radius = ringRadius1,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Dynamic orbit satellite dots
            val rad1 = Math.toRadians(rotation.toDouble())
            val dot1X = center.x + ringRadius1 * cos(rad1).toFloat()
            val dot1Y = center.y + ringRadius1 * sin(rad1).toFloat()
            drawCircle(
                color = primaryGlow,
                radius = 4.5f,
                center = Offset(dot1X, dot1Y)
            )

            // Layer 3: Counter-rotating Ring 2
            val ringRadius2 = effectiveRadius * 1.08f
            drawCircle(
                color = secondaryGlow.copy(alpha = 0.35f),
                radius = ringRadius2,
                center = center,
                style = Stroke(width = 2f)
            )

            val rad2 = Math.toRadians((-rotation * 1.4).toDouble())
            val dot2X = center.x + ringRadius2 * cos(rad2).toFloat()
            val dot2Y = center.y + ringRadius2 * sin(rad2).toFloat()
            drawCircle(
                color = secondaryGlow,
                radius = 3.5f,
                center = Offset(dot2X, dot2Y)
            )

            // Layer 4: Core Plasma Sphere with Multi-Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryGlow,
                        secondaryGlow,
                        Color(0xFF090D1A)
                    ),
                    center = Offset(center.x - effectiveRadius * 0.2f, center.y - effectiveRadius * 0.25f),
                    radius = effectiveRadius
                ),
                radius = effectiveRadius,
                center = center
            )

            // Layer 5: Concentric Soundwave Pulse during Listening/Speaking
            if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                val waveAlpha = (0.5f - (dynamicAudioScale.value - 1f)).coerceIn(0.1f, 0.6f)
                drawCircle(
                    color = primaryGlow.copy(alpha = waveAlpha),
                    radius = effectiveRadius * 1.4f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
