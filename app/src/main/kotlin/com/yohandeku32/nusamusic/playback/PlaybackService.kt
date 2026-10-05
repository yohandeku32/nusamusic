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
import androidx.media3.exoplayer.source.ShuffleOrder
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

        primaryPlayer = player

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

        val prefs = getSharedPreferences("playback_preferences", MODE_PRIVATE)
        val enabled = prefs.getBoolean("crossfade_enabled", false)
        val durationMs = prefs.getLong("crossfade_duration_ms", 5_000L)
            .coerceIn(1_000L, 12_000L)

        if (!enabled) {
            if (crossfadeActive || crossfadePlayer != null) {
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
            val secondary = crossfadePlayer ?: run {
                cancelCrossfade(restorePrimaryVolume = true)
                return
            }

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

            if (remaining <= 250L) {
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

        val remaining = duration - primary.currentPosition
        if (remaining in 1L..durationMs) {
            startCrossfade(primary, nextIndex, durationMs)
        }
    }

    private fun ensureCrossfadePlayer(): ExoPlayer {
        crossfadePlayer?.let { return it }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        return ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this)
        )
            // The secondary player must never compete for audio focus.
            .setAudioAttributes(audioAttributes, false)
            .build()
            .also { secondary ->
                secondary.volume = 0f
                secondary.addListener(object : Player.Listener {
                    override fun onPlayerError(
                        error: androidx.media3.common.PlaybackException
                    ) {
                        Log.e(
                            "NusaCrossfade",
                            "SECONDARY PLAYER ERROR",
                            error
                        )
                        cancelCrossfade(restorePrimaryVolume = true)
                    }
                })
                crossfadePlayer = secondary
            }
    }

    private fun startCrossfade(
        primary: ExoPlayer,
        nextIndex: Int,
        durationMs: Long
    ) {
        if (crossfadeActive) return

        val secondary = ensureCrossfadePlayer()

        val currentItem = primary.currentMediaItem ?: return
        val nextItem = primary.getMediaItemAt(nextIndex)

        if (currentItem.mediaId == nextItem.mediaId) return

        expectedPrimaryMediaId = currentItem.mediaId
        expectedNextMediaId = nextItem.mediaId
        handoffDone = false

        secondary.stop()
        secondary.clearMediaItems()
        secondary.setMediaItem(nextItem)
        secondary.volume = 0f
        secondary.prepare()
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
        active: ExoPlayer,
        fadingIn: ExoPlayer
    ) {
        if (!crossfadeActive || handoffDone) return

        val expectedNextId = expectedNextMediaId ?: return
        val nextItem = fadingIn.currentMediaItem ?: return

        if (nextItem.mediaId != expectedNextId) {
            cancelCrossfade(restorePrimaryVolume = true)
            return
        }

        if (fadingIn.playbackState != Player.STATE_READY &&
            fadingIn.playbackState != Player.STATE_BUFFERING
        ) {
            return
        }

        val nextIndexInActive = (0 until active.mediaItemCount)
            .firstOrNull {
                active.getMediaItemAt(it).mediaId == expectedNextId
            } ?: run {
            cancelCrossfade(restorePrimaryVolume = true)
            return
        }

        // The transition player is already producing the next track's audio.
        // Before swapping the session player, restore the rest of the original
        // playlist onto it so Next/Previous/Repeat continue to work.
        restorePlaylistOnFadingPlayer(
            sourcePlayer = active,
            targetPlayer = fadingIn,
            currentIndex = nextIndexInActive
        )

        handoffDone = true

        mediaSession?.setPlayer(fadingIn)

        fadingIn.volume = 1f
        fadingIn.play()

        active.volume = 0f
        active.pause()
        active.stop()
        active.clearMediaItems()

        primaryPlayer = fadingIn
        crossfadePlayer = active

        Log.d(
            "NusaCrossfade",
            "SEAMLESS HANDOFF next=" + expectedNextId +
                " position=" + fadingIn.currentPosition + "ms"
        )

        crossfadeActive = false
        expectedPrimaryMediaId = null
        expectedNextMediaId = null
        handoffDone = false
    }

    private fun restorePlaylistOnFadingPlayer(
        sourcePlayer: ExoPlayer,
        targetPlayer: ExoPlayer,
        currentIndex: Int
    ) {
        if (sourcePlayer.mediaItemCount <= 1) {
            targetPlayer.repeatMode = sourcePlayer.repeatMode
            targetPlayer.shuffleModeEnabled = sourcePlayer.shuffleModeEnabled
            return
        }

        val currentId = sourcePlayer.getMediaItemAt(currentIndex).mediaId

        val remainingItems = (0 until sourcePlayer.mediaItemCount)
            .filter { it != currentIndex }
            .map { sourcePlayer.getMediaItemAt(it) }

        targetPlayer.addMediaItems(remainingItems)

        // Preserve the exact playback order that was active before the
        // crossfade. This matters when shuffle mode is enabled.
        if (sourcePlayer.shuffleModeEnabled) {
            val newIndexByMediaId = buildMap {
                put(currentId, 0)
                var newIndex = 1
                for (index in 0 until sourcePlayer.mediaItemCount) {
                    if (index == currentIndex) continue
                    put(sourcePlayer.getMediaItemAt(index).mediaId, newIndex)
                    newIndex++
                }
            }

            val shuffledIndices = mutableListOf<Int>()
            var originalIndex = currentIndex
            val visited = mutableSetOf<Int>()

            while (originalIndex != androidx.media3.common.C.INDEX_UNSET &&
                visited.add(originalIndex)
            ) {
                val mediaId = sourcePlayer.getMediaItemAt(originalIndex).mediaId
                newIndexByMediaId[mediaId]?.let { shuffledIndices.add(it) }

                originalIndex = sourcePlayer
                    .getShuffleOrder()
                    .getNextIndex(originalIndex)
            }

            if (shuffledIndices.size < targetPlayer.mediaItemCount) {
                for (index in 0 until targetPlayer.mediaItemCount) {
                    if (index !in shuffledIndices) {
                        shuffledIndices.add(index)
                    }
                }
            }

            targetPlayer.setShuffleOrder(
                ShuffleOrder.DefaultShuffleOrder(
                    shuffledIndices.toIntArray(),
                    System.nanoTime()
                )
            )
            targetPlayer.shuffleModeEnabled = true
        } else {
            targetPlayer.shuffleModeEnabled = false
        }

        targetPlayer.repeatMode = sourcePlayer.repeatMode

        Log.d(
            "NusaCrossfade",
            "RESTORE QUEUE count=" + targetPlayer.mediaItemCount +
                " shuffle=" + sourcePlayer.shuffleModeEnabled +
                " repeat=" + sourcePlayer.repeatMode
        )
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
