package com.chiko.musicplayer.image

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.LruCache
import com.chiko.musicplayer.audio.extractEmbeddedPicture
import com.chiko.musicplayer.audio.resizeArtwork
import java.io.FileNotFoundException

class ArtContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") throw FileNotFoundException("Read-only access only")
        val songId = uri.lastPathSegment?.toLongOrNull() ?: return null
        val bytes = loadBytes(songId) ?: return null
        return writeToPipe(bytes)
    }

    override fun getType(uri: Uri): String = "image/jpeg"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun loadBytes(songId: Long): ByteArray? {
        cache.get(songId)?.let { return if (it.isEmpty()) null else it }
        val ctx = context ?: return null
        val songUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId,
        )
        val embedded = extractEmbeddedPicture(ctx, songUri)?.let { resizeArtwork(it) }
        if (embedded != null) {
            cache.put(songId, embedded)
            return embedded
        }
        val albumId = queryAlbumId(songId)
        if (albumId != null && albumId > 0L) {
            val albumUri = ContentUris.withAppendedId(ALBUM_ART, albumId)
            try {
                ctx.contentResolver.openInputStream(albumUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) {
                        val resized = resizeArtwork(bytes)
                        cache.put(songId, resized)
                        return resized
                    }
                }
            } catch (_: Throwable) {}
        }
        cache.put(songId, EMPTY)
        return null
    }

    private fun queryAlbumId(songId: Long): Long? {
        val ctx = context ?: return null
        val songUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId,
        )
        return ctx.contentResolver.query(
            songUri,
            arrayOf(MediaStore.Audio.Media.ALBUM_ID),
            null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
    }

    private fun writeToPipe(bytes: ByteArray): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]
        Thread {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { it.write(bytes) }
            } catch (_: Throwable) {
                try { writeSide.close() } catch (_: Throwable) {}
            }
        }.start()
        return readSide
    }

    companion object {
        const val AUTHORITY = "com.chiko.musicplayer.art"
        private val ALBUM_ART: Uri = Uri.parse("content://media/external/audio/albumart")
        private val EMPTY = ByteArray(0)
        private val cache = LruCache<Long, ByteArray>(40)
    }
}

fun songArtUri(songId: Long): Uri =
    Uri.parse("content://${ArtContentProvider.AUTHORITY}/$songId")
