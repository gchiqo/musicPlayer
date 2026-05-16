package com.chiko.musicplayer.youtube

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloadCenter(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val downloader = YoutubeFileDownloader(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "YouTube audio download progress" }
            )
        }
    }

    fun downloadAudio(
        video: YoutubeVideo,
        streamUrl: String,
        folder: String? = null,
        onDone: (Boolean) -> Unit = {},
    ) {
        scope.launch {
            val notifId = (video.id.toInt() and Int.MAX_VALUE).coerceAtLeast(1)
            var lastPct = -1
            show(notifId, video.title, 0, ongoing = true)

            val ok = downloader.downloadAudio(video, streamUrl, folder) { written, total ->
                if (total <= 0L) return@downloadAudio
                val pct = ((written * 100L) / total).toInt().coerceIn(0, 100)
                if (pct != lastPct) {
                    lastPct = pct
                    show(notifId, video.title, pct, ongoing = true)
                }
            }

            showDone(notifId, video.title, ok)
            onDone(ok)
        }
    }

    private fun show(id: Int, title: String, percent: Int, ongoing: Boolean) {
        if (!canPostNotifications()) return
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$percent%")
            .setProgress(100, percent, false)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(id, n)
    }

    private fun showDone(id: Int, title: String, success: Boolean) {
        if (!canPostNotifications()) return
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(if (success) "Download complete" else "Download failed")
            .setContentText(title)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(id, n)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "yt_downloads"
    }
}
