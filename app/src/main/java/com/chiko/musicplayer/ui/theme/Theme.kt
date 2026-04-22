package com.chiko.musicplayer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MusicPlayerTheme(
    accent: Color = NeonViolet,
    background: Color = Color.Black,
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

    val tertiary = complementaryColor(accent)

    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        primaryContainer = SurfaceCardElevated,
        onPrimaryContainer = SoftWhite,
        secondary = accent,
        onSecondary = Color.Black,
        tertiary = tertiary,
        onTertiary = Color.Black,
        background = background,
        onBackground = SoftWhite,
        surface = Charcoal,
        onSurface = SoftWhite,
        surfaceVariant = SurfaceCard,
        onSurfaceVariant = MutedLavender,
        outline = MutedLavender,
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content,
    )
}

private fun complementaryColor(accent: Color): Color {
    val r = accent.red
    val g = accent.green
    val b = accent.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (delta < 0.02f) return NeonCyan
    val h = when (max) {
        r -> ((g - b) / delta) % 6f
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    }
    val hueDeg = ((h * 60f) + 360f) % 360f
    val shifted = (hueDeg + 140f) % 360f
    val l = (max + min) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))
    return hslToColor(shifted, s.coerceIn(0f, 1f), l.coerceIn(0.35f, 0.75f))
}

private fun hslToColor(hue: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = hue / 60f
    val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r1, g1, b1) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(r1 + m, g1 + m, b1 + m, 1f)
}
