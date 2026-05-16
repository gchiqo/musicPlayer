package com.chiko.musicplayer.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.chiko.musicplayer.MainActivity
import com.chiko.musicplayer.audio.AudioVisualizerProcessor
import com.chiko.musicplayer.audio.EqualizerManager

@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        EqualizerManager.init(this)

        val visualizer = AudioVisualizerProcessor()
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessorChain(
                        DefaultAudioSink.DefaultAudioProcessorChain(visualizer)
                    )
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        EqualizerManager.attach(player.audioSessionId)
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int,
            ) {
                EqualizerManager.attach(audioSessionId)
            }
        })

        // Tapping the media notification reopens the app and jumps straight
        // to the now-playing screen.
        val openPlayerIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            openPlayerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        EqualizerManager.detach()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
