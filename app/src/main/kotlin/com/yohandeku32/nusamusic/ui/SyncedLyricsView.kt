package com.yohandeku32.nusamusic.ui

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
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

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
    val motion = remember(listState) { LyricsFollowMotion() }
    var positioned by remember(listState) { mutableStateOf(false) }
    var browsing by remember { mutableStateOf(false) }
    LaunchedEffect(dragging) {
        if (dragging) browsing = true else if (browsing) {
            snapshotFlow { listState.isScrollInProgress }.first { !it }
            delay(2_500)
            browsing = false
        }
    }
    val normalStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = fontSize.sp, lineHeight = (fontSize * 1.25f).sp, fontWeight = FontWeight.Bold) }
    val backgroundStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = (fontSize * 0.625f).sp, lineHeight = (fontSize * 0.875f).sp, fontWeight = FontWeight.SemiBold) }
    BoxWithConstraints(modifier) {
        LaunchedEffect(frame.anchor, visible, browsing, fontSize, maxHeight, maxWidth) {
            if (!visible || browsing || dragging || frame.anchor < 0) {
                motion.reset()
                if (!visible) positioned = false
                return@LaunchedEffect
            }
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > frame.anchor }
            // Wait for the current font size and viewport padding to be measured.
            withFrameNanos { }
            fun viewportCenter(): Float = with(listState.layoutInfo) {
                (viewportStartOffset + viewportEndOffset) / 2f
            }
            if (!positioned) {
                motion.reset()
                listState.scrollToItem(frame.anchor, -viewportCenter().roundToInt())
            } else if (listState.layoutInfo.visibleItemsInfo.none { it.index == frame.anchor }) {
                // A seek or a return from manual browsing may be far away. Animate there,
                // then refine the center using the real wrapped row height.
                motion.reset()
                listState.animateScrollToItem(frame.anchor, -viewportCenter().roundToInt())
            }
            val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == frame.anchor }
                ?: return@LaunchedEffect
            val distance = target.offset + target.size / 2f - viewportCenter()
            if (!positioned) listState.scroll { scrollBy(distance) }
            else if (abs(distance) > 1f) motion.move(listState, distance)
            positioned = true
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().lyricsEdgeFade(),
            // First and last rows must also have enough room to reach the center.
            contentPadding = PaddingValues(vertical = maxHeight / 2)
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
