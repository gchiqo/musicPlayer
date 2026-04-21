package com.chiko.musicplayer.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val TAG = "Resonance"

class YoutubeRepository {

    suspend fun search(query: String): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        Log.d(TAG, "yt search start: $query")
        val service = ServiceList.YouTube
        val qh = service.searchQHFactory.fromQuery(query)
        val extractor = service.getSearchExtractor(qh)
        try {
            extractor.fetchPage()
        } catch (t: Throwable) {
            Log.e(TAG, "yt search fetchPage failed", t)
            throw t
        }
        val results = extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toYoutubeVideo() }
        Log.d(TAG, "yt search done: ${results.size} results")
        results
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
