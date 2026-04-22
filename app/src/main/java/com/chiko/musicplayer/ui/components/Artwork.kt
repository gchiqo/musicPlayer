package com.chiko.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chiko.musicplayer.data.Song

@Composable
fun Artwork(
    song: Song?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    iconSize: Dp = 32.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val gradient = remember(accent, surface) {
        Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.4f),
                surface,
            )
        )
    }
    var failed by remember(song?.id) { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (contentScale == ContentScale.Crop) Modifier.background(gradient)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (song != null && !failed) {
            val model: Any = if (song.isRemote) song.artworkUri.toString() else song
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .memoryCacheKey("song-art:${song.id}")
                    .diskCacheKey("song-art:${song.id}")
                    .build(),
                contentDescription = null,
                contentScale = contentScale,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
