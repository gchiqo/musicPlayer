package com.chiko.musicplayer.youtube

import org.schabi.newpipe.extractor.Page

data class YoutubeFeed(
    val url: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val videos: List<YoutubeVideo>,
    val kind: Kind,
    val nextPage: Page? = null,
) {
    enum class Kind { Playlist, Channel }
}

data class YoutubeSearchPage(
    val items: List<YoutubeResult>,
    val nextPage: Page?,
)
