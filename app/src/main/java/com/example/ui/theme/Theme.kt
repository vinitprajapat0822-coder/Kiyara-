package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MidnightColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianDark,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricViolet,
    onSecondary = CoolWhite,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = VioletGlow,
    tertiary = HighThinkingAccent,
    onTertiary = ObsidianDark,
    background = ObsidianDark,
    onBackground = CoolWhite,
    surface = SurfaceDark,
    onSurface = CoolWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = GlassBorderCyan,
    outlineVariant = GlassBorderSubtle,
    error = RubyError,
    onError = CoolWhite
)

@Composable
fun KiaraaTheme(
    darkTheme: Boolean = true, // Default to Midnight Terminal dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = MidnightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianDark.toArgb()
                window.navigationBarColor = ObsidianDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
