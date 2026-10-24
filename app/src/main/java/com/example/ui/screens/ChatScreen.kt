package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.ui.MainViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LensClock
import com.example.ui.components.liquidGlassBackground
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletGlow
import com.example.voice.VoiceState

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val activeModelName by viewModel.activeModelName.collectAsState()
    val orbPulsation by viewModel.orbPulsationRate.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()

    val listState = rememberLazyListState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.voiceManager.startListening()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickChips = listOf(
        "💻 Android: Kotlin debugging",
        "🐧 Termux: Safe command guide",
        "📱 Phone: Open an app",
        "🎵 Music: Find a lo-fi playlist",
        "🧠 Memory: Save a preference",
        "📋 Daily tasks: Make a plan"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .liquidGlassBackground()
    ) {
        // Top Header Bar
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = SurfaceElevated.copy(alpha = 0.85f),
            borderColor = GlassBorderCyan
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedOrb(
                        voiceState = voiceState,
                        size = 38.dp,
                        audioRms = audioRms,
                                                pulsationRate = orbPulsation,
                        onClick = {
                            if (voiceState == VoiceState.SPEAKING) {
                                viewModel.voiceManager.stopSpeaking()
                            } else if (hasAudioPermission) {
                                if (voiceState == VoiceState.LISTENING) viewModel.voiceManager.stopListening()
                                else viewModel.voiceManager.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KIARAA AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        AnimatedContent(
                            targetState = voiceState,
                            label = "kiaraa-assistant-status"
                        ) { state ->
                            Text(
                                text = when (state) {
                                    VoiceState.LISTENING -> "Listening..."
                                    VoiceState.SPEAKING -> "Speaking..."
                                    VoiceState.PROCESSING -> "Thinking..."
                                    VoiceState.ERROR -> "Voice error · Tap to retry"
                                    VoiceState.IDLE -> "Ready · $activeModelName"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (state) {
                                    VoiceState.LISTENING, VoiceState.SPEAKING -> NeonCyan
                                    VoiceState.PROCESSING -> ElectricViolet
                                    VoiceState.ERROR -> RubyError
                                    VoiceState.IDLE -> TextDim
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LensClock(size = 30.dp)
                    Text(
                        text = "KIARAA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickChips) { chipText ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                        .clickable {
                            val prompt = chipText.substringAfter(": ").ifEmpty { chipText }
                            viewModel.sendMessage(prompt)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = chipText,
                        fontSize = 11.sp,
                        color = CoolWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Conversation Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onCopy = { viewModel.triggerCapability("clipboard_copy", msg.content) },
                    onSpeak = { viewModel.voiceManager.speak(msg.content) }
                )
            }

            if (isGenerating) {
                item {
                    GeneratingIndicator()
                }
            }
        }

        // Bottom Input Row
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = SurfaceElevated.copy(alpha = 0.9f),
            borderColor = if (voiceState == VoiceState.LISTENING) NeonCyan else GlassBorderCyan
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    placeholder = {
                        Text(
                            text = if (voiceState == VoiceState.LISTENING) "Listening..." else "Ask KIARAA in Hinglish or English...",
                            color = TextDim,
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = CoolWhite,
                        unfocusedTextColor = CoolWhite
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.sendMessage()
                    })
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Microphone Button (with permission check)
                IconButton(
                    onClick = {
                        if (hasAudioPermission) {
                            if (voiceState == VoiceState.LISTENING) {
                                viewModel.voiceManager.stopListening()
                            } else {
                                viewModel.voiceManager.startListening()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (voiceState == VoiceState.LISTENING) NeonCyan.copy(alpha = 0.25f)
                            else SurfaceDark
                        )
                        .border(
                            width = 1.dp,
                            color = if (voiceState == VoiceState.LISTENING) NeonCyan else GlassBorderSubtle,
                            shape = CircleShape
                        )
                        .testTag("mic_toggle_button")
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Voice Input",
                        tint = if (voiceState == VoiceState.LISTENING) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) NeonCyan else SurfaceDark
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank()) ObsidianDark else TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) Brush.horizontalGradient(
                        listOf(Color(0xFF0F314C), Color(0xFF13456B))
                    ) else Brush.horizontalGradient(listOf(SurfaceGlass, SurfaceGlass))
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) GlassBorderCyan else GlassBorderSubtle,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KIARAA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                        }

                        Row {
                            IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak Aloud",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolWhite,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
fun GeneratingIndicator() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceGlass)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "KIARAA is thinking…",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
        }
    }
}
