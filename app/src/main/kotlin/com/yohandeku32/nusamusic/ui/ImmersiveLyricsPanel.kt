package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive

/**
 * In-place Immersive lyric viewport. This is the existing Lyrics engine
 * (same state, Media3 audio clock, follow-scroll, timing, taps, backing
 * vocals, credits and cached text) without a separate modal window.
 *
 * Its parent owns the size and entrance/exit motion; no anchored bottom
 * sheet or modal scrim can cover the Now Playing playback controls.
 */
@Composable
fun ImmersiveLyricsPanel(
    song: Song?,
    positionMs: State<Long>,
    readPlaybackPosition: () -> Long,
    readPlaybackEpoch: () -> Int,
    isPlaying: Boolean,
    playbackEpoch: Int,
    durationMs: Long,
    state: ImmersiveLyricsState,
    onSeek: (Long) -> Unit,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val entry = song?.let { state.forSong(it) }
    LaunchedEffect(song) { if (song != null) state.load(song, scope) }

    // Retain the original frame-clock driver. Pause freezes all renderers,
    // while Media3 seeks update the epoch and re-anchor lyric follow motion.
    val playback = remember(song?.uri) {
        LyricsPlaybackState(readPlaybackPosition(), readPlaybackEpoch())
    }
    val framePosition = playback.position
    val motion = playback.motion
    val readPosition by rememberUpdatedState(readPlaybackPosition)
    val readEpoch by rememberUpdatedState(readPlaybackEpoch)
    val playing by rememberUpdatedState(isPlaying)
    val epoch by rememberUpdatedState(playbackEpoch)
    val positionSource by rememberUpdatedState(positionMs)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(song?.uri, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow { playing to epoch }.collectLatest { (running, _) ->
                playback.publish(readPosition(), readEpoch())
                if (running) while (isActive) withFrameNanos {
                    playback.publish(readPosition(), readEpoch())
                } else snapshotFlow { positionSource.value }.collect {
                    playback.publish(readPosition(), readEpoch())
                }
            }
        }
    }

    Box(modifier) {
        val data = entry?.data
        when {
            entry != null && (entry.loading || !entry.attempted) && data == null ->
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            data?.synced != null -> key(song?.uri, data.synced) {
                SyncedLyricsView(
                    data.synced,
                    framePosition,
                    true,
                    requireNotNull(entry).list,
                    motion,
                    onSeek = {
                        val target = it.start.toLong()
                            .coerceIn(0, maxOf(durationMs, song?.durationMs ?: 0))
                        onSeek(target)
                    },
                    fontSize = fontSize,
                    credits = data.stored.credits,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    inferredEnds = data.inferredEnds
                )
            }

            data?.stored?.plain == true -> {
                val lines = remember(data) { data.stored.content.lines() }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    items(lines) { line ->
                        Text(
                            line,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.25f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!data.stored.credits.isEmpty) item {
                        LyricsCredits(data.stored.credits)
                    }
                }
            }

            else -> Text(
                "No embedded lyrics available",
                Modifier.align(Alignment.Center).padding(28.dp),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}
