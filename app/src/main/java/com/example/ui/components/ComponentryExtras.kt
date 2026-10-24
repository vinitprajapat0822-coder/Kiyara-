package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryItem
import com.example.ui.theme.CreamText
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.MintSoft
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Compose counterparts to selected Componentry interaction patterns.
 * These are intentionally lightweight and avoid web-only dependencies and WebGL.
 */
@Composable
fun ComponentryStatusFlap(status: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val accent = when {
        status.contains("ERROR", ignoreCase = true) -> Color(0xFFE87979)
        status.contains("THINK", ignoreCase = true) -> GoldAccent
        else -> MintPrimary
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(MintSoft)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                if (motionEnabled) {
                    (fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 3 }) togetherWith
                        (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 4 })
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "kiaraa_status_flap"
        ) { label ->
            Text(
                text = label,
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ComponentryWelcomeCard(status: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        shape = RoundedCornerShape(23.dp),
        backgroundColor = SurfaceElevated.copy(alpha = 0.92f),
        borderColor = MintPrimary.copy(alpha = 0.44f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = if (motionEnabled) {
                            fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 7 }
                        } else EnterTransition.None
                    ) {
                        ComponentryKineticTextReveal(
                            text = "A little space for your thoughts."
                        )
                    }
                    Text(
                        text = "Haanji, batao kya chal raha hai?",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    ComponentryStatusFlap(status = status)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    LensClock(size = 48.dp)
                    Text(
                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nowMillis)),
                        color = GoldAccent,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            ComponentryLayeredStack(
                title = "SAATHI • LIVE BAAT",
                detail = "Chat, voice and memory in one calm workspace.",
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.fillMaxWidth().height(18.dp)) {
                ComponentryDitherGradient(Modifier.fillMaxSize())
                ComponentrySpectralRibbon(Modifier.fillMaxSize())
            }
            Text(
                text = "Your notes stay in the on-device vault. Phone actions ask for the access and confirmation they need.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            ComponentrySignature()
        }
    }
}

@Composable
private fun FeaturePill(title: String, detail: String) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceGlass)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 7.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, color = MintPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(detail, color = TextMuted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun ComponentryMemoryDeck(memories: List<MemoryItem>, modifier: Modifier = Modifier) {
    if (memories.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "RECENT IN YOUR VAULT",
            color = GoldAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories.take(4), key = { it.id }) { memory ->
                val accent = if (memory.id % 2L == 0L) MintPrimary else GoldAccent
                LiquidGlassCard(
                    modifier = Modifier.width(168.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = SurfaceGlass,
                    borderColor = accent.copy(alpha = 0.42f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = memory.category.uppercase(Locale.getDefault()),
                            color = accent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = memory.title,
                            color = CreamText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnnotatedAssistantText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val annotated = remember(text) {
        buildAnnotatedString {
            val token = Regex("""\*\*(.+?)\*\*|\x60([^\x60]+)\x60|(?m)^#{1,3}\s+.+$""")
            var cursor = 0
            token.findAll(text).forEach { match ->
                if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
                when {
                    match.groups[1] != null -> withStyle(SpanStyle(color = MintPrimary, fontWeight = FontWeight.SemiBold)) {
                        append(match.groups[1]!!.value)
                    }
                    match.groups[2] != null -> withStyle(SpanStyle(color = GoldAccent, fontFamily = FontFamily.Monospace)) {
                        append(match.groups[2]!!.value)
                    }
                    else -> withStyle(SpanStyle(color = MintPrimary, fontWeight = FontWeight.Bold)) {
                        append(match.value.trimStart('#', ' '))
                    }
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = CreamText,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}
