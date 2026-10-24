package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight, native Compose clock inspired by Componentry's Lens Clock.
 * Updates once per second and exposes the current time to TalkBack.
 */
@Composable
fun LensClock(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            now = current
            delay(1_000L - (current % 1_000L))
        }
    }

    val calendar = remember(now) { java.util.Calendar.getInstance().apply { timeInMillis = now } }
    val hour = calendar.get(java.util.Calendar.HOUR).toFloat() +
        calendar.get(java.util.Calendar.MINUTE) / 60f
    val minute = calendar.get(java.util.Calendar.MINUTE).toFloat()
    val second = calendar.get(java.util.Calendar.SECOND).toFloat()

    val accessibleTime = remember(now) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
    }

    Canvas(modifier = modifier.size(size).semantics {
        contentDescription = "Analog clock, $accessibleTime"
    }) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.46f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x335DCEA0), Color(0x1AD4B483), Color(0x00101820)),
                center = center,
                radius = radius * 1.35f
            ),
            radius = radius * 1.35f,
            center = center
        )
        drawCircle(
            color = GlassBorderSubtle,
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        for (tick in 0 until 12) {
            val angle = Math.toRadians((tick * 30.0) - 90.0)
            val inner = radius * if (tick % 3 == 0) 0.72f else 0.82f
            val outer = radius * 0.91f
            drawLine(
                color = if (tick % 3 == 0) ElectricViolet else NeonCyan.copy(alpha = 0.55f),
                start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                strokeWidth = if (tick % 3 == 0) 1.4.dp.toPx() else 0.7.dp.toPx()
            )
        }

        fun hand(value: Float, fullTurn: Float, length: Float, color: Color, width: Float) {
            val angle = Math.toRadians((value / fullTurn * 360.0) - 90.0)
            drawLine(
                color = color,
                start = center,
                end = Offset(center.x + cos(angle).toFloat() * radius * length, center.y + sin(angle).toFloat() * radius * length),
                strokeWidth = width,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        hand(hour, 12f, 0.48f, Color(0xFFF6F3EC), 1.8.dp.toPx())
        hand(minute, 60f, 0.68f, NeonCyan, 1.2.dp.toPx())
        hand(second, 60f, 0.74f, ElectricViolet.copy(alpha = 0.85f), 0.7.dp.toPx())
        drawCircle(color = Color(0xFFF6F3EC), radius = 1.6.dp.toPx(), center = center)
    }
}
