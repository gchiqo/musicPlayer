package com.chiko.musicplayer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.chiko.musicplayer.image.songArtUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    /** Result of attempting to update audio file metadata. */
    sealed class WriteResult {
        object Success : WriteResult()
        /** R+ requires explicit user consent before writing. Caller must launch [intentSender]. */
        data class NeedsConsent(val intentSender: IntentSender) : WriteResult()
        data class Failure(val error: Throwable) : WriteResult()
    }

    /**
     * Returns an IntentSender the user must approve to allow writes on R+, or null if
     * pre-R (no consent dialog needed; we just attempt the write directly).
     */
    fun buildWriteConsent(uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        if (uris.isEmpty()) return null
        return MediaStore.createWriteRequest(context.contentResolver, uris).intentSender
    }

    /** Updates RELATIVE_PATH on each URI to "Music/<folderName>/". */
    suspend fun moveToFolder(uris: List<Uri>, folderName: String): Int = withContext(Dispatchers.IO) {
        val safe = sanitizeFolderName(folderName)
        if (safe.isBlank()) return@withContext 0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-Q has no RELATIVE_PATH; would require File operations. Skip for now.
            Log.w("Resonance", "moveToFolder unsupported on API < 29")
            return@withContext 0
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/$safe/")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val finalize = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
        var updated = 0
        for (uri in uris) {
            try {
                val n = context.contentResolver.update(uri, values, null, null)
                if (n > 0) {
                    context.contentResolver.update(uri, finalize, null, null)
                    updated++
                }
            } catch (t: Throwable) {
                Log.w("Resonance", "moveToFolder failed for $uri", t)
            }
        }
        updated
    }

    private fun sanitizeFolderName(name: String): String {
        val trimmed = name.trim().trimStart('/').trimEnd('/')
        // Strip characters not safe for paths.
        return trimmed.replace(Regex("[\\\\:*?\"<>|]"), "")
    }


    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.BUCKET_ID)
                add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = mutableListOf<Song>()
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val bucketIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val (folderId, folderName) = resolveFolder(cursor, bucketIdCol, bucketNameCol, dataCol)
                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown artist",
                    album = cursor.getString(albumCol) ?: "Unknown album",
                    albumId = albumId,
                    durationMs = cursor.getLong(durationCol),
                    uri = ContentUris.withAppendedId(collection, id),
                    artworkUri = songArtUri(id),
                    folderId = folderId,
                    folderName = folderName,
                    dateAddedSec = cursor.getLong(dateAddedCol),
                )
            }
        }
        songs
    }

    private fun resolveFolder(
        cursor: android.database.Cursor,
        bucketIdCol: Int,
        bucketNameCol: Int,
        dataCol: Int,
    ): Pair<Long, String> {
        if (bucketIdCol >= 0 && bucketNameCol >= 0 && !cursor.isNull(bucketIdCol)) {
            val name = cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: "Unknown"
            return cursor.getLong(bucketIdCol) to name
        }
        if (dataCol >= 0) {
            val path = cursor.getString(dataCol).orEmpty()
            val parent = path.substringBeforeLast('/', "").substringAfterLast('/').ifEmpty { "Unknown" }
            return parent.lowercase().hashCode().toLong() to parent
        }
        return 0L to "Unknown"
    }

}
