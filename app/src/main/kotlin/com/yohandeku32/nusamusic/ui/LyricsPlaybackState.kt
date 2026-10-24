package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Stable lyric playback cursor. Media3 may return a briefly *older* position
 * after a device-clock rebase or playback-speed correction. That must never
 * replay a syllable, visual-focus transition or scroll target.
 *
 * Only the explicit playback epoch may re-anchor backwards. A seek updates
 * that epoch in MainActivity; pause retains the last sampled position.
 */
internal class LyricsPlaybackState(initial: Long, initialEpoch: Int) {
    val position = mutableLongStateOf(clamp(initial))
    val motion = LyricsMotionState(clamp(initial).toInt())
    private var epoch = initialEpoch

    fun publish(value: Long, playbackEpoch: Int) {
        val now = clamp(value)
        if (playbackEpoch == epoch) {
            // All lyric consumers (fill, glow AND line follow) share this
            // monotonic position. A private painter-only jitter filter left
            // the list focus moving backwards even when its glow was stable.
            val previous = position.longValue
            if (now > previous) position.longValue = now
            return
        }
        Snapshot.withMutableSnapshot {
            if (playbackEpoch != epoch) {
                epoch = playbackEpoch
                motion.reset(now.toInt())
            }
            position.longValue = now
        }
    }
    private fun clamp(value: Long) = value.coerceIn(0, Int.MAX_VALUE.toLong())
}
