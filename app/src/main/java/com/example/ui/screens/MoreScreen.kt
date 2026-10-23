package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.CoolWhite
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextMuted

private data class MoreDestination(
    val title: String,
    val description: String,
    val screen: AppScreen,
    val icon: ImageVector
)

@Composable
fun MoreScreen(onSelectScreen: (AppScreen) -> Unit) {
    val destinations = listOf(
        MoreDestination(
            title = "Memory Vault",
            description = "Saved preferences, projects, and decisions on this device.",
            screen = AppScreen.MEMORY,
            icon = Icons.Default.Storage
        ),
        MoreDestination(
            title = "Tools & Device Actions",
            description = "Inspect capabilities, permissions, and the action audit log.",
            screen = AppScreen.CAPABILITIES,
            icon = Icons.Default.Security
        ),
        MoreDestination(
            title = "Personalization & Privacy",
            description = "Orb appearance, AI provider status, and optional device access.",
            screen = AppScreen.SETTINGS,
            icon = Icons.Default.Settings
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your KIARAA",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )
        Text(
            text = "One clean workspace for memory, tools, and settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))

        destinations.forEach { destination ->
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectScreen(destination.screen) },
                backgroundColor = SurfaceGlass,
                borderColor = if (destination.screen == AppScreen.CAPABILITIES) GlassBorderCyan else GlassBorderSubtle,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CoolWhite
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = destination.description,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
