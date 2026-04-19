package com.chiko.musicplayer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ResonanceColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.Black,
    primaryContainer = SurfaceCardElevated,
    onPrimaryContainer = SoftWhite,
    secondary = NeonPink,
    onSecondary = Color.Black,
    tertiary = NeonCyan,
    onTertiary = Color.Black,
    background = MidnightBlue,
    onBackground = SoftWhite,
    surface = Charcoal,
    onSurface = SoftWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = MutedLavender,
    outline = MutedLavender,
)

val AppGradient = Brush.verticalGradient(
    colors = listOf(MidnightBlue, DeepPurple, MidnightBlue),
)

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
