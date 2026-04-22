package com.chiko.musicplayer.youtube

sealed class YoutubeResult {
    abstract val url: String

    data class Video(val video: YoutubeVideo) : YoutubeResult() {
        override val url: String get() = video.url
    }

    data class Channel(
        override val url: String,
        val name: String,
        val avatarUrl: String?,
        val subscribers: Long,
    ) : YoutubeResult()

    data class Playlist(
        override val url: String,
        val title: String,
        val uploader: String,
        val thumbnailUrl: String?,
        val videoCount: Long,
    ) : YoutubeResult()
}

enum class YoutubeFilter { Videos, Channels, Playlists }
