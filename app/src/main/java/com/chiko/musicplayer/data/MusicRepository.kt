package com.chiko.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

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
                    artworkUri = ContentUris.withAppendedId(ARTWORK_URI, albumId),
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

    private companion object {
        val ARTWORK_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
