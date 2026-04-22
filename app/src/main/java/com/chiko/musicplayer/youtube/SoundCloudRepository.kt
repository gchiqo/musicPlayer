package com.chiko.musicplayer.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val TAG = "Resonance"

/**
 * Thin SoundCloud wrapper that mirrors [YoutubeRepository]'s API but targets
 * `ServiceList.SoundCloud`. Returns the shared YoutubeVideo/Result/Feed types
 * tagged with [StreamSource.SoundCloud] so the rest of the app doesn't need
 * to care which backend produced a track.
 */
class SoundCloudRepository {

    private val service get() = ServiceList.SoundCloud

    suspend fun search(
        query: String,
        filter: YoutubeFilter = YoutubeFilter.Videos,
    ): YoutubeSearchPage = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext YoutubeSearchPage(emptyList(), null)
        Log.d(TAG, "sc search start: $query (${filter.name})")
        val primary = runCatching {
            val qh = service.searchQHFactory.fromQuery(query, contentFiltersFor(filter), "")
            val extractor = service.getSearchExtractor(qh)
            extractor.fetchPage()
            extractor.initialPage
        }.onFailure { Log.w(TAG, "sc search with filter failed, retrying 'all'", it) }
            .getOrNull()
        val page = primary ?: run {
            val qh = service.searchQHFactory.fromQuery(query, emptyList(), "")
            val extractor = service.getSearchExtractor(qh)
            extractor.fetchPage()
            extractor.initialPage
        }
        val results = mapSearchItems(page.items)
        Log.d(TAG, "sc search done: ${results.size} results (hasNext=${page.nextPage != null})")
        YoutubeSearchPage(results, page.nextPage)
    }

    suspend fun searchNext(
        query: String,
        filter: YoutubeFilter,
        pageToken: Page,
    ): YoutubeSearchPage = withContext(Dispatchers.IO) {
        val qh = service.searchQHFactory.fromQuery(query, contentFiltersFor(filter), "")
        val extractor = service.getSearchExtractor(qh)
        val page = extractor.getPage(pageToken)
        YoutubeSearchPage(mapSearchItems(page.items), page.nextPage)
    }

    suspend fun resolveAudioStream(url: String): ResolvedStream? = withContext(Dispatchers.IO) {
        Log.d(TAG, "sc resolveAudio start: $url")
        val info = try {
            StreamInfo.getInfo(service, url)
        } catch (t: Throwable) {
            Log.e(TAG, "sc resolveAudio getInfo failed", t)
            throw t
        }
        val streams = info.audioStreams.filter { it.content.isNotBlank() }
        // Prefer progressive MP3 over HLS — simpler to cache, download, and seek.
        val progressive = streams.firstOrNull {
            it.format?.mimeType?.contains("mpeg", ignoreCase = true) == true ||
                it.format?.mimeType?.contains("mp3", ignoreCase = true) == true
        }
        val picked = progressive ?: streams.maxByOrNull { it.averageBitrate }
        if (picked == null) {
            Log.w(TAG, "sc resolveAudio: no audio stream")
            return@withContext null
        }
        Log.d(TAG, "sc resolveAudio ok: ${picked.format?.mimeType}")
        ResolvedStream(url = picked.content, mimeType = picked.format?.mimeType)
    }

    suspend fun loadPlaylist(url: String): YoutubeFeed = withContext(Dispatchers.IO) {
        Log.d(TAG, "sc loadPlaylist: $url")
        val extractor = service.getPlaylistExtractor(url)
        extractor.fetchPage()
        val page = extractor.initialPage
        val tracks = page.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toYoutubeVideo() }
        YoutubeFeed(
            url = url,
            title = extractor.name.orEmpty(),
            subtitle = extractor.uploaderName.orEmpty(),
            thumbnailUrl = extractor.thumbnails?.maxByOrNull { it.width }?.url,
            videos = tracks,
            kind = YoutubeFeed.Kind.Playlist,
            nextPage = page.nextPage,
        )
    }

    suspend fun loadChannel(url: String): YoutubeFeed = withContext(Dispatchers.IO) {
        Log.d(TAG, "sc loadChannel: $url")
        val extractor = service.getChannelExtractor(url)
        extractor.fetchPage()
        val name = extractor.name.orEmpty()
        val avatarUrl = extractor.avatars?.maxByOrNull { it.width }?.url
        val subscribers = runCatching { extractor.subscriberCount }.getOrDefault(-1L)

        val tracks = runCatching {
            val tab = extractor.tabs.firstOrNull()
            if (tab != null) {
                val tabExtractor = service.getChannelTabExtractor(tab)
                tabExtractor.fetchPage()
                tabExtractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .map { it.toYoutubeVideo() }
            } else emptyList()
        }.getOrElse { t ->
            Log.w(TAG, "sc channel tab fetch failed", t)
            emptyList()
        }

        YoutubeFeed(
            url = url,
            title = name,
            subtitle = if (subscribers > 0) formatFollowers(subscribers) else "",
            thumbnailUrl = avatarUrl,
            videos = tracks,
            kind = YoutubeFeed.Kind.Channel,
        )
    }

    suspend fun playlistNext(
        playlistUrl: String,
        pageToken: Page,
    ): Pair<List<YoutubeVideo>, Page?> = withContext(Dispatchers.IO) {
        val extractor = service.getPlaylistExtractor(playlistUrl)
        val page = extractor.getPage(pageToken)
        val tracks = page.items.filterIsInstance<StreamInfoItem>().map { it.toYoutubeVideo() }
        Pair(tracks, page.nextPage)
    }

    private fun contentFiltersFor(filter: YoutubeFilter): List<String> = when (filter) {
        YoutubeFilter.Videos -> listOf("tracks")
        YoutubeFilter.Channels -> listOf("users")
        YoutubeFilter.Playlists -> listOf("playlists")
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

    private fun formatFollowers(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM followers".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fK followers".format(n / 1_000.0)
        else -> "$n followers"
    }

    private fun StreamInfoItem.toYoutubeVideo(): YoutubeVideo = YoutubeVideo(
        url = url,
        title = name.orEmpty(),
        uploader = uploaderName.orEmpty(),
        thumbnailUrl = thumbnails.maxByOrNull { it.width }?.url,
        durationSec = duration.coerceAtLeast(0),
        source = StreamSource.SoundCloud,
    )
}
