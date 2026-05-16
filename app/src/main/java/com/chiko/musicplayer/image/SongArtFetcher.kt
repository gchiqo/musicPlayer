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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.source

class SongArtFetcher(
    private val song: Song,
    private val context: Context,
) : Fetcher {

    // MediaMetadataRetriever.setDataSource opens and parses each media file —
    // expensive. A fast fling can fan out dozens of these onto Dispatchers.IO
    // at once, thrashing disk/CPU and dropping frames on cheap phones. Cap the
    // number that run concurrently so scrolling stays smooth; off-screen
    // requests are cancelled by Coil while they wait on the permit.
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        extractionLimit.withPermit { fetchInner() }
    }

    private fun fetchInner(): FetchResult? {
        extractEmbeddedPicture(context, song.uri)?.let { bytes ->
            return SourceResult(
                source = ImageSource(Buffer().apply { write(bytes) }, context),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }
        return albumArt()
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

        // Shared across every fetcher instance — the cap is process-wide.
        val extractionLimit = Semaphore(4)
    }
}

class SongKeyer : Keyer<Song> {
    override fun key(data: Song, options: Options): String = "song-art:${data.id}"
}
