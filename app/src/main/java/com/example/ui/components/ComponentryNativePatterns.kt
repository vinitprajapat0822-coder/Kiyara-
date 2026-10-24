package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreamText
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.MintSoft
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlin.math.sin

private fun motionAllowed(): Boolean = true

/** GrainGradient native port: restrained radial color fields plus deterministic fine grain. */
fun Modifier.componentryGrainGradient(): Modifier = drawBehind {
    val radius = maxOf(size.width, size.height) * 0.75f
    drawRect(ObsidianDark)
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0x225DCEA0), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width * .14f, size.height * .12f), radius = radius),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(size.width * .14f, size.height * .12f)
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0x1BD4B483), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width * .92f, size.height * .66f), radius = radius * .82f),
        radius = radius * .82f,
        center = androidx.compose.ui.geometry.Offset(size.width * .92f, size.height * .66f)
    )
    // Fixed pseudo-random pattern: no per-frame work and no external texture.
    for (i in 0 until 84) {
        val x = ((i * 73) % 997) / 997f * size.width
        val y = ((i * 193 + 37) % 991) / 991f * size.height
        drawCircle(Color.White.copy(alpha = if (i % 7 == 0) .035f else .012f), radius = if (i % 11 == 0) 1.1f else .55f, center = androidx.compose.ui.geometry.Offset(x, y))
    }
}

/** SpectralRibbon native port: a static mint/gold wave that is inexpensive on older phones. */
@Composable
fun ComponentrySpectralRibbon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val mid = size.height * .56f
        for (ribbon in 0..2) {
            val path = Path()
            val phase = ribbon * 1.3f
            path.moveTo(0f, mid)
            for (step in 0..48) {
                val x = size.width * step / 48f
                val y = mid + sin((step / 48f * 6.28318f) + phase).toFloat() * size.height * (.12f + ribbon * .045f)
                path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = if (ribbon == 1) GoldAccent.copy(alpha = .14f) else MintPrimary.copy(alpha = .12f - ribbon * .02f),
                style = Stroke(width = if (ribbon == 0) 1.2.dp.toPx() else .7.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/** DitherGradient native port: a static low-contrast dotted gradient, not WebGL. */
@Composable
fun ComponentryDitherGradient(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val step = 11.dp.toPx()
        var row = 0
        var y = 0f
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else step / 2f
            while (x < size.width) {
                val edge = (1f - (x / size.width).coerceIn(0f, 1f)) * .045f
                drawCircle(
                    color = if ((row + (x / step).toInt()) % 5 == 0) GoldAccent.copy(alpha = edge) else MintPrimary.copy(alpha = edge * .7f),
                    radius = .65.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
                x += step
            }
            y += step
            row++
        }
    }
}

/** KineticTextReveal native port: short, one-shot entrance and full reduced-motion respect. */
@Composable
fun ComponentryKineticTextReveal(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var shown by remember(text) { mutableStateOf(reduceMotion) }
    LaunchedEffect(text, reduceMotion) {
        if (!reduceMotion) {
            shown = false
            delay(45L)
            shown = true
        }
    }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, animationSpec = tween(if (reduceMotion) 0 else 260), label = "kinetic_text_alpha")
    val offset by animateFloatAsState(if (shown) 0f else 8f, animationSpec = tween(if (reduceMotion) 0 else 260), label = "kinetic_text_offset")
    Text(
        text = text,
        modifier = modifier.graphicsLayer { this.alpha = alpha; translationY = offset },
        color = CreamText,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp
    )
}

/** TextMorph native port: status labels transition without restarting the full screen. */
@Composable
fun ComponentryTextMorph(status: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    AnimatedContent(
        targetState = status,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) EnterTransition.None togetherWith ExitTransition.None
            else (fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 3 }) togetherWith
                (fadeOut(tween(100)) + slideOutVertically(tween(100)) { -it / 4 })
        },
        label = "componentry_text_morph"
    ) { value ->
        Text(value, color = MintPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

/** HoverTransition native port: touch press uses a tiny scale and never requires a mouse. */
@Composable
fun ComponentryHoverTransition(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .975f else 1f, animationSpec = tween(110), label = "componentry_press_scale")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        content = content
    )
}

/** LayeredStack native port: compact glass layers for a memory/feature preview. */
@Composable
fun ComponentryLayeredStack(
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(top = 5.dp, end = 5.dp, bottom = 2.dp)) {
        Box(Modifier.matchParentSize().padding(start = 5.dp, top = 5.dp).clip(RoundedCornerShape(14.dp)).background(GoldAccent.copy(alpha = .12f)))
        Box(Modifier.matchParentSize().padding(start = 2.dp, top = 2.dp).clip(RoundedCornerShape(14.dp)).background(MintPrimary.copy(alpha = .10f)))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceGlass)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            androidx.compose.foundation.layout.Column {
                Text(title, color = MintPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

/** OrbitCardStack native port: card strip for a short list; cards remain ordinary accessible Compose content. */
@Composable
fun ComponentryOrbitCardStack(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    ComponentryHoverTransition(onClick = onClick, modifier = modifier) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .background(SurfaceGlass, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(title, color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = CreamText, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

/** Signature native port: quiet wordmark, not a fake handwritten font. */
@Composable
fun ComponentrySignature(modifier: Modifier = Modifier) {
    Text(
        text = "— KIARAA",
        modifier = modifier,
        color = GoldAccent.copy(alpha = .9f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Cursive
    )
}

/** MagneticDock native port: selected destinations get a spring-like scale without mouse-only interaction. */
@Composable
fun ComponentryDockHighlight(selected: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, animationSpec = tween(160), label = "dock_item_scale")
    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
        content()
    }
}
