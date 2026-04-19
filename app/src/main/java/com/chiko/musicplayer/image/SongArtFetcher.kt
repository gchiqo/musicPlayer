package com.chiko.musicplayer.image

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.chiko.musicplayer.audio.extractEmbeddedPicture
import com.chiko.musicplayer.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.source

class SongArtFetcher(
    private val song: Song,
    private val context: Context,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        extractEmbeddedPicture(context, song.uri)?.let { bytes ->
            return@withContext SourceResult(
                source = ImageSource(Buffer().apply { write(bytes) }, context),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }
        albumArt()
    }

    private fun albumArt(): SourceResult? {
        if (song.albumId <= 0L) return null
        val uri = ContentUris.withAppendedId(ALBUM_ART, song.albumId)
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            SourceResult(
                source = ImageSource(stream.source().buffer(), context),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        } catch (_: Exception) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Song> {
        override fun create(data: Song, options: Options, imageLoader: ImageLoader): Fetcher =
            SongArtFetcher(data, context)
    }

    private companion object {
        val ALBUM_ART: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}

class SongKeyer : Keyer<Song> {
    override fun key(data: Song, options: Options): String = "song-art:${data.id}"
}
