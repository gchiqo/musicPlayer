package com.chiko.musicplayer.youtube

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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

        for ((key, values) in request.headers()) {
            if (key.equals("Content-Length", ignoreCase = true)) continue
            for (v in values) {
                conn.addRequestProperty(key, v)
            }
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
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: IOException) {
            ""
        } finally {
            conn.disconnect()
        }

        return Response(code, message, headers, responseBody, conn.url.toString())
    }
}
