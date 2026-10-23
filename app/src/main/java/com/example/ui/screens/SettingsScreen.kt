package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AnimatedOrb
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.OrbVisualTheme
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
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
import com.example.ui.theme.VioletGlow
import com.example.voice.VoiceState

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val orbTheme by viewModel.orbTheme.collectAsState()
    val orbPulsation by viewModel.orbPulsationRate.collectAsState()
    val activeModel by viewModel.activeModelName.collectAsState()
    val includeMemoryInAiContext by viewModel.includeMemoryInAiContext.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Character Studio Header & Orb Preview
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceElevated.copy(alpha = 0.9f),
                borderColor = GlassBorderCyan
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Studio",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHARACTER STUDIO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedOrb(
                        voiceState = voiceState,
                        size = 110.dp,
                        visualTheme = orbTheme,
                        pulsationRate = orbPulsation
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Visual Codename: MR.V — Midnight Terminal",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CoolWhite
                    )
                }
            }
        }

        // Theme Palette Switcher
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceGlass
            ) {
                Column {
                    Text(
                        text = "Orb Visual Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = CoolWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val themes = listOf(
                        Pair(OrbVisualTheme.MIDNIGHT_TERMINAL, "Midnight (Cyan/Violet)"),
                        Pair(OrbVisualTheme.NEON_MATRIX, "Neon Matrix (Emerald)"),
                        Pair(OrbVisualTheme.SOLAR_FLARE, "Solar Flare (Amber/Flame)"),
                        Pair(OrbVisualTheme.QUANTUM_NEBULA, "Quantum Nebula (Magenta)")
                    )

                    for ((themeOption, label) in themes) {
                        val isSelected = orbTheme == themeOption
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else GlassBorderSubtle,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setOrbTheme(themeOption) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) NeonCyan else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pulsation Rate: ${String.format("%.1fx", orbPulsation)}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )

                    Slider(
                        value = orbPulsation,
                        onValueChange = { viewModel.setOrbPulsationRate(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SurfaceElevated
                        )
                    )
                }
            }
        }

        // Avatar Roadmap (Transparency rule from Section 9)
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceGlass
            ) {
                Column {
                    Text(
                        text = "Avatar Roadmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    RoadmapRow(phase = "Phase 1 (Active)", desc = "Animated Liquid Glass Neural Orb with 6 visual states & audio RMS scaling.", isActive = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    RoadmapRow(phase = "Phase 2 (Planned)", desc = "Custom 3D character integration & real audio phoneme lip-sync.", isActive = false)
                    Spacer(modifier = Modifier.height(4.dp))
                    RoadmapRow(phase = "Phase 3 (Planned)", desc = "Personalized user photo/avatar styling with expression morphing.", isActive = false)
                }
            }
        }

        // Active model — no unsupported model selector or fake thinking toggle.
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceGlass,
                borderColor = GlassBorderSubtle
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "AI Provider",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Provider",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active model: $activeModel",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CoolWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "KIARAA uses the configured Gemini API key from AI Studio Secrets. If the request fails, the app reports the provider error and may use its local companion fallback.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Memory Vault in AI replies",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoolWhite
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (includeMemoryInAiContext) {
                                    "On: up to 8 recent saved memories can be sent to Gemini with your prompt."
                                } else {
                                    "Off: saved memories stay on this device and are not added to Gemini prompts."
                                },
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = includeMemoryInAiContext,
                            onCheckedChange = { viewModel.setIncludeMemoryInAiContext(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextDim,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // Optional OS-granted permissions. KIARAA never enables these silently.
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceGlass,
                borderColor = GlassBorderCyan
            ) {
                Column {
                    Text(
                        text = "Device Access • Optional",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Back / Home / Recents navigation and notification reading require separate approval in Android Settings. Navigation does not read screen contents. Notification text is kept in memory and is not automatically sent to Gemini.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.18f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Navigation Setup",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                        }
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet.copy(alpha = 0.18f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Notification Access",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = VioletGlow
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You stay in control: turn either permission off any time in Android Settings.",
                        fontSize = 10.sp,
                        color = TextDim
                    )
                }
            }
        }

        // Reset & Privacy Operations
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF1F1015).copy(alpha = 0.7f),
                borderColor = RubyError.copy(alpha = 0.3f)
            ) {
                Column {
                    Text(
                        text = "Data & Privacy Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = RubyError
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reset on-device conversation memory or purge stored history.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.clearChatHistory() },
                            colors = ButtonDefaults.buttonColors(containerColor = RubyError.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RubyError.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Clear Chat History",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = RubyError
                            )
                        }

                        Button(
                            onClick = { viewModel.clearAllMemories() },
                            colors = ButtonDefaults.buttonColors(containerColor = RubyError.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RubyError.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Purge Memory Vault",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = RubyError
                            )
                        }
                    }
                }
            }
        }

        // Platform & Optimization Info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "KIARAA AI • Codename MR.V (Midnight Terminal)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim
                    )
                    Text(
                        text = "Target: Redmi Note 7S (Android 10, 4GB RAM) • Zero-lag Compose Canvas",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun RoadmapRow(phase: String, desc: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isActive) EmeraldSuccess else TextDim)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = phase,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isActive) EmeraldSuccess else TextDim
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
