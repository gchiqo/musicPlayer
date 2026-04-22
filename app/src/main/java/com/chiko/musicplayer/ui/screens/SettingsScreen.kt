package com.chiko.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

private val AccentOptions = listOf(
    0xFFEDEDED, // default white
    0xFFB388FF, // violet
    0xFFFF6FD8, // pink
    0xFF6FE9FF, // cyan
    0xFF4CCF6A, // green
    0xFFFFBD33, // amber
    0xFFFF5A5A, // red
    0xFF9B59B6, // purple
    0xFF3BA7FF, // blue
)

private val BackgroundOptions = listOf(
    0xFF000000, // black (default)
    0xFF0A0A14, // near-black blue
    0xFF101018, // deep charcoal
    0xFF1A1022, // deep violet
    0xFF0F1A14, // deep forest
    0xFF1A120A, // deep espresso
    0xFF0C1418, // deep teal
    0xFF1A0A14, // deep wine
    0xFF14141E, // slate
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    accentArgb: Int,
    backgroundArgb: Int,
    dynamicFromArt: Boolean,
    onAccentChange: (Int) -> Unit,
    onBackgroundChange: (Int) -> Unit,
    onDynamicFromArtChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onClearYoutubeHistory: () -> Unit,
    onClearLibraryHistory: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic from album art",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Override accent + background with colors picked from the current song's cover.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(12.dp))
                Switch(
                    checked = dynamicFromArt,
                    onCheckedChange = onDynamicFromArtChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionHeader("Accent color")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AccentOptions.forEach { argb ->
                    val color = Color(argb.toInt())
                    val selected = color.toArgb() == accentArgb
                    ColorChip(
                        color = color,
                        selected = selected,
                        onClick = { onAccentChange(color.toArgb()) },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionHeader("Background color")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BackgroundOptions.forEach { argb ->
                    val color = Color(argb.toInt())
                    val selected = color.toArgb() == backgroundArgb
                    ColorChip(
                        color = color,
                        selected = selected,
                        onClick = { onBackgroundChange(color.toArgb()) },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionHeader("Search history")
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onClearYoutubeHistory,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear YouTube history")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClearLibraryHistory,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear library history")
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = Color.Black,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
