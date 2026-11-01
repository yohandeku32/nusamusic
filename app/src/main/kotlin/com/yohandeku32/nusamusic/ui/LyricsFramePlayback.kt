package com.yohandeku32.nusamusic.ui

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive

/** The full and mini surfaces sample the same Media3 clock and explicit seek epoch. */
@Composable
internal fun rememberLyricsFramePlayback(
    songKey: String?, position: State<Long>, readPosition: () -> Long, readEpoch: () -> Int,
    isPlaying: Boolean, playbackEpoch: Int
): LyricsPlaybackState {
    val playback = remember(songKey) { LyricsPlaybackState(readPosition(), readEpoch()) }
    val latestPosition by rememberUpdatedState(readPosition)
    val latestEpoch by rememberUpdatedState(readEpoch)
    val source by rememberUpdatedState(position)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(songKey, owner, isPlaying, playbackEpoch) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            playback.publish(latestPosition(), latestEpoch())
            if (isPlaying) while (isActive) withFrameNanos {
                playback.publish(latestPosition(), latestEpoch())
            } else snapshotFlow { source.value }.collect {
                playback.publish(latestPosition(), latestEpoch())
            }
        }
    }
    return playback
}
