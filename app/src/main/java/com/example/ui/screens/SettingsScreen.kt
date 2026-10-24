package com.example.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.MainViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.liquidGlassBackground
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.HighThinkingAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.voice.VoiceState
import com.example.operating.KiaraaAccessibilityService

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
    val notificationEnabled = remember(accessRefresh) { com.example.operating.KiaraaNotificationListenerService.hasAccess(context) }
    val writeEnabled = remember(accessRefresh) { Settings.System.canWrite(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().liquidGlassBackground().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(backgroundColor = SurfaceElevated.copy(alpha = 0.92f), borderColor = GlassBorderCyan) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("KIARAA AI", color = NeonCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Personal AI • Voice + Chat", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    AnimatedOrb(voiceState = VoiceState.IDLE, size = 104.dp, pulsationRate = orbPulsation)
                    Text("Your voice-first personal assistant", color = CoolWhite, fontSize = 13.sp)
                    Text("Home • Voice • Memory • Settings", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.size(8.dp))
                        Text("Appearance", color = CoolWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(Brush.linearGradient(listOf(NeonCyan, ElectricViolet))))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Liquid Glass 27 · Midnight", color = CoolWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("One unified frosted-glass surface with cyan/violet highlights and a darker edge.", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Orb motion", color = CoolWhite, fontSize = 12.sp)
                        Text(String.format("%.1fx", orbPulsation), color = CyanGlow, fontSize = 12.sp)
                    }
                    Slider(orbPulsation, { viewModel.setOrbPulsationRate(it) }, valueRange = 0.5f..2f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = SurfaceElevated))
                }
            }
        }
        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass, borderColor = GlassBorderCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = HighThinkingAccent)
                        Spacer(Modifier.size(8.dp))
                        Text("AI connection", color = CoolWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Active model: " + activeModel.ifBlank { "gemini-3.5-flash-lite" }, color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Gemini 3.5 Flash-Lite uses a protected request header, one transient-network retry, and explicit API error messages.", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Voice output", color = CoolWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        voiceProviderStatus,
                        color = when {
                            voiceProviderStatus.contains("unavailable", ignoreCase = true) || voiceProviderStatus.contains("failed", ignoreCase = true) -> RubyError
                            voiceProviderStatus.contains("ElevenLabs", ignoreCase = true) -> CyanGlow
                            else -> TextMuted
                        },
                        fontSize = 12.sp
                    )
                    voiceError?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = AmberWarning, fontSize = 11.sp)
                    }
                    Text("Release security: the prototype currently calls Gemini from the Android client. Before public release, move the API key behind an authenticated backend proxy; a key packaged in an APK can be extracted.", color = AmberWarning, fontSize = 11.sp)
                }
            }
        }
        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Device access", color = CoolWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("All special access is opt-in. Android settings open only after you request a feature; KIARAA never enables them silently.", color = TextMuted, fontSize = 11.sp)
                    Text("Accessibility: " + if (accessibilityEnabled) "Enabled" else "Not enabled", color = if (accessibilityEnabled) CyanGlow else AmberWarning, fontSize = 12.sp)
                    AccessButton("Enable / manage Accessibility", Icons.Default.Accessibility) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    Text("Notification access: " + if (notificationEnabled) "Enabled" else "Not enabled", color = if (notificationEnabled) CyanGlow else AmberWarning, fontSize = 12.sp)
                    AccessButton("Enable / manage Notification Access", Icons.Default.NotificationsActive) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    Text("Modify system settings: " + if (writeEnabled) "Enabled" else "Not enabled", color = if (writeEnabled) CyanGlow else AmberWarning, fontSize = 12.sp)
                    AccessButton("Allow brightness / timeout changes", Icons.Default.Tune) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }
                    Text("After returning, tap a status/control again to refresh. Android restricts direct Wi-Fi, mobile-data, airplane-mode and some DND switches, so KIARAA opens the settings page rather than claiming a successful toggle. Media search opens results but does not auto-select a video/song.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
        item {
            LiquidGlassCard(backgroundColor = SurfaceGlass, borderColor = GlassBorderSubtle) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanGlow)
                        Spacer(Modifier.size(8.dp))
                        Text("Privacy & local data", color = CoolWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Memory is stored locally. Only save details you choose. Passwords, OTPs, PINs and credentials must never be stored.", color = TextMuted, fontSize = 11.sp)
                    Button(onClick = { privacyAction = "chat" }, modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RubyError.copy(alpha = 0.16f))) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RubyError)
                        Spacer(Modifier.size(8.dp))
                        Text("Clear chat history", color = CoolWhite)
                    }
                    Button(onClick = { privacyAction = "memory" }, modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RubyError.copy(alpha = 0.16f))) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RubyError)
                        Spacer(Modifier.size(8.dp))
                        Text("Clear Memory Vault", color = CoolWhite)
                    }
                }
            }
        }
        item {
            Text("KIARAA AI • Personal Assistant", modifier = Modifier.fillMaxWidth().padding(8.dp), color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }

    if (privacyAction.isNotBlank()) {
        val isChat = privacyAction == "chat"
        AlertDialog(
            onDismissRequest = { privacyAction = "" },
            title = { Text(if (isChat) "Clear chat history?" else "Clear Memory Vault?", color = CoolWhite) },
            text = { Text("This permanently removes the selected local data. This cannot be undone.", color = TextMuted) },
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
private fun AccessButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) {
        Icon(icon, contentDescription = null, tint = NeonCyan)
        Spacer(Modifier.size(8.dp))
        Text(label, color = CoolWhite, fontSize = 12.sp)
    }
}
