package com.chiko.musicplayer.youtube

import android.net.Uri
import com.chiko.musicplayer.data.Song

enum class StreamSource(val label: String) {
    YouTube("YouTube"),
    SoundCloud("SoundCloud"),
}

data class YoutubeVideo(
    val url: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val durationSec: Long,
    val source: StreamSource = StreamSource.YouTube,
) {
    val id: Long get() = ("${source.name}:$url").hashCode().toLong()

    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = uploader,
        album = source.label,
        albumId = 0L,
        durationMs = durationSec * 1000L,
        uri = Uri.parse(url),
        artworkUri = Uri.parse(thumbnailUrl.orEmpty()),
        folderId = -1L,
        folderName = source.label,
        dateAddedSec = 0L,
        isRemote = true,
    )
}
