package com.yohandeku32.nusamusic.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
    private var expectedPrimaryMediaId: String? = null
    private var expectedNextMediaId: String? = null
    private var preloadedNextMediaId: String? = null
    private var handoffDone = false

    private val crossfadeTick = object : Runnable {
        override fun run() {
            updateCrossfade()
            crossfadeHandler.postDelayed(this, 40L)
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

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val transitionPlayer = ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this)
        )
            .setAudioAttributes(audioAttributes, false)
            .setLoadControl(loadControl)
            .build()

        primaryPlayer = player
        crossfadePlayer = transitionPlayer

        transitionPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("NusaCrossfade", "SECONDARY PLAYER ERROR", error)
                cancelCrossfade(restorePrimaryVolume = true)
            }
        })

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("NusaCrossfade", "PRIMARY PLAYER ERROR", error)
                cancelCrossfade(restorePrimaryVolume = true)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // A manual next/previous or an unexpected automatic transition
                // invalidates the in-flight crossfade. The normal transition
                // caused by our explicit handoff is already handled by
                // completeCrossfadeHandoff().
                if (crossfadeActive &&
                    !handoffDone &&
                    mediaItem?.mediaId != expectedPrimaryMediaId
                ) {
                    cancelCrossfade(restorePrimaryVolume = true)
                }

                if (!handoffDone &&
                    getSharedPreferences("playback_preferences", MODE_PRIVATE)
                        .getBoolean("crossfade_enabled", false)
                ) {
                    preloadNext(primaryPlayer, crossfadePlayer)
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
            if (crossfadeActive || preloadedNextMediaId != null) {
                cancelCrossfade(restorePrimaryVolume = true)
            }
            return
        }

        val currentItem = primary.currentMediaItem
        if (currentItem == null) {
            cancelCrossfade(restorePrimaryVolume = true)
            return
        }

        if (crossfadeActive) {
            // A user action such as pause/seek/next/previous cancels the
            // transition. Buffering does not, because playWhenReady remains true.
            if (!primary.playWhenReady) {
                cancelCrossfade(restorePrimaryVolume = true)
                return
            }

            if (currentItem.mediaId != expectedPrimaryMediaId) {
                cancelCrossfade(restorePrimaryVolume = true)
                return
            }

            val duration = primary.duration
            if (duration <= 0L || duration == androidx.media3.common.C.TIME_UNSET) {
                cancelCrossfade(restorePrimaryVolume = true)
                return
            }

            val remaining = duration - primary.currentPosition

            if (remaining <= 60L) {
                completeCrossfadeHandoff(primary, secondary)
            } else {
                applyCrossfadeVolumes(primary, secondary, durationMs)
            }

            return
        }

        if (!primary.isPlaying || !primary.playWhenReady) return
        if (primary.repeatMode == Player.REPEAT_MODE_ONE) return
        if (!primary.hasNextMediaItem()) return

        val duration = primary.duration
        if (duration <= 0L || duration == androidx.media3.common.C.TIME_UNSET) return

        val nextIndex = primary.nextMediaItemIndex
        if (nextIndex < 0 || nextIndex >= primary.mediaItemCount) return

        // Prepare the next item well before the actual fade window. This is
        // important for local FLAC/ALAC files where decoder setup may take
        // longer than the configured crossfade duration.
        preloadNext(primary, secondary)

        val remaining = duration - primary.currentPosition
        if (remaining in 1L..durationMs &&
            preloadedNextMediaId == primary.getMediaItemAt(nextIndex).mediaId &&
            secondary.playbackState == Player.STATE_READY
        ) {
            startCrossfade(primary, secondary, nextIndex, durationMs)
        }
    }

    private fun preloadNext(
        primary: ExoPlayer?,
        secondary: ExoPlayer?
    ) {
        if (primary == null || secondary == null) return

        if (!getSharedPreferences("playback_preferences", MODE_PRIVATE)
                .getBoolean("crossfade_enabled", false)
        ) {
            return
        }
        if (!primary.hasNextMediaItem()) return
        if (primary.repeatMode == Player.REPEAT_MODE_ONE) return

        val nextIndex = primary.nextMediaItemIndex
        if (nextIndex < 0 || nextIndex >= primary.mediaItemCount) return

        val nextItem = primary.getMediaItemAt(nextIndex)
        val currentSecondaryId = secondary.currentMediaItem?.mediaId

        if (currentSecondaryId == nextItem.mediaId &&
            secondary.playbackState != Player.STATE_IDLE
        ) {
            preloadedNextMediaId = nextItem.mediaId
            return
        }

        secondary.stop()
        secondary.clearMediaItems()
        secondary.setMediaItem(nextItem)
        secondary.volume = 0f
        secondary.prepare()
        preloadedNextMediaId = nextItem.mediaId

        Log.d(
            "NusaCrossfade",
            "PRELOAD next=" + nextItem.mediaId
        )
    }

    private fun startCrossfade(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        nextIndex: Int,
        durationMs: Long
    ) {
        if (crossfadeActive) return

        val currentItem = primary.currentMediaItem ?: return
        val nextItem = primary.getMediaItemAt(nextIndex)

        if (currentItem.mediaId == nextItem.mediaId) return

        expectedPrimaryMediaId = currentItem.mediaId
        expectedNextMediaId = nextItem.mediaId
        handoffDone = false

        if (secondary.currentMediaItem?.mediaId != nextItem.mediaId) {
            preloadNext(primary, secondary)
        }

        if (secondary.currentMediaItem?.mediaId != nextItem.mediaId ||
            secondary.playbackState != Player.STATE_READY
        ) {
            return
        }

        secondary.volume = 0f
        secondary.play()

        crossfadeActive = true
        Log.d(
            "NusaCrossfade",
            "START current=" + currentItem.mediaId +
                " next=" + nextItem.mediaId +
                " duration=" + durationMs + "ms"
        )
        applyCrossfadeVolumes(primary, secondary, durationMs)
    }

    private fun applyCrossfadeVolumes(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        durationMs: Long
    ) {
        if (!secondary.playWhenReady) return

        val duration = primary.duration
        if (duration <= 0L || duration == androidx.media3.common.C.TIME_UNSET) return

        val remaining = (duration - primary.currentPosition)
            .coerceIn(0L, durationMs)

        val progress = (
            1f - (remaining.toFloat() / durationMs.toFloat())
        ).coerceIn(0f, 1f)

        // Equal-power crossfade keeps perceived loudness more stable
        // during the overlap than a simple linear ramp.
        val fadeOut = kotlin.math.cos(
            progress * Math.PI.toDouble() / 2.0
        ).toFloat()
        val fadeIn = kotlin.math.sin(
            progress * Math.PI.toDouble() / 2.0
        ).toFloat()

        primary.volume = fadeOut.coerceIn(0f, 1f)
        secondary.volume = fadeIn.coerceIn(0f, 1f)
    }

    private fun completeCrossfadeHandoff(
        primary: ExoPlayer,
        secondary: ExoPlayer
    ) {
        if (!crossfadeActive || handoffDone) return

        val expectedNextId = expectedNextMediaId ?: return
        val secondaryItem = secondary.currentMediaItem ?: return
        if (secondaryItem.mediaId != expectedNextId) {
            cancelCrossfade(restorePrimaryVolume = true)
            return
        }

        if (secondary.playbackState != Player.STATE_READY &&
            secondary.playbackState != Player.STATE_BUFFERING
        ) {
            return
        }

        val nextIndex = primary.mediaItemCount.let { count ->
            (0 until count).firstOrNull {
                primary.getMediaItemAt(it).mediaId == expectedNextId
            }
        } ?: run {
            cancelCrossfade(restorePrimaryVolume = true)
            return
        }

        val nextPosition = secondary.currentPosition.coerceAtLeast(0L)

        handoffDone = true

        // Explicitly jump the session player onto the next item at the exact
        // position the transition player has reached. This avoids waiting for
        // ExoPlayer's automatic playlist transition event.
        primary.seekTo(nextIndex, nextPosition)
        primary.volume = 1f
        primary.play()

        secondary.volume = 0f
        secondary.pause()
        secondary.stop()
        secondary.clearMediaItems()

        Log.d(
            "NusaCrossfade",
            "HANDOFF next=" + expectedNextId +
                " position=" + nextPosition + "ms"
        )

        crossfadeActive = false
        expectedPrimaryMediaId = null
        expectedNextMediaId = null
        preloadedNextMediaId = null
        handoffDone = false
    }

    private fun cancelCrossfade(restorePrimaryVolume: Boolean) {
        Log.d("NusaCrossfade", "CANCEL")
        crossfadeActive = false
        expectedPrimaryMediaId = null
        expectedNextMediaId = null
        handoffDone = false

        crossfadePlayer?.let { secondary ->
            secondary.volume = 0f
            secondary.pause()
            secondary.stop()
            secondary.clearMediaItems()
        }

        if (restorePrimaryVolume) {
            primaryPlayer?.volume = 1f
        }
        preloadedNextMediaId = null
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
