package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import kotlin.math.roundToInt

@Composable
internal fun SyncedLyricsView(
    lyrics: SyncedLyrics, position: State<Long>, visible: Boolean,
    listState: LazyListState, motionState: LyricsMotionState,
    onSeek: (ISyncedLine) -> Unit, fontSize: Float, modifier: Modifier = Modifier
) {
    val clock = remember { LyricsPlaybackClock(position, visible) }
    SideEffect { clock.update(position, visible) }
    val timeline = remember(lyrics) { LyricsTimeline(lyrics.lines) }
    val frame by remember(timeline, clock) { derivedStateOf(structuralEqualityPolicy()) { timeline.frame(clock.read()) } }
    val dragging by listState.interactionSource.collectIsDraggedAsState()
    val motion = remember(listState) { LyricsFollowMotion() }
    var positioned by remember(listState) { mutableStateOf(false) }
    var previousRevision by remember { mutableIntStateOf(-1) }
    var browsing by remember { mutableStateOf(false) }
    LaunchedEffect(motionState.revision) { browsing = false }
    LaunchedEffect(dragging) {
        if (dragging) browsing = true else if (browsing) {
            snapshotFlow { listState.isScrollInProgress }.first { !it }
            delay(2500)
            browsing = false
        }
    }
    val normalStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = fontSize.sp, lineHeight = (fontSize * 1.25f).sp, fontWeight = FontWeight.Bold) }
    val backgroundStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = (fontSize * 0.625f).sp, lineHeight = (fontSize * 0.875f).sp, fontWeight = FontWeight.SemiBold) }
    BoxWithConstraints(modifier) {
        val leadDistance = with(androidx.compose.ui.platform.LocalDensity.current) { 48.dp.toPx() }
        LaunchedEffect(visible, browsing, dragging, fontSize, maxHeight, maxWidth, motionState.revision) {
            if (!visible || browsing || dragging) {
                motion.reset()
                if (!visible) positioned = false
                return@LaunchedEffect
            }
            if (previousRevision != motionState.revision) { positioned = false; previousRevision = motionState.revision }
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it == lyrics.lines.size }
            fun center() = with(listState.layoutInfo) { (viewportStartOffset + viewportEndOffset) / 2f }
            fun distance(target: Int, offset: Float): Float {
                val items = listState.layoutInfo.visibleItemsInfo
                val item = items.firstOrNull { it.index == target }
                val actualCenter = if (item != null) item.offset + item.size / 2f else {
                    val first = items.firstOrNull() ?: return 0f
                    val average = items.map { it.size }.average().toFloat().coerceAtLeast(1f)
                    first.offset + (target - first.index) * average + average / 2f
                }
                return actualCenter - center() - offset
            }
            var previousTime = clock.read()
            var lastFollowIndex = timeline.frame(previousTime).anchor
            snapshotFlow { clock.read() }.collect { now ->
                val current = timeline.frame(now)
                val target = current.gap?.nextLine
                    ?: current.anchor.takeIf { it in current.active }
                    ?: current.active.firstOrNull()
                    ?: if (positioned) lastFollowIndex else current.anchor
                lastFollowIndex = target
                if (target !in lyrics.lines.indices) return@collect
                val lead = current.gap?.let { countdownFollowOffset(it, now) * leadDistance } ?: 0f
                if (!positioned) {
                    motion.reset()
                    listState.scrollToItem(target, -(center() + lead).roundToInt())
                    withFrameNanos { }
                    listState.scrollBy(distance(target, lead))
                    positioned = true
                } else {
                    val elapsed = now.toLong() - previousTime
                    val error = distance(target, lead)
                    if (elapsed in 1..350 && abs(error) > 0.1f) {
                        val limit = (listState.layoutInfo.viewportSize.height / 6f).coerceAtLeast(1f)
                        val step = motion.step(error, elapsed).coerceIn(-limit, limit)
                        val consumed = listState.scrollBy(step)
                        if (abs(step - consumed) > 0.5f) motion.reset()
                    } else if (abs(error) <= 0.1f) motion.reset()
                }
                previousTime = now
            }
        }
        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize().lyricsEdgeFade(),
            contentPadding = PaddingValues(vertical = maxHeight / 2)
        ) {
            // Only lyric rows are measured: completed countdowns leave no blank list slots.
            itemsIndexed(lyrics.lines, key = { index, _ -> index }, contentType = { _, line -> if (line is KaraokeLine) "karaoke" else "line" }) { _, line ->
                if (line is KaraokeLine) CenteredKaraokeLine(line, clock.read, motionState,
                    if (line.isAccompaniment) backgroundStyle else normalStyle, onSeek)
                else LineSyncedLyricsRow(line as SyncedLine, clock.read, motionState, normalStyle) { onSeek(line) }
            }
        }
        frame.gap?.let { gap ->
            LyricsGapDots(gap, clock.read, Modifier.align(Alignment.CenterStart).padding(start = 16.dp))
        }
    }
}
