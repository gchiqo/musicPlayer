package com.chiko.musicplayer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ResonanceColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.Black,
    primaryContainer = SurfaceCardElevated,
    onPrimaryContainer = SoftWhite,
    secondary = NeonViolet,
    onSecondary = Color.Black,
    tertiary = NeonCyan,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = SoftWhite,
    surface = Charcoal,
    onSurface = SoftWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = MutedLavender,
    outline = MutedLavender,
)

val AppGradient: Brush = SolidColor(Color.Black)

@Composable
fun MusicPlayerTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = ResonanceColorScheme,
        typography = Typography,
        content = content
    )
}
