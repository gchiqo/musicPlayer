package com.chiko.musicplayer.youtube

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class YoutubeFileDownloader(private val context: Context) {

    suspend fun downloadAudio(
        video: YoutubeVideo,
        streamUrl: String,
        onProgress: ((written: Long, total: Long) -> Unit)? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            var temp: File? = null
            try {
                val ext = if (video.source == StreamSource.SoundCloud) "mp3" else "m4a"
                val tmp = File(context.cacheDir, "audio-${System.currentTimeMillis()}.$ext")
                temp = tmp
                Log.d(TAG, "downloadAudio: streaming to ${tmp.name}")
                downloadToFile(streamUrl, tmp, onProgress)

                val thumbBytes = video.thumbnailUrl?.let { downloadBytes(it) }
                if (thumbBytes != null) {
                    runCatching { embedArtwork(tmp, thumbBytes, video) }
                        .onFailure { Log.e(TAG, "embedArtwork failed (continuing)", it) }
                } else {
                    Log.w(TAG, "no thumbnail bytes, saving without cover")
                }

                val filename = sanitize(video.title) + "." + ext
                val ok = saveAudioToPublicMusic(tmp, filename)
                Log.d(TAG, "downloadAudio done: ok=$ok")
                ok
            } catch (t: Throwable) {
                Log.e(TAG, "downloadAudio failed", t)
                false
            } finally {
                temp?.delete()
            }
        }

    fun downloadVideo(video: YoutubeVideo, streamUrl: String): Long {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val filename = sanitize(video.title) + ".mp4"
        val request = DownloadManager.Request(Uri.parse(streamUrl))
            .setTitle(video.title)
            .setDescription("Resonance video download")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        return manager.enqueue(request)
    }

    private fun downloadToFile(
        url: String,
        dest: File,
        onProgress: ((written: Long, total: Long) -> Unit)? = null,
    ) {
        dest.outputStream().use { out ->
            var from = 0L
            var total = -1L
            val chunk = 4L * 1024 * 1024
            while (true) {
                val to = from + chunk - 1
                val wrote = fetchRange(url, from, to, out)
                if (wrote == FETCH_WHOLE_FILE) {
                    Log.d(TAG, "downloadToFile: server sent whole file in one response")
                    val size = dest.length()
                    onProgress?.invoke(size, size)
                    return
                }
                if (total == -1L) total = wrote.second
                from += wrote.first
                if (total > 0) onProgress?.invoke(from, total)
                if (wrote.first == 0L) break
                if (total > 0 && from >= total) break
            }
            Log.d(TAG, "downloadToFile done, bytes=$from total=$total")
        }
    }

    /**
     * Fetch [from]..[to] into [out]. Returns (bytesWritten, totalSize) or [FETCH_WHOLE_FILE]
     * if the server ignored the Range header and returned the entire body.
     */
    private fun fetchRange(url: String, from: Long, to: Long, out: java.io.OutputStream): Pair<Long, Long> {
        var attempt = 0
        while (true) {
            attempt++
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Range", "bytes=$from-$to")
            conn.connectTimeout = 30_000
            conn.readTimeout = 45_000
            conn.instanceFollowRedirects = true
            try {
                val code = conn.responseCode
                if (code != 200 && code != 206) {
                    throw java.io.IOException("HTTP $code for range $from-$to")
                }
                val total = when {
                    code == 206 -> conn.getHeaderField("Content-Range")
                        ?.substringAfterLast('/')
                        ?.toLongOrNull() ?: -1L
                    code == 200 -> conn.contentLengthLong
                    else -> -1L
                }
                var written = 0L
                conn.inputStream.use { inp ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = inp.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        written += n
                    }
                }
                return if (code == 200) FETCH_WHOLE_FILE else written to total
            } catch (e: java.io.IOException) {
                if (attempt >= 3) throw e
                Log.w(TAG, "range $from-$to attempt $attempt failed: ${e.message}; retrying")
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun downloadBytes(url: String): ByteArray? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun embedArtwork(audioFile: File, artBytes: ByteArray, video: YoutubeVideo) {
        val audio = AudioFileIO.read(audioFile)
        val tag = audio.tagOrCreateAndSetDefault
        runCatching { tag.setField(FieldKey.TITLE, video.title) }
        runCatching { tag.setField(FieldKey.ARTIST, video.uploader) }
        runCatching { tag.setField(FieldKey.ALBUM, video.source.label) }

        val artwork = ArtworkFactory.getNew().apply {
            binaryData = artBytes
            mimeType = "image/jpeg"
        }
        runCatching { tag.deleteArtworkField() }
        tag.setField(artwork)

        AudioFileIO.write(audio)
    }

    private fun saveAudioToPublicMusic(source: File, filename: String): Boolean {
        val mime = if (filename.endsWith(".mp3", ignoreCase = true)) "audio/mpeg" else "audio/mp4"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Audio.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: return false
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null,
            )
            true
        } else {
            @Suppress("DEPRECATION")
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            musicDir.mkdirs()
            val destFile = File(musicDir, filename)
            source.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(mime),
                null,
            )
            true
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(120).ifEmpty { "download" }

    private companion object {
        const val TAG = "Resonance"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; SM-G781U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        // sentinel: server didn't honor Range and sent the whole body
        val FETCH_WHOLE_FILE: Pair<Long, Long> = -1L to -1L
    }
}
