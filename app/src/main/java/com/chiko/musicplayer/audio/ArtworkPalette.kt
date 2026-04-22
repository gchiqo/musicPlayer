package com.chiko.musicplayer.audio

import android.content.Context
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import com.chiko.musicplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class ArtworkColors(val accent: Int, val background: Int)

suspend fun extractArtworkColors(
    context: Context,
    song: Song,
    fallbackAccent: Int,
    fallbackBackground: Int,
): ArtworkColors = withContext(Dispatchers.IO) {
    val bytes: ByteArray? = if (song.isRemote) {
        runCatching {
            URL(song.artworkUri.toString()).openStream().use { it.readBytes() }
        }.getOrNull()
    } else {
        extractEmbeddedPicture(context, song.uri)
    }
    if (bytes == null || bytes.isEmpty()) {
        return@withContext ArtworkColors(fallbackAccent, fallbackBackground)
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: return@withContext ArtworkColors(fallbackAccent, fallbackBackground)
    val palette = try {
        Palette.from(bitmap).maximumColorCount(16).generate()
    } catch (_: Throwable) {
        return@withContext ArtworkColors(fallbackAccent, fallbackBackground)
    } finally {
        bitmap.recycle()
    }
    val accent = palette.getVibrantColor(0)
        .takeIf { it != 0 }
        ?: palette.getLightVibrantColor(0).takeIf { it != 0 }
        ?: palette.getMutedColor(fallbackAccent)
    val bg = palette.getDarkMutedColor(0)
        .takeIf { it != 0 }
        ?: palette.getDarkVibrantColor(fallbackBackground)
    ArtworkColors(accent, bg)
}
