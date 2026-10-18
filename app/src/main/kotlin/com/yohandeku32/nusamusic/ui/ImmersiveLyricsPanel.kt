package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yohandeku32.nusamusic.data.ArtworkLoader
import com.yohandeku32.nusamusic.data.ImmersiveArtworkCache
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/** Fixed half-window modal; Expanded is its sole half-height anchor, never fullscreen. */
@OptIn(ExperimentalMaterial3Api::class)
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
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    fontSize: Float
) {
    val scope = rememberCoroutineScope()
    val entry = song?.let { state.forSong(it) }
    LaunchedEffect(song) { if (song != null) state.load(song, scope) }
    val context = LocalContext.current
    var backdrop by remember(song?.uri) {
        mutableStateOf(song?.let { ImmersiveArtworkCache.cached(it)?.backdrop }?.let { Color(it) } ?: Color(0xFF24242A))
    }
    LaunchedEffect(song?.uri) {
        if (song == null || ImmersiveArtworkCache.cached(song) != null) return@LaunchedEffect
        val bitmap = ArtworkLoader.load(context, song.uri, 128, song.albumId.takeIf { it > 0 }?.toString())
        if (bitmap != null) backdrop = withContext(Dispatchers.Default) {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            Color(dominantArtworkBackdrop(pixels))
        }
    }
    // MediaController computes the actual position using the current speed. Reading it on
    // the display clock needs no network, metadata IO, or whole-list recomposition.
    val playback = remember(song?.uri) { LyricsPlaybackState(readPlaybackPosition(), readPlaybackEpoch()) }
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
            // collectLatest cancels the old pump before a pause/play or discontinuity starts the next.
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
    val height = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() / 2 }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = Dp.Unspecified,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = backdrop,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.28f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(Modifier.fillMaxWidth().height(height).background(
            Brush.verticalGradient(listOf(backdrop, Color(0xFF101014))))
            .windowInsetsPadding(WindowInsets.navigationBars)) {
            Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            }
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lyrics", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close lyrics", tint = Color.White) }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                val data = entry?.data
                when {
                    entry != null && (entry.loading || !entry.attempted) && data == null -> CircularProgressIndicator(
                        Modifier.align(Alignment.Center).size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    data?.synced != null -> key(song?.uri, data.synced) {
                        SyncedLyricsView(data.synced, framePosition, true, requireNotNull(entry).list, motion,
                            onSeek = {
                                val target = it.start.toLong().coerceIn(0, maxOf(durationMs, song?.durationMs ?: 0))
                                onSeek(target)
                            }, fontSize = fontSize, credits = data.stored.credits, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp))
                    }
                    data?.stored?.plain == true -> {
                        val lines = remember(data) { data.stored.content.lines() }
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp)) {
                            items(lines) { line -> Text(line, color = Color.White.copy(alpha = 0.85f),
                                fontSize = fontSize.sp, lineHeight = (fontSize * 1.25f).sp, fontWeight = FontWeight.Bold) }
                            if (!data.stored.credits.isEmpty) item { LyricsCredits(data.stored.credits) }
                        }
                    }
                    else -> Text("No embedded lyrics available", Modifier.align(Alignment.Center).padding(28.dp),
                        color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }
        }
    }
}
