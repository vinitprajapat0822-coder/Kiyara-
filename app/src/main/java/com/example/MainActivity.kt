package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.operating.RiskLevel
import com.example.ui.components.ComponentryDockHighlight
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.LiveBaatScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.KiaraaTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KiaraaTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val pendingDeviceConfirmation by viewModel.pendingDeviceConfirmation.collectAsState()
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    // Handle Android system back button properly: return to Terminal screen
    BackHandler(enabled = currentScreen != AppScreen.TERMINAL) {
        viewModel.setScreen(AppScreen.TERMINAL)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark),
        bottomBar = {
            // Keep the chat input unobstructed: hide the dock while the IME is visible.
            if (!isKeyboardVisible) {
                KiaraaNavigationBar(
                    currentScreen = currentScreen,
                    onSelectScreen = { viewModel.setScreen(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianDark)
        ) {
            // Main Screen Switcher
            when (currentScreen) {
                AppScreen.TERMINAL -> ChatScreen(viewModel = viewModel)
                AppScreen.LIVE_BAAT -> LiveBaatScreen(viewModel = viewModel)
                AppScreen.MEMORY -> MemoryScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Top Status / Notification Banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                statusBanner?.let { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, GlassBorderCyan, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CoolWhite
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Screen taps/text entry are blocked; opening an external notification requires explicit confirmation.
            pendingDeviceConfirmation?.let { pending ->
                AlertDialog(
                    onDismissRequest = { viewModel.cancelPendingDeviceAction() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Confirm device action",
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pending.title,
                                color = CoolWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    },
                    text = {
                        Text(
                            text = pending.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmPendingDeviceAction() },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                        ) {
                            Text("Confirm action", color = CoolWhite, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelPendingDeviceAction() }) {
                            Text("Cancel", color = TextMuted)
                        }
                    },
                    containerColor = SurfaceElevated
                )
            }

            // Explicit Risk Confirmation Dialog (Section 12 Safeguards)
            pendingConfirmation?.let { (capability, param) ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissConfirmation() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Risk Check",
                                tint = if (capability.riskLevel == RiskLevel.HIGH) RubyError else AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Action Authorization",
                                color = CoolWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Tool: ${capability.name}",
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Risk Level: ${capability.riskLevel.name}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (capability.riskLevel == RiskLevel.HIGH) RubyError else AmberWarning
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Target: $param\n\nExplicit confirmation is mandatory under KIARAA AI Operating Policy before executing higher-risk or external operations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmPendingAction() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (capability.riskLevel == RiskLevel.HIGH) RubyError else AmberWarning
                            )
                        ) {
                            Text("Confirm & Execute", color = CoolWhite, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissConfirmation() }) {
                            Text("Decline", color = TextMuted)
                        }
                    },
                    containerColor = SurfaceElevated
                )
            }
        }
    }
}

@Composable
fun KiaraaNavigationBar(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceGlass.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .shadow(14.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(28.dp))
    ) {
        val navItems = listOf(
            Triple(AppScreen.TERMINAL, "Home", Icons.Default.Home),
            Triple(AppScreen.LIVE_BAAT, "Voice", Icons.Default.Hearing),
            Triple(AppScreen.MEMORY, "Memory", Icons.Default.Storage),
            Triple(AppScreen.SETTINGS, "Settings", Icons.Default.Settings)
        )

        navItems.forEach { (screen, label, icon) ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(screen) },
                icon = {
                    ComponentryDockHighlight(selected = isSelected) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    unselectedIconColor = TextDim,
                    unselectedTextColor = TextDim,
                    indicatorColor = NeonCyan.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
            )
        }
    }
}
