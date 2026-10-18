package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshots.Snapshot

/** One publisher and one reset per player discontinuity, never inferred from a slow display frame. */
internal class LyricsPlaybackState(initial: Long, initialEpoch: Int) {
    val position = mutableLongStateOf(clamp(initial))
    val motion = LyricsMotionState(clamp(initial).toInt())
    private var epoch = initialEpoch

    fun publish(value: Long, playbackEpoch: Int) {
        val now = clamp(value)
        if (playbackEpoch == epoch) { position.longValue = now; return }
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
