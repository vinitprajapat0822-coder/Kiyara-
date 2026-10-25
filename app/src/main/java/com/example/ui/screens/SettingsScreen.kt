package com.example.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.operating.KiaraaAccessibilityService
import com.example.ui.MainViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.componentryGrainGradient
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.HighThinkingAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.voice.VoiceState

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orbPulsation by viewModel.orbPulsationRate.collectAsState()
    val activeModel by viewModel.activeModelName.collectAsState()
    val voiceProviderStatus by viewModel.voiceManager.voiceProviderStatus.collectAsState()
    val voiceError by viewModel.voiceManager.errorMessage.collectAsState()
    var privacyAction by remember { mutableStateOf("") }
    var accessRefresh by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) accessRefresh += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val accessibilityEnabled = remember(accessRefresh) {
        val master = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val component = ComponentName(context, KiaraaAccessibilityService::class.java).flattenToString()
        master && enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }
    val notificationEnabled = remember(accessRefresh) {
        com.example.operating.KiaraaNotificationListenerService.hasAccess(context)
    }
    val writeEnabled = remember(accessRefresh) { Settings.System.canWrite(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().componentryGrainGradient().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LiquidGlassCard(backgroundColor = SurfaceElevated.copy(alpha = 0.92f), borderColor = GlassBorderCyan) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("KIARAA AI", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    AnimatedOrb(voiceState = VoiceState.IDLE, size = 88.dp, pulsationRate = orbPulsation)
                    Text("Voice-first personal assistant", color = TextMuted, fontSize = 12.sp)
                }
            }
        }

        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Appearance", color = CoolWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))))
                        Text("Midnight Liquid Glass", color = CoolWhite, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Orb motion", color = TextMuted, fontSize = 11.sp)
                        Text(String.format("%.1fx", orbPulsation), color = CyanGlow, fontSize = 11.sp)
                    }
                    Slider(
                        value = orbPulsation,
                        onValueChange = { viewModel.setOrbPulsationRate(it) },
                        valueRange = 0.5f..2f,
                        modifier = Modifier.semantics {
                            contentDescription = "Orb motion speed"
                            stateDescription = String.format("%.1f times normal", orbPulsation)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SurfaceElevated
                        )
                    )
                }
            }
        }

        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass, borderColor = GlassBorderCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = HighThinkingAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("AI connection", color = CoolWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        activeModel.ifBlank { "gemini-3.5-flash-lite" },
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(voiceProviderStatus, color = CyanGlow, fontSize = 11.sp)
                    voiceError?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = AmberWarning, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Device access", color = CoolWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    StatusLine("Accessibility", accessibilityEnabled)
                    AccessButton("Accessibility", Icons.Default.Accessibility) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    StatusLine("Notifications", notificationEnabled)
                    AccessButton("Notification access", Icons.Default.NotificationsActive) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    StatusLine("System settings", writeEnabled)
                    AccessButton("Brightness / timeout", Icons.Default.Tune) {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                        )
                    }
                }
            }
        }

        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacy", color = CoolWhite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { privacyAction = "chat" },
                        modifier = Modifier.fillMaxWidth().height(42.dp).semantics { contentDescription = "Clear chat history" },
                        colors = ButtonDefaults.buttonColors(containerColor = RubyError.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RubyError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Clear chat", color = CoolWhite, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { privacyAction = "memory" },
                        modifier = Modifier.fillMaxWidth().height(42.dp).semantics { contentDescription = "Clear Memory Vault" },
                        colors = ButtonDefaults.buttonColors(containerColor = RubyError.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RubyError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Clear memory", color = CoolWhite, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("KIARAA", modifier = Modifier.fillMaxWidth().padding(4.dp), color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }

    if (privacyAction.isNotBlank()) {
        val isChat = privacyAction == "chat"
        AlertDialog(
            onDismissRequest = { privacyAction = "" },
            title = { Text(if (isChat) "Clear chat?" else "Clear memory?", color = CoolWhite) },
            text = { Text("This cannot be undone.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    if (isChat) viewModel.clearChatHistory() else viewModel.clearAllMemories()
                    privacyAction = ""
                }) { Text("Clear", color = RubyError) }
            },
            dismissButton = { TextButton(onClick = { privacyAction = "" }) { Text("Cancel", color = TextMuted) } },
            containerColor = SurfaceElevated
        )
    }
}

@Composable
private fun StatusLine(label: String, enabled: Boolean) {
    Text(
        "$label: " + if (enabled) "On" else "Off",
        color = if (enabled) CyanGlow else AmberWarning,
        fontSize = 11.sp
    )
}

@Composable
private fun AccessButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp).semantics { contentDescription = label },
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
    ) {
        Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, color = CoolWhite, fontSize = 12.sp)
    }
}
