package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.rememberReducedMotion
import com.example.ui.components.liquidGlassBackground
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletGlow
import com.example.voice.VoiceState

@Composable
fun LiveBaatScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val partialText by viewModel.voiceManager.partialText.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()
    val orbPulsation by viewModel.orbPulsationRate.collectAsState()
    val reduceMotion = rememberReducedMotion()

    var isLiveBaatMode by remember { mutableStateOf(true) }

    LaunchedEffect(isLiveBaatMode) {
        viewModel.voiceManager.setContinuousListeningEnabled(isLiveBaatMode)
        if (!isLiveBaatMode) viewModel.voiceManager.cancelListening()
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassBackground()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceElevated)
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isLiveBaatMode) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isLiveBaatMode) NeonCyan else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(role = Role.Button) { isLiveBaatMode = true }
                    .semantics {
                        contentDescription = "Live Baat mode"
                        stateDescription = if (isLiveBaatMode) "Selected" else "Not selected"
                        role = Role.Button
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Live Baat",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isLiveBaatMode) NeonCyan else TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isLiveBaatMode) ElectricViolet.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (!isLiveBaatMode) ElectricViolet else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(role = Role.Button) { isLiveBaatMode = false }
                    .semantics {
                        contentDescription = "Saathi voice chat mode"
                        stateDescription = if (!isLiveBaatMode) "Selected" else "Not selected"
                        role = Role.Button
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Saathi (Voice Chat)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (!isLiveBaatMode) VioletGlow else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedOrb(
                voiceState = voiceState,
                size = 210.dp,
                audioRms = audioRms,
                pulsationRate = orbPulsation,
                onClick = {
                    if (voiceState == VoiceState.SPEAKING) {
                        viewModel.voiceManager.stopSpeaking()
                    } else if (hasAudioPermission) {
                        if (isLiveBaatMode) {
                            if (voiceState == VoiceState.LISTENING) {
                                viewModel.voiceManager.setContinuousListeningEnabled(false)
                                viewModel.voiceManager.cancelListening()
                            } else {
                                viewModel.voiceManager.setContinuousListeningEnabled(true)
                                viewModel.voiceManager.startListening()
                            }
                        } else if (voiceState == VoiceState.LISTENING) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            viewModel.voiceManager.startListening()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when (voiceState) {
                            VoiceState.LISTENING -> NeonCyan.copy(alpha = 0.15f)
                            VoiceState.SPEAKING -> ElectricViolet.copy(alpha = 0.15f)
                            VoiceState.PROCESSING -> VioletGlow.copy(alpha = 0.15f)
                            VoiceState.ERROR -> RubyError.copy(alpha = 0.15f)
                            VoiceState.IDLE -> SurfaceElevated
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when (voiceState) {
                            VoiceState.LISTENING -> NeonCyan
                            VoiceState.SPEAKING -> ElectricViolet
                            VoiceState.PROCESSING -> VioletGlow
                            VoiceState.ERROR -> RubyError
                            VoiceState.IDLE -> GlassBorderSubtle
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .semantics {
                        contentDescription = "Voice status"
                        stateDescription = when (voiceState) {
                            VoiceState.LISTENING -> "Listening"
                            VoiceState.SPEAKING -> "Speaking"
                            VoiceState.PROCESSING -> "Processing"
                            VoiceState.ERROR -> "Audio error"
                            VoiceState.IDLE -> "Idle"
                        }
                        liveRegion = LiveRegionMode.Polite
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (voiceState) {
                                    VoiceState.LISTENING -> NeonCyan
                                    VoiceState.SPEAKING -> ElectricViolet
                                    VoiceState.PROCESSING -> VioletGlow
                                    VoiceState.ERROR -> RubyError
                                    VoiceState.IDLE -> TextDim
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (voiceState) {
                            VoiceState.LISTENING -> "LIVE LISTENING • BOLIE"
                            VoiceState.SPEAKING -> "KIARAA IS SPEAKING"
                            VoiceState.PROCESSING -> "PROCESSING SPEECH"
                            VoiceState.ERROR -> "AUDIO ERROR"
                            VoiceState.IDLE -> "TAP MIC TO SPEAK"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoolWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AudioWaveformBar(
                voiceState = voiceState,
                audioRms = audioRms,
                barCount = 24,
                activeColor = if (isLiveBaatMode) NeonCyan else VioletGlow
            )

            Spacer(modifier = Modifier.height(16.dp))

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .semantics {
                        contentDescription = when {
                            partialText.isNotBlank() -> "Partial transcript: $partialText"
                            recognizedText.isNotBlank() -> "Recognized speech: $recognizedText"
                            voiceState == VoiceState.SPEAKING -> "KIARAA is speaking"
                            else -> "Voice transcript area"
                        }
                        liveRegion = LiveRegionMode.Polite
                    },
                backgroundColor = SurfaceGlass.copy(alpha = 0.6f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (partialText.isNotBlank()) "\"$partialText...\""
                        else if (recognizedText.isNotBlank()) "\"$recognizedText\""
                        else if (voiceState == VoiceState.SPEAKING) "KIARAA vocalizing response..."
                        else if (isLiveBaatMode) "Live Baat will listen again after each response while this screen stays open."
                        else "Saathi listens once per turn. Ask about coding, daily tasks, or phone controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (partialText.isNotBlank() || recognizedText.isNotBlank()) NeonCyan else TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = voiceState == VoiceState.SPEAKING,
                enter = if (reduceMotion) EnterTransition.None else fadeIn(),
                exit = if (reduceMotion) ExitTransition.None else fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF3B1D28))
                        .border(1.dp, RubyError.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button) { viewModel.voiceManager.stopSpeaking() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("barge_in_button")
                        .semantics { contentDescription = "Interrupt and stop KIARAA speaking" }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Stop",
                            tint = RubyError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Interrupt / Stop KIARAA",
                            color = CoolWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            IconButton(
                onClick = {
                    if (hasAudioPermission) {
                        if (isLiveBaatMode && voiceState == VoiceState.LISTENING) {
                            viewModel.voiceManager.setContinuousListeningEnabled(false)
                            viewModel.voiceManager.cancelListening()
                        } else if (isLiveBaatMode) {
                            viewModel.voiceManager.setContinuousListeningEnabled(true)
                            viewModel.voiceManager.startListening()
                        } else if (voiceState == VoiceState.LISTENING) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            viewModel.voiceManager.startListening()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (voiceState == VoiceState.LISTENING) Brush.radialGradient(
                            listOf(NeonCyan, Color(0xFF0284C7))
                        ) else Brush.radialGradient(
                            listOf(SurfaceElevated, SurfaceDark)
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = if (voiceState == VoiceState.LISTENING) NeonCyan else GlassBorderCyan,
                        shape = CircleShape
                    )
                    .testTag("live_baat_mic_button")
                    .semantics {
                        contentDescription = when {
                            !hasAudioPermission -> "Grant microphone permission"
                            voiceState == VoiceState.LISTENING -> "Stop voice listening"
                            else -> "Start voice listening"
                        }
                        stateDescription = when (voiceState) {
                            VoiceState.LISTENING -> "Listening"
                            VoiceState.SPEAKING -> "Speaking"
                            VoiceState.PROCESSING -> "Processing"
                            VoiceState.ERROR -> "Voice error"
                            VoiceState.IDLE -> "Idle"
                        }
                    }
            ) {
                Icon(
                    imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Mic else Icons.Default.Hearing,
                    contentDescription = null,
                    tint = if (voiceState == VoiceState.LISTENING) ObsidianDark else NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (!hasAudioPermission) "Microphone permission required. Tap to grant."
                else if (voiceState == VoiceState.LISTENING) "Tap to pause speech"
                else "Tap to activate voice",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextDim
            )
        }
    }
}
