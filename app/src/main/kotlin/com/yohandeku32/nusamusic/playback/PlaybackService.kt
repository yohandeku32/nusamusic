package com.yohandeku32.nusamusic.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.yohandeku32.nusamusic.MainActivity

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var primaryPlayer: ExoPlayer? = null
    private var crossfadePlayer: ExoPlayer? = null

    private val crossfadeHandler = Handler(Looper.getMainLooper())
    private var crossfadeActive = false
    private var expectedNextMediaId: String? = null

    private val crossfadeTick = object : Runnable {
        override fun run() {
            updateCrossfade()
            crossfadeHandler.postDelayed(this, 50L)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,
                15_000,
                2_000,
                3_000
            )
            .build()

        val player = ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this)
        )
            .setLoadControl(loadControl)
            .build()

        val transitionPlayer = ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this)
        )
            .setLoadControl(loadControl)
            .build()

        primaryPlayer = player
        crossfadePlayer = transitionPlayer

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                handlePrimaryTransition(mediaItem, reason)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && crossfadeActive) {
                    cancelCrossfade(restorePrimaryVolume = true)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && crossfadeActive) {
                    finishCrossfadeAtEnd()
                }
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this,
            1001,
            Intent(this, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        crossfadeHandler.post(crossfadeTick)
    }

    private fun updateCrossfade() {
        val primary = primaryPlayer ?: return
        val secondary = crossfadePlayer ?: return

        val prefs = getSharedPreferences("playback_preferences", MODE_PRIVATE)
        val enabled = prefs.getBoolean("crossfade_enabled", false)
        val durationMs = prefs.getLong("crossfade_duration_ms", 5_000L)
            .coerceIn(1_000L, 12_000L)

        if (!enabled) {
            if (crossfadeActive) {
                cancelCrossfade(restorePrimaryVolume = true)
            }
            return
        }

        if (!primary.isPlaying || crossfadeActive) {
            if (crossfadeActive && primary.currentMediaItem == null) {
                cancelCrossfade(restorePrimaryVolume = true)
            } else if (crossfadeActive) {
                applyCrossfadeVolumes(primary, secondary, durationMs)
            }
            return
        }

        if (primary.repeatMode == Player.REPEAT_MODE_ONE) return

        val duration = primary.duration
        if (duration <= 0L) return

        val nextIndex = primary.nextMediaItemIndex
        if (nextIndex < 0 || nextIndex >= primary.mediaItemCount) return

        val remaining = duration - primary.currentPosition
        if (remaining <= durationMs) {
            startCrossfade(primary, secondary, nextIndex, durationMs)
        }
    }

    private fun startCrossfade(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        nextIndex: Int,
        durationMs: Long
    ) {
        if (crossfadeActive) return

        val nextItem = primary.getMediaItemAt(nextIndex)
        expectedNextMediaId = nextItem.mediaId

        secondary.stop()
        secondary.clearMediaItems()
        secondary.setMediaItem(nextItem)
        secondary.volume = 0f
        secondary.prepare()
        secondary.play()

        crossfadeActive = true
        applyCrossfadeVolumes(primary, secondary, durationMs)
    }

    private fun applyCrossfadeVolumes(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        durationMs: Long
    ) {
        val duration = primary.duration
        if (duration <= 0L) return

        val remaining = (duration - primary.currentPosition)
            .coerceIn(0L, durationMs)
        val progress = 1f - (remaining.toFloat() / durationMs.toFloat())
        val fadeIn = progress.coerceIn(0f, 1f)
        val fadeOut = 1f - fadeIn

        primary.volume = fadeOut
        secondary.volume = fadeIn
    }

    private fun handlePrimaryTransition(mediaItem: MediaItem?, reason: Int) {
        if (!crossfadeActive) return

        val expectedId = expectedNextMediaId
        if (expectedId != null && expectedId == mediaItem?.mediaId) {
            val primary = primaryPlayer ?: return
            val secondary = crossfadePlayer ?: return

            if (secondary.playbackState == Player.STATE_READY ||
                secondary.playbackState == Player.STATE_BUFFERING
            ) {
                val handoffPosition = secondary.currentPosition.coerceAtLeast(0L)

                primary.volume = 0f
                primary.seekTo(handoffPosition)
                primary.volume = 1f
                primary.play()

                secondary.volume = 0f
                secondary.pause()
                secondary.stop()
                secondary.clearMediaItems()
            }

            crossfadeActive = false
            expectedNextMediaId = null
            primary.volume = 1f
        } else {
            cancelCrossfade(restorePrimaryVolume = true)
        }

        @Suppress("UNUSED_PARAMETER")
        val ignoredReason = reason
    }

    private fun finishCrossfadeAtEnd() {
        val primary = primaryPlayer ?: return
        val secondary = crossfadePlayer ?: return

        if (!crossfadeActive) return

        if (secondary.playbackState == Player.STATE_READY ||
            secondary.playbackState == Player.STATE_BUFFERING
        ) {
            val handoffPosition = secondary.currentPosition.coerceAtLeast(0L)
            primary.seekTo(handoffPosition)
            primary.volume = 1f
            primary.play()

            secondary.volume = 0f
            secondary.pause()
            secondary.stop()
            secondary.clearMediaItems()
        }

        crossfadeActive = false
        expectedNextMediaId = null
        primary.volume = 1f
    }

    private fun cancelCrossfade(restorePrimaryVolume: Boolean) {
        crossfadeActive = false
        expectedNextMediaId = null

        crossfadePlayer?.let { secondary ->
            secondary.volume = 0f
            secondary.pause()
            secondary.stop()
            secondary.clearMediaItems()
        }

        if (restorePrimaryVolume) {
            primaryPlayer?.volume = 1f
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        crossfadeHandler.removeCallbacksAndMessages(null)

        cancelCrossfade(restorePrimaryVolume = true)

        primaryPlayer?.release()
        primaryPlayer = null

        crossfadePlayer?.release()
        crossfadePlayer = null

        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }
}
