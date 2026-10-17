package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/** Keep Accompanist's word renderer; own lazy composition and scroll following. */
@Composable
internal fun SyncedLyricsView(
    lyrics: SyncedLyrics,
    position: State<Long>,
    visible: Boolean,
    listState: LazyListState,
    onSeek: (ISyncedLine) -> Unit,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    val clock = remember { LyricsPlaybackClock(position, visible) }
    SideEffect { clock.update(position, visible) }
    val timeline = remember(lyrics) { LyricsTimeline(lyrics.lines) }
    // List composition changes only when focus changes; word drawing reads the live clock.
    val frame by remember(timeline, clock) { derivedStateOf(structuralEqualityPolicy()) { timeline.frame(clock.read()) } }
    val dragging by listState.interactionSource.collectIsDraggedAsState()
    var browsing by remember { mutableStateOf(false) }
    LaunchedEffect(dragging) {
        if (dragging) browsing = true else if (browsing) {
            snapshotFlow { listState.isScrollInProgress }.first { !it }
            delay(2_500)
            browsing = false
        }
    }
    LaunchedEffect(frame.anchor, visible, browsing, fontSize) {
        if (!visible || browsing || frame.anchor < 0) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > frame.anchor }
        // Allow resized rows to finish measurement before choosing the scroll distance.
        withFrameNanos { }
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == frame.anchor }
        // Do not skip a new target just because our previous animation was running.
        // LaunchedEffect cancels that animation and follows the newest line instead.
        if (target == null) listState.scrollToItem(frame.anchor)
        else if (abs(target.offset) > 2) listState.animateScrollBy(target.offset.toFloat(), tween(240))
    }
    val normalStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = fontSize.sp, lineHeight = (fontSize * 1.25f).sp, fontWeight = FontWeight.Bold) }
    val backgroundStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = (fontSize * 0.625f).sp, lineHeight = (fontSize * 0.875f).sp, fontWeight = FontWeight.SemiBold) }
    BoxWithConstraints(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 48.dp, bottom = (maxHeight - 48.dp).coerceAtLeast(48.dp))
        ) {
            itemsIndexed(lyrics.lines, key = { index, _ -> index }, contentType = { _, line -> if (line is KaraokeLine) "karaoke" else "line" }) { index, line ->
                when (line) {
                    is KaraokeLine -> CenteredKaraokeLine(
                        line = line,
                        onSeek = onSeek,
                        time = clock.read,
                        focused = index in frame.active,
                        style = if (line.isAccompaniment) backgroundStyle else normalStyle
                    )
                    is SyncedLine -> CenteredSyncedLine(
                        line = line,
                        focused = index in frame.active,
                        onSeek = onSeek,
                        style = normalStyle
                    )
                }
            }
        }
    }
}
