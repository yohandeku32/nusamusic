package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.WindowManager
import kotlinx.coroutines.launch
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
    dismissRequestId: Int,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    fontSize: Float
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var closing by remember { mutableStateOf(false) }
    val latestDismiss by rememberUpdatedState(onDismiss)
    var observedDismissRequest by remember { mutableIntStateOf(dismissRequestId) }

    fun requestDismiss() {
        if (closing) return
        closing = true
        scope.launch {
            try {
                // Drive the real anchored sheet (and Material3's scrim)
                // to Hidden before removing its content from composition.
                // Drag dismissals resume from the dragged offset/velocity.
                if (sheetState.isVisible) sheetState.hide()
            } finally {
                if (!sheetState.isVisible) latestDismiss()
                else closing = false
            }
        }
    }

    LaunchedEffect(dismissRequestId) {
        if (dismissRequestId != observedDismissRequest) {
            observedDismissRequest = dismissRequestId
            requestDismiss()
        }
    }

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
        onDismissRequest = ::requestDismiss,
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = backdrop,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.28f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        // Bottom sheets have a separate dialog window. The Activity already
        // hides only the status bar, but the dialog can reveal its own bar.
        // Keep that dialog fullscreen until the exit animation finishes.
        // Do not show the Activity bar on dismissal: Nusa keeps it hidden.
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
        val hideDialogStatusBar: () -> Unit = {
            dialogWindow?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
        DisposableEffect(dialogWindow) {
            hideDialogStatusBar()
            onDispose {
                // The dialog window is being destroyed after Hidden. Its
                // flags are scoped to that window, not the Now Playing UI.
            }
        }
        LaunchedEffect(dialogWindow) {
            // Reassert after the first dialog frame without polling or
            // mutating the system-window layout on every lyric draw.
            withFrameNanos { }
            hideDialogStatusBar()
        }

        Column(Modifier.fillMaxWidth().height(height).background(
            Brush.verticalGradient(listOf(backdrop, Color(0xFF101014))))
            .windowInsetsPadding(WindowInsets.navigationBars)) {
            Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            }
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lyrics", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = ::requestDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close lyrics",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
                            }, fontSize = fontSize, credits = data.stored.credits, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            inferredEnds = data.inferredEnds)
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
