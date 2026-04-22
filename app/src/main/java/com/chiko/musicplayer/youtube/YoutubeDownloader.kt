package com.chiko.musicplayer.youtube

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

private const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0"

class YoutubeDownloader : Downloader() {

    override fun execute(request: Request): Response {
        val conn = (URL(request.url()).openConnection() as HttpURLConnection).apply {
            requestMethod = request.httpMethod()
            connectTimeout = 30_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            useCaches = false
            doInput = true
        }

        var sawUserAgent = false
        for ((key, values) in request.headers()) {
            // Skip headers that HttpURLConnection manages itself, or that would
            // force us to handle decompression manually. If we forward
            // `Accept-Encoding: gzip`, HttpURLConnection assumes the caller will
            // decompress — which breaks SoundCloud's JS scrape (gibberish body,
            // clientId regex misses).
            if (key.equals("Content-Length", ignoreCase = true)) continue
            if (key.equals("Accept-Encoding", ignoreCase = true)) continue
            if (key.equals("Host", ignoreCase = true)) continue
            if (key.equals("User-Agent", ignoreCase = true)) sawUserAgent = true
            for (v in values) {
                conn.addRequestProperty(key, v)
            }
        }
        if (!sawUserAgent) {
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
        }

        val body = request.dataToSend()
        if (body != null) {
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { it.write(body) }
        }

        val code = conn.responseCode
        val message = conn.responseMessage ?: ""
        val headers: Map<String, List<String>> = conn.headerFields
            .filterKeys { it != null }
            .mapValues { it.value.orEmpty() }
        val responseBody = try {
            val raw: InputStream? = if (code in 200..299) conn.inputStream else conn.errorStream
            val encoding = conn.contentEncoding
            val decoded = if (raw != null && encoding?.equals("gzip", ignoreCase = true) == true) {
                GZIPInputStream(raw)
            } else raw
            decoded?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: IOException) {
            ""
        } finally {
            conn.disconnect()
        }

        return Response(code, message, headers, responseBody, conn.url.toString())
    }
}
