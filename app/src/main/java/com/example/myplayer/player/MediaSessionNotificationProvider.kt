package com.example.myplayer.player

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import com.example.myplayer.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.Arrays

@UnstableApi
class MediaSessionNotificationProvider(private var context: Context) : MediaNotification.Provider {
    private lateinit var nBuilder: NotificationCompat.Builder


    companion object {
            const val DEFAULT_CHANNEL_ID: String = "player_channel_id"
            const val DEFAULT_NOTIFICATION_ID: Int = 1002
    }
    private val pendingOnBitmapLoadedFutureCallback: OnBitmapLoadedFutureCallback? =
        null

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        TODO("Not yet implemented")
    }

    private fun addNotificationActions(
        mediaSession: MediaSession?,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory
    ): IntArray {
        var compactViewIndices = IntArray(3)
        val defaultCompactViewIndices = IntArray(3)
        Arrays.fill(compactViewIndices, C.INDEX_UNSET)
        Arrays.fill(defaultCompactViewIndices, C.INDEX_UNSET)
        var compactViewCommandCount = 0
        for (i in mediaButtons.indices) {
            val commandButton = mediaButtons[i]
            if (commandButton.sessionCommand != null) {
                builder.addAction(
                    actionFactory.createCustomActionFromCustomCommandButton(
                        mediaSession!!,
                        commandButton
                    )
                )
            } else {
                Assertions.checkState(commandButton.playerCommand != Player.COMMAND_INVALID)
                builder.addAction(
                    actionFactory.createMediaAction(
                        mediaSession!!,
                        IconCompat.createWithResource(context, commandButton.iconResId),
                        commandButton.displayName,
                        commandButton.playerCommand
                    )
                )
            }
            if (compactViewCommandCount == 3) {
                continue
            }
            val compactViewIndex =
                commandButton.extras.getInt(
                    DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX,  /* defaultValue= */
                    C.INDEX_UNSET
                )
            if (compactViewIndex >= 0 && compactViewIndex < compactViewIndices.size) {
                compactViewCommandCount++
                compactViewIndices[compactViewIndex] = i
            } else if (commandButton.playerCommand == COMMAND_SEEK_TO_PREVIOUS
                || commandButton.playerCommand == COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            ) {
                defaultCompactViewIndices[0] = i
            } else if (commandButton.playerCommand == Player.COMMAND_PLAY_PAUSE) {
                defaultCompactViewIndices[1] = i
            } else if (commandButton.playerCommand == COMMAND_SEEK_TO_NEXT
                || commandButton.playerCommand == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
            ) {
                defaultCompactViewIndices[2] = i
            }
        }
        if (compactViewCommandCount == 0) {
            // If there is no custom configuration we use the seekPrev (if any), play/pause (if any),
            // seekNext (if any) action in compact view.
            var indexInCompactViewIndices = 0
            for (i in defaultCompactViewIndices.indices) {
                if (defaultCompactViewIndices[i] == C.INDEX_UNSET) {
                    continue
                }
                compactViewIndices[indexInCompactViewIndices] = defaultCompactViewIndices[i]
                indexInCompactViewIndices++
            }
        }
        var i = 0
        while (i < compactViewIndices.size) {
            if (compactViewIndices[i] == C.INDEX_UNSET) {
                compactViewIndices = compactViewIndices.copyOf(i)
                break
            }
            i++
        }
        return compactViewIndices
    }

    private fun getMediaButtons(
        session: MediaSession?,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        // Skip to previous action.
        val commandButtons = ImmutableList.Builder<CommandButton>()
        if (playerCommands.containsAny(
                COMMAND_SEEK_TO_PREVIOUS,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            )
        ) {
            val commandButtonExtras = Bundle()
            commandButtonExtras.putInt(
                DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX,
                C.INDEX_UNSET
            )
            commandButtons.add(
                CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                    .setPlayerCommand(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .setExtras(commandButtonExtras)
                    .build()
            )
        }
        if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
            val commandButtonExtras = Bundle()
            commandButtonExtras.putInt(
                DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX,
                C.INDEX_UNSET
            )
            if (showPauseButton) {
                commandButtons.add(
                    CommandButton.Builder(CommandButton.ICON_PAUSE)
                        .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                        .setExtras(commandButtonExtras)
                        .build()
                )
            } else {
                commandButtons.add(
                    CommandButton.Builder(CommandButton.ICON_PLAY)
                        .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                        .setExtras(commandButtonExtras)
                        .build()
                )
            }
        }
        // Skip to next action.
        if (playerCommands.containsAny(COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) {
            val commandButtonExtras = Bundle()
            commandButtonExtras.putInt(
                DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX,
                C.INDEX_UNSET
            )
            commandButtons.add(
                CommandButton.Builder(CommandButton.ICON_NEXT)
                    .setPlayerCommand(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .setExtras(commandButtonExtras)
                    .build()
            )
        }
        for (i in customLayout.indices) {
            val button = customLayout[i]
            if (button.sessionCommand != null
                && button.sessionCommand!!.commandCode == SessionCommand.COMMAND_CODE_CUSTOM
            ) {
                commandButtons.add(button)
            }
        }
        return commandButtons.build()
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        // notification should be created before you return here
        return MediaNotification(DEFAULT_NOTIFICATION_ID,  createNotification(mediaSession, ImmutableList.of(), actionFactory))
    }

    @OptIn(UnstableApi::class)
    fun createNotification(
        session: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory
    ): Notification  {
        nBuilder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)

        val customLayoutWithEnabledCommandButtonsOnly =
            ImmutableList.Builder<CommandButton>()
        for (i in customLayout.indices) {
            val button: CommandButton = customLayout[i]
            if (button.sessionCommand != null && button.sessionCommand!!.commandCode == SessionCommand.COMMAND_CODE_CUSTOM && button.isEnabled) {
                customLayoutWithEnabledCommandButtonsOnly.add(customLayout[i])
            }
        }

        val compactViewIndices =
            addNotificationActions(
                session,
                getMediaButtons(
                    session,
                    session.player.availableCommands,
                    customLayoutWithEnabledCommandButtonsOnly.build(),
                    !Util.shouldShowPlayButton(
                        session.player, session.showPlayButtonIfPlaybackIsSuppressed
                    )
                ),
                nBuilder,
                actionFactory
            )
        if (session.player.isCommandAvailable(Player.COMMAND_GET_METADATA)) {
            val metadata: MediaMetadata = session.player.mediaMetadata
            nBuilder
                .setContentTitle(metadata.title)
                .setContentText(metadata.artist)
            val bitmapFuture: ListenableFuture<Bitmap>? =
                session.bitmapLoader.loadBitmapFromMetadata(metadata)
            if (bitmapFuture != null) {
                pendingOnBitmapLoadedFutureCallback?.discardIfPending()
                if (bitmapFuture.isDone) {
                    nBuilder.setLargeIcon(Futures.getDone<Bitmap>(bitmapFuture))
                }
            }
        }

        val playbackStartTimeMs = getPlaybackStartTimeEpochMs(session.player)
        val displayElapsedTimeWithChronometer = playbackStartTimeMs != C.TIME_UNSET

        nBuilder
            .setWhen(if (displayElapsedTimeWithChronometer) playbackStartTimeMs else 0L)
            .setShowWhen(displayElapsedTimeWithChronometer)
            .setUsesChronometer(displayElapsedTimeWithChronometer)

        if (Util.SDK_INT >= 31) {
            nBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val notification =  nBuilder

            /*
            .setContentIntent(mediaSession.getSessionActivity())
            .setDeleteIntent(
                actionFactory.createMediaActionPendingIntent(
                    mediaSession,
                    Player.COMMAND_STOP.toLong()
                )
            )
            */
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.play)
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowCancelButton(true)
                    .setShowActionsInCompactView(*compactViewIndices)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .build()
        return notification
    }


    private fun getPlaybackStartTimeEpochMs(player: Player): Long {
        // Changing "showWhen" causes notification flicker if SDK_INT < 21.
        return if ((Util.SDK_INT >= 21 && player.isPlaying
                    && !player.isPlayingAd
                    && !player.isCurrentMediaItemDynamic) && player.playbackParameters.speed == 1f
        ) {
            System.currentTimeMillis() - player.contentPosition
        } else {
            C.TIME_UNSET
        }
    }

    class OnBitmapLoadedFutureCallback(
        private val notificationId: Int,
        private val builder: NotificationCompat.Builder,
        private val onNotificationChangedCallback: MediaNotification.Provider.Callback
    ) : FutureCallback<Bitmap?> {
        private var discarded = false

        fun discardIfPending() {
            discarded = true
        }

        override fun onSuccess(result: Bitmap?) {
            if (!discarded) {
                builder.setLargeIcon(result)
                onNotificationChangedCallback.onNotificationChanged(
                    MediaNotification(notificationId, builder.build())
                )
            }
        }

        override fun onFailure(t: Throwable) {
            if (!discarded) {

            }
        }
    }
}