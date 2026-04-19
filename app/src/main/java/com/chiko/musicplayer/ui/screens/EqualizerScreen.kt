package com.chiko.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chiko.musicplayer.audio.EqualizerManager
import com.chiko.musicplayer.ui.theme.NeonViolet

@Composable
fun EqualizerScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAvailable by EqualizerManager.isAvailable.collectAsState()
    val enabled by EqualizerManager.enabled.collectAsState()
    val bands by EqualizerManager.bands.collectAsState()
    val levels by EqualizerManager.bandLevels.collectAsState()
    val minLevel by EqualizerManager.minLevel.collectAsState()
    val maxLevel by EqualizerManager.maxLevel.collectAsState()
    val presets by EqualizerManager.presets.collectAsState()
    val currentPreset by EqualizerManager.currentPreset.collectAsState()
    val bassStrength by EqualizerManager.bassStrength.collectAsState()

    val gradient = Brush.verticalGradient(
        listOf(NeonViolet.copy(alpha = 0.2f), Color.Black, Color.Black)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            TopBar(enabled = enabled, isAvailable = isAvailable, onClose = onClose)

            if (!isAvailable) {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = "Equalizer is not available on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("Preset")
            Spacer(Modifier.height(8.dp))
            PresetRow(
                presets = presets,
                currentPreset = currentPreset,
                enabled = enabled,
                onSelect = { EqualizerManager.applyPreset(it) },
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Bands")
            Spacer(Modifier.height(8.dp))
            bands.forEachIndexed { i, band ->
                val level = levels.getOrNull(i) ?: 0.toShort()
                BandRow(
                    label = band.centerHz.formatHz(),
                    level = level,
                    minLevel = minLevel,
                    maxLevel = maxLevel,
                    enabled = enabled,
                    onChange = { EqualizerManager.setBandLevel(i, it) },
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Bass boost")
            Spacer(Modifier.height(8.dp))
            BassBoostRow(
                strength = bassStrength,
                enabled = enabled,
                onChange = { EqualizerManager.setBassStrength(it) },
            )

            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { EqualizerManager.resetFlat() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.5f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Reset to flat",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun TopBar(
    enabled: Boolean,
    isAvailable: Boolean,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Equalizer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (isAvailable) {
            Switch(
                checked = enabled,
                onCheckedChange = { EqualizerManager.setEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = NeonViolet,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PresetRow(
    presets: List<String>,
    currentPreset: Short,
    enabled: Boolean,
    onSelect: (Short) -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = currentPreset == EqualizerManager.CUSTOM_PRESET,
            onClick = { },
            enabled = false,
            label = { Text("Custom") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                selectedLabelColor = NeonViolet,
                disabledSelectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        presets.forEachIndexed { index, name ->
            val idx = index.toShort()
            FilterChip(
                selected = currentPreset == idx,
                onClick = { if (enabled) onSelect(idx) },
                enabled = enabled,
                label = { Text(name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                    selectedLabelColor = NeonViolet,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun BandRow(
    label: String,
    level: Short,
    minLevel: Short,
    maxLevel: Short,
    enabled: Boolean,
    onChange: (Short) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Slider(
            value = level.toFloat().coerceIn(minLevel.toFloat(), maxLevel.toFloat()),
            onValueChange = { if (enabled) onChange(it.toInt().toShort()) },
            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NeonViolet,
                activeTrackColor = NeonViolet,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = level.formatDb(),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
    }
}

@Composable
private fun BassBoostRow(
    strength: Short,
    enabled: Boolean,
    onChange: (Short) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Bass",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Slider(
            value = strength.toFloat().coerceIn(0f, 1000f),
            onValueChange = { if (enabled) onChange(it.toInt().toShort()) },
            valueRange = 0f..1000f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(strength.toInt() / 10).coerceIn(0, 100)}%",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
    }
}

private fun Int.formatHz(): String {
    if (this < 1000) return "$this Hz"
    val k = this / 1000f
    return if (k == k.toInt().toFloat()) "${k.toInt()} kHz" else "%.1f kHz".format(k)
}

private fun Short.formatDb(): String = "%+.1f dB".format(this / 100f)
