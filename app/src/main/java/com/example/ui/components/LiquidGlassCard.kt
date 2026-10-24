package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SurfaceGlass

/** Low-cost KIARAA ambient backdrop; avoids heavy WebGL on older Android devices. */
fun Modifier.liquidGlassBackground(): Modifier = this.drawBehind {
    drawRect(ObsidianDark)
    val radius = maxOf(size.width, size.height) * 0.82f
    val mintCenter = Offset(size.width * 0.16f, size.height * 0.10f)
    val goldCenter = Offset(size.width * 0.92f, size.height * 0.64f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x245DCEA0), Color.Transparent),
            center = mintCenter,
            radius = radius
        ),
        radius = radius,
        center = mintCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x22D4B483), Color.Transparent),
            center = goldCenter,
            radius = radius * 0.78f
        ),
        radius = radius * 0.78f,
        center = goldCenter
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderColor: Color = GlassBorderCyan,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = SurfaceGlass,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val topTint = backgroundColor.copy(alpha = (backgroundColor.alpha * 0.72f).coerceIn(0.24f, 0.70f))
    val lowerTint = backgroundColor.copy(alpha = (backgroundColor.alpha * 0.55f).coerceIn(0.18f, 0.56f))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, clip = false)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.085f),
                        topTint,
                        lowerTint,
                        Color.Black.copy(alpha = 0.13f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        borderColor.copy(alpha = 0.74f),
                        GlassBorderSubtle
                    )
                ),
                shape = shape
            )
            .then(clickableModifier)
            .padding(16.dp),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color(0x66D4B483),
                                Color.White.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
            )
            content()
        }
    )
}
