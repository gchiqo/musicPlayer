package com.chiko.musicplayer.youtube

import android.net.Uri
import com.chiko.musicplayer.data.Song

data class YoutubeVideo(
    val url: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val durationSec: Long,
) {
    val id: Long get() = ("yt:$url").hashCode().toLong()

    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = uploader,
        album = "YouTube",
        albumId = 0L,
        durationMs = durationSec * 1000L,
        uri = Uri.parse(url),
        artworkUri = Uri.parse(thumbnailUrl.orEmpty()),
        folderId = -1L,
        folderName = "YouTube",
        dateAddedSec = 0L,
        isRemote = true,
    )
}
