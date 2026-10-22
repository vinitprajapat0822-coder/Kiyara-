package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.voice.VoiceState
import kotlin.math.sin

@Composable
fun AudioWaveformBar(
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
    audioRms: Float = 0f,
    barCount: Int = 18,
    activeColor: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val isActive = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING

    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val sineVal = sin(phase + (i * 0.45f)).toFloat()
            val dynamicHeight = if (isActive) {
                val base = 8.dp
                val amplitude = 26.dp * (0.3f + audioRms * 0.7f)
                base + (amplitude * (sineVal + 1f) / 2f)
            } else {
                5.dp
            }

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(dynamicHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) activeColor.copy(alpha = 0.5f + (sineVal + 1f) * 0.25f)
                        else activeColor.copy(alpha = 0.2f)
                    )
            )
        }
    }
}
