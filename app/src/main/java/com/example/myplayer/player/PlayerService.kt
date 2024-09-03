package com.example.myplayer.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.myplayer.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture


class PlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val player: ExoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AUDIO_CONTENT_TYPE_MUSIC).setUsage(USAGE_MEDIA).build()
                ,true)
            .build()
        player.volume = 0.1f

        val channel = NotificationChannel(
            MediaSessionNotificationProvider.DEFAULT_CHANNEL_ID,
            "Player",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = getString(R.string.player_channel_description)

        val notificationManager =
            getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MyCallback()).build()
        this.setMediaNotificationProvider(MediaSessionNotificationProvider(this))

    }

    private inner class MyCallback : MediaSession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Set available player and session commands.
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .build()
                ).build()
        }

        @OptIn(UnstableApi::class)
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val item = mediaSession.player.currentMediaItem
                return Futures.immediateFuture( MediaSession.MediaItemsWithStartPosition(
                    if(item != null) listOf(item) else emptyList(),0,0
                ))

        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            "TO DO"
            /*
            if (customCommand.customAction == ACTION_FAVORITES) {
                // Do custom logic here
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }*/
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
    // This example always accepts the connection request
    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession


    @OptIn(UnstableApi::class)
    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (mediaSession?.player?.playWhenReady != true) {
            stopSelf()
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {

        mediaSession?.run {
            player.stop()
            player.release()
            mediaSession?.release()
            mediaSession = null
            release()

        }
        super.onDestroy()
    }
}