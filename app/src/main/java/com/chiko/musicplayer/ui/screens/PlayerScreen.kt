package com.chiko.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.chiko.musicplayer.data.Song
import com.chiko.musicplayer.ui.components.Artwork
import com.chiko.musicplayer.ui.components.formatDuration
import com.chiko.musicplayer.ui.theme.NeonViolet

private val ArtworkCorner = 32.dp

@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = remember(song.id) {
        Brush.verticalGradient(
            listOf(NeonViolet.copy(alpha = 0.35f), Color.Black, Color.Black)
        )
    }
    var sliderInteracting by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    val effectivePosition = if (sliderInteracting) sliderValue.toLong() else positionMs

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp),
        ) {
            val isWide = maxWidth >= 600.dp
            val sliderCallbacks = SliderCallbacks(
                onSeekStart = {
                    sliderInteracting = true
                    sliderValue = positionMs.toFloat()
                },
                onSeekChange = { sliderValue = it },
                onSeekEnd = {
                    onSeek(sliderValue.toLong())
                    sliderInteracting = false
                },
            )
            if (isWide) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(
                        onClose = onClose,
                        onOpenEqualizer = onOpenEqualizer,
                        onOpenVisualizer = onOpenVisualizer,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HeroArtwork(
                            song = song,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 12.dp),
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.weight(1f))
                            ProgressSection(
                                positionMs = effectivePosition,
                                durationMs = durationMs,
                                playbackSpeed = playbackSpeed,
                                onSpeedChange = onSpeedChange,
                                onSeekStart = sliderCallbacks.onSeekStart,
                                onSeekChange = sliderCallbacks.onSeekChange,
                                onSeekEnd = sliderCallbacks.onSeekEnd,
                            )
                            Spacer(Modifier.height(20.dp))
                            Controls(
                                isPlaying = isPlaying,
                                shuffle = shuffle,
                                repeatMode = repeatMode,
                                onPlayPause = onPlayPause,
                                onNext = onNext,
                                onPrevious = onPrevious,
                                onToggleShuffle = onToggleShuffle,
                                onCycleRepeat = onCycleRepeat,
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(
                        onClose = onClose,
                        onOpenEqualizer = onOpenEqualizer,
                        onOpenVisualizer = onOpenVisualizer,
                    )
                    Spacer(Modifier.height(8.dp))
                    HeroArtwork(
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    ProgressSection(
                        positionMs = effectivePosition,
                        durationMs = durationMs,
                        playbackSpeed = playbackSpeed,
                        onSpeedChange = onSpeedChange,
                        onSeekStart = sliderCallbacks.onSeekStart,
                        onSeekChange = sliderCallbacks.onSeekChange,
                        onSeekEnd = sliderCallbacks.onSeekEnd,
                    )
                    Spacer(Modifier.height(20.dp))
                    Controls(
                        isPlaying = isPlaying,
                        shuffle = shuffle,
                        repeatMode = repeatMode,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private data class SliderCallbacks(
    val onSeekStart: () -> Unit,
    val onSeekChange: (Float) -> Unit,
    val onSeekEnd: () -> Unit,
)

@Composable
private fun TopBar(
    onClose: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenVisualizer: () -> Unit,
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
                contentDescription = "Collapse",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenVisualizer) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Visualizer",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = onOpenEqualizer) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Equalizer",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun HeroArtwork(
    song: Song,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(ArtworkCorner + 8.dp))
                .background(
                    Brush.radialGradient(
                        listOf(NeonViolet.copy(alpha = 0.55f), Color.Transparent)
                    )
                ),
        )
        Artwork(
            song = song,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(ArtworkCorner),
                    clip = false,
                )
                .clip(RoundedCornerShape(ArtworkCorner)),
            cornerRadius = ArtworkCorner,
            iconSize = 96.dp,
        )
    }
}

@Composable
private fun ProgressSection(
    positionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: () -> Unit,
) {
    val maxValue = durationMs.coerceAtLeast(1L).toFloat()
    Slider(
        value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toFloat(),
        onValueChange = {
            onSeekStart()
            onSeekChange(it)
        },
        onValueChangeFinished = onSeekEnd,
        valueRange = 0f..maxValue,
        colors = SliderDefaults.colors(
            thumbColor = NeonViolet,
            activeTrackColor = NeonViolet,
            inactiveTrackColor = Color.White.copy(alpha = 0.18f),
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = positionMs.formatDuration(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        SpeedChip(speed = playbackSpeed, onChange = onSpeedChange)
        Spacer(Modifier.weight(1f))
        Text(
            text = durationMs.formatDuration(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SpeedChip(
    speed: Float,
    onChange: (Float) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = speed.formatSpeed(),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                color = if (speed == 1.0f) MaterialTheme.colorScheme.onSurface else NeonViolet,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { s ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = s.formatSpeed(),
                            color = if (s == speed) NeonViolet else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        onChange(s)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun Float.formatSpeed(): String {
    val s = "%.2f".format(this).trimEnd('0').trimEnd('.')
    return "${s}×"
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffle) NeonViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(40.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(NeonViolet),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                    Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) NeonViolet
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
