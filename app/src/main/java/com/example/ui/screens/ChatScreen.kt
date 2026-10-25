package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.ui.components.rememberReducedMotion
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.AnnotatedAssistantText
import com.example.ui.components.ComponentryTextMorph
import com.example.ui.components.ComponentryDitherGradient
import com.example.ui.components.ComponentrySpectralRibbon
import com.example.ui.components.componentryGrainGradient
import com.example.ui.components.ComponentryWelcomeCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LensClock
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
    val reduceMotion = rememberReducedMotion()

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

    LaunchedEffect(messages.size, reduceMotion) {
        if (messages.isNotEmpty()) {
            if (reduceMotion) listState.scrollToItem(messages.size - 1)
            else listState.animateScrollToItem(messages.size - 1)
        }
    }

    val slashCommands = listOf("/voice", "/memory", "/settings", "/stop", "/help")

    fun executeSlashCommand(command: String) {
        when (command) {
            "/voice" -> {
                viewModel.updateInputText("")
                viewModel.setScreen(AppScreen.LIVE_BAAT)
            }
            "/memory" -> {
                viewModel.updateInputText("")
                viewModel.setScreen(AppScreen.MEMORY)
            }
            "/settings" -> {
                viewModel.updateInputText("")
                viewModel.setScreen(AppScreen.SETTINGS)
            }
            "/stop" -> {
                viewModel.voiceManager.stopSpeaking()
                viewModel.voiceManager.stopListening()
                viewModel.updateInputText("")
            }
            "/help" -> {
                viewModel.updateInputText("")
                viewModel.sendMessage("Explain KIARAA's chat, voice, memory and phone-assist features.")
            }
            else -> viewModel.sendMessage()
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
            .componentryGrainGradient()
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
                        ComponentryTextMorph(
                            status = when {
                                isGenerating || voiceState == VoiceState.PROCESSING -> "THINKING"
                                voiceState == VoiceState.LISTENING -> "LISTENING"
                                voiceState == VoiceState.SPEAKING -> "SPEAKING"
                                voiceState == VoiceState.ERROR -> "VOICE ERROR"
                                else -> "READY"
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "KIARAA assistant status: " + when {
                                    isGenerating || voiceState == VoiceState.PROCESSING -> "Thinking"
                                    voiceState == VoiceState.LISTENING -> "Listening"
                                    voiceState == VoiceState.SPEAKING -> "Speaking"
                                    voiceState == VoiceState.ERROR -> "Voice error"
                                    else -> "Ready"
                                }
                                liveRegion = LiveRegionMode.Polite
                            }
                        )
                        Text(
                            text = activeModelName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim,
                            maxLines = 1
                        )
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

        // Low-cost spectral ribbon + dither accent; decorative only, no touch interception.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(horizontal = 28.dp)
        ) {
            ComponentryDitherGradient(Modifier.fillMaxSize())
            ComponentrySpectralRibbon(Modifier.fillMaxSize())
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
                        .clickable(role = Role.Button) {
                            val prompt = chipText.substringAfter(": ").ifEmpty { chipText }
                            viewModel.sendMessage(prompt)
                        }
                        .semantics { contentDescription = "Suggestion: $chipText" }
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
            if (messages.isEmpty()) {
                item(key = "kiaraa_welcome") {
                    ComponentryWelcomeCard(
                        status = when {
                            isGenerating || voiceState == VoiceState.PROCESSING -> "THINKING"
                            voiceState == VoiceState.LISTENING -> "LISTENING"
                            voiceState == VoiceState.SPEAKING -> "SPEAKING"
                            voiceState == VoiceState.ERROR -> "VOICE ERROR"
                            else -> "READY"
                        }
                    )
                }
            }
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

        // Slash-command palette: actions stay local and do not send a command as chat text.
        AnimatedVisibility(
            visible = inputText.trimStart().startsWith("/"),
            enter = if (reduceMotion) EnterTransition.None else fadeIn(),
            exit = if (reduceMotion) ExitTransition.None else fadeOut()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .semantics { contentDescription = "Slash command suggestions" },
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(slashCommands.filter { command ->
                    val query = inputText.trim().lowercase()
                    query == "/" || command.startsWith(query)
                }) { command ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, GlassBorderCyan, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { executeSlashCommand(command) }
                            .semantics { contentDescription = "Run $command command" }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Text(command, color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Bottom Input Row
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .then(if (reduceMotion) Modifier else Modifier.animateContentSize(animationSpec = tween(180))),
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
                        .testTag("chat_input_field")
                        .semantics {
                            contentDescription = "Message input for KIARAA"
                            stateDescription = if (inputText.isBlank()) "Empty" else "${inputText.length} characters entered"
                        },
                    label = { Text("Message to KIARAA") },
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
                        executeSlashCommand(inputText.trim().lowercase())
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
                        .semantics {
                            contentDescription = if (voiceState == VoiceState.LISTENING) "Stop voice input" else "Start voice input"
                            stateDescription = if (voiceState == VoiceState.LISTENING) "Listening" else "Not listening"
                        }
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (voiceState == VoiceState.LISTENING) "Stop voice input" else "Start voice input",
                        tint = if (voiceState == VoiceState.LISTENING) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                IconButton(
                    onClick = { executeSlashCommand(inputText.trim().lowercase()) },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) NeonCyan else SurfaceDark
                        )
                        .testTag("send_button")
                        .semantics {
                            contentDescription = if (inputText.trimStart().startsWith("/")) {
                                "Run KIARAA slash command"
                            } else if (inputText.isNotBlank() && !isGenerating) {
                                "Send message to KIARAA"
                            } else {
                                "Send message unavailable"
                            }
                            stateDescription = if (isGenerating) "KIARAA is responding" else "Ready"
                        }
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

                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CoolWhite,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                } else {
                    AnnotatedAssistantText(
                        text = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
            if (rememberReducedMotion()) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = NeonCyan
                )
            }
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
