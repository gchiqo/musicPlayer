package com.chiko.musicplayer.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val TAG = "Resonance"

class YoutubeRepository {

    suspend fun search(
        query: String,
        filter: YoutubeFilter = YoutubeFilter.Videos,
    ): YoutubeSearchPage = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext YoutubeSearchPage(emptyList(), null)
        Log.d(TAG, "yt search start: $query (${filter.name})")
        val service = ServiceList.YouTube
        val contentFilters = when (filter) {
            YoutubeFilter.Videos -> listOf("videos")
            YoutubeFilter.Channels -> listOf("channels")
            YoutubeFilter.Playlists -> listOf("playlists")
        }
        val qh = service.searchQHFactory.fromQuery(query, contentFilters, "")
        val extractor = service.getSearchExtractor(qh)
        try {
            extractor.fetchPage()
        } catch (t: Throwable) {
            Log.e(TAG, "yt search fetchPage failed", t)
            throw t
        }
        val page = extractor.initialPage
        val results = mapSearchItems(page.items)
        Log.d(TAG, "yt search done: ${results.size} results (hasNext=${page.nextPage != null})")
        YoutubeSearchPage(results, page.nextPage)
    }

    suspend fun searchNext(
        query: String,
        filter: YoutubeFilter,
        pageToken: Page,
    ): YoutubeSearchPage = withContext(Dispatchers.IO) {
        Log.d(TAG, "yt searchNext: $query")
        val service = ServiceList.YouTube
        val contentFilters = when (filter) {
            YoutubeFilter.Videos -> listOf("videos")
            YoutubeFilter.Channels -> listOf("channels")
            YoutubeFilter.Playlists -> listOf("playlists")
        }
        val qh = service.searchQHFactory.fromQuery(query, contentFilters, "")
        val extractor = service.getSearchExtractor(qh)
        val page = extractor.getPage(pageToken)
        YoutubeSearchPage(mapSearchItems(page.items), page.nextPage)
    }

    suspend fun playlistNext(
        playlistUrl: String,
        pageToken: Page,
    ): Pair<List<YoutubeVideo>, Page?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "yt playlistNext: $playlistUrl")
        val service = ServiceList.YouTube
        val extractor = service.getPlaylistExtractor(playlistUrl)
        val page = extractor.getPage(pageToken)
        val videos = page.items.filterIsInstance<StreamInfoItem>().map { it.toYoutubeVideo() }
        Pair(videos, page.nextPage)
    }

    private fun mapSearchItems(items: List<*>): List<YoutubeResult> = items.mapNotNull { item ->
        when (item) {
            is StreamInfoItem -> YoutubeResult.Video(item.toYoutubeVideo())
            is ChannelInfoItem -> YoutubeResult.Channel(
                url = item.url,
                name = item.name.orEmpty(),
                avatarUrl = item.thumbnails?.maxByOrNull { it.width }?.url,
                subscribers = item.subscriberCount.coerceAtLeast(-1L),
            )
            is PlaylistInfoItem -> YoutubeResult.Playlist(
                url = item.url,
                title = item.name.orEmpty(),
                uploader = item.uploaderName.orEmpty(),
                thumbnailUrl = item.thumbnails?.maxByOrNull { it.width }?.url,
                videoCount = item.streamCount.coerceAtLeast(0L),
            )
            else -> null
        }
    }

    suspend fun resolveAudioStream(url: String): ResolvedStream? = withContext(Dispatchers.IO) {
        Log.d(TAG, "yt resolveAudio start: $url")
        val info = try {
            StreamInfo.getInfo(ServiceList.YouTube, url)
        } catch (t: Throwable) {
            Log.e(TAG, "yt resolveAudio getInfo failed", t)
            throw t
        }
        val nonEmpty = info.audioStreams.filter { it.content.isNotBlank() }
        // Prefer AAC in an MP4 container so the file extension (.m4a) is honest
        // and JAudioTagger can write cover-art atoms into it.
        val preferredMp4 = nonEmpty
            .filter { it.format?.mimeType?.contains("mp4", ignoreCase = true) == true }
            .maxByOrNull { it.averageBitrate }
        val audio = preferredMp4 ?: nonEmpty.maxByOrNull { it.averageBitrate }
        if (audio == null) {
            Log.w(TAG, "yt resolveAudio: no audio stream")
            return@withContext null
        }
        Log.d(TAG, "yt resolveAudio ok: ${audio.format?.mimeType} @ ${audio.averageBitrate}bps")
        ResolvedStream(url = audio.content, mimeType = audio.format?.mimeType)
    }

    suspend fun resolveVideoStream(url: String): ResolvedStream? = withContext(Dispatchers.IO) {
        Log.d(TAG, "yt resolveVideo start: $url")
        val info = try {
            StreamInfo.getInfo(ServiceList.YouTube, url)
        } catch (t: Throwable) {
            Log.e(TAG, "yt resolveVideo getInfo failed", t)
            throw t
        }
        val progressive = info.videoStreams
            .filter { !it.isVideoOnly && it.content.isNotBlank() }
            .maxByOrNull { parseResolution(it.resolution) }
        val picked = progressive ?: info.videoStreams
            .filter { it.content.isNotBlank() }
            .maxByOrNull { parseResolution(it.resolution) }
        if (picked == null) {
            Log.w(TAG, "yt resolveVideo: no video stream")
            return@withContext null
        }
        Log.d(TAG, "yt resolveVideo ok: ${picked.resolution} ${picked.format?.mimeType}")
        ResolvedStream(url = picked.content, mimeType = picked.format?.mimeType)
    }

    suspend fun loadPlaylist(url: String): YoutubeFeed = withContext(Dispatchers.IO) {
        Log.d(TAG, "yt loadPlaylist: $url")
        val service = ServiceList.YouTube
        val extractor = service.getPlaylistExtractor(url)
        extractor.fetchPage()
        val page = extractor.initialPage
        val videos = page.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toYoutubeVideo() }
        YoutubeFeed(
            url = url,
            title = extractor.name.orEmpty(),
            subtitle = extractor.uploaderName.orEmpty(),
            thumbnailUrl = extractor.thumbnails?.maxByOrNull { it.width }?.url,
            videos = videos,
            kind = YoutubeFeed.Kind.Playlist,
            nextPage = page.nextPage,
        )
    }

    suspend fun loadChannel(url: String): YoutubeFeed = withContext(Dispatchers.IO) {
        Log.d(TAG, "yt loadChannel: $url")
        val service = ServiceList.YouTube
        val extractor = service.getChannelExtractor(url)
        extractor.fetchPage()
        val name = extractor.name.orEmpty()
        val avatarUrl = extractor.avatars?.maxByOrNull { it.width }?.url
        val subscribers = runCatching { extractor.subscriberCount }.getOrDefault(-1L)

        val videosTab = extractor.tabs.firstOrNull { tab ->
            tab.contentFilters.any { f -> f.equals(ChannelTabs.VIDEOS, ignoreCase = true) }
        } ?: extractor.tabs.firstOrNull()

        val videos = if (videosTab != null) {
            runCatching {
                val tabExtractor = service.getChannelTabExtractor(videosTab)
                tabExtractor.fetchPage()
                tabExtractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .map { it.toYoutubeVideo() }
            }.getOrElse { t ->
                Log.w(TAG, "channel tab fetch failed", t)
                emptyList()
            }
        } else emptyList()

        YoutubeFeed(
            url = url,
            title = name,
            subtitle = if (subscribers > 0) formatSubs(subscribers) else "",
            thumbnailUrl = avatarUrl,
            videos = videos,
            kind = YoutubeFeed.Kind.Channel,
        )
    }

    private fun formatSubs(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM subscribers".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fK subscribers".format(n / 1_000.0)
        else -> "$n subscribers"
    }

    private fun parseResolution(resolution: String?): Int {
        if (resolution.isNullOrBlank()) return 0
        return resolution.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
    }

    private fun StreamInfoItem.toYoutubeVideo(): YoutubeVideo = YoutubeVideo(
        url = url,
        title = name.orEmpty(),
        uploader = uploaderName.orEmpty(),
        thumbnailUrl = thumbnails.maxByOrNull { it.width }?.url,
        durationSec = duration.coerceAtLeast(0),
    )
}

data class ResolvedStream(
    val url: String,
    val mimeType: String?,
)
