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
import com.yohandeku32.nusamusic.data.SongCredits
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
    onSeek: (ISyncedLine) -> Unit, fontSize: Float, credits: SongCredits = SongCredits(), modifier: Modifier = Modifier
) {
    val clock = remember { LyricsPlaybackClock(position, visible) }
    SideEffect { clock.update(position, visible) }
    val display = remember(lyrics) { LyricsDisplayRows(lyrics.lines) }
    val mainHeights = remember(lyrics) { HashMap<Int, Int>() }
    val followEnd = remember(lyrics) { lyrics.lines.maxOfOrNull { it.end }?.toLong()?.plus(160) ?: 0L }
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
        val leadPadding = with(androidx.compose.ui.platform.LocalDensity.current) { 28.dp.toPx() }
        LaunchedEffect(visible, browsing, dragging, fontSize, maxHeight, maxWidth, motionState.revision) {
            if (!visible || browsing || dragging) {
                motion.reset()
                if (!visible) positioned = false
                return@LaunchedEffect
            }
            if (previousRevision != motionState.revision) { positioned = false; previousRevision = motionState.revision }
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it == display.rows.size + if (credits.isEmpty) 0 else 1 }
            fun center() = with(listState.layoutInfo) { (viewportStartOffset + viewportEndOffset) / 2f }
            fun distance(target: Int, offset: Float): Float {
                val items = listState.layoutInfo.visibleItemsInfo
                val item = items.firstOrNull { it.index == target }
                val actualCenter = if (item != null) item.offset + (mainHeights[target] ?: item.size) / 2f else {
                    val first = items.firstOrNull() ?: return 0f
                    val average = items.map { it.size }.average().toFloat().coerceAtLeast(1f)
                    first.offset + (target - first.index) * average + average / 2f
                }
                return actualCenter - center() - offset
            }
            fun leadFor(target: Int, gap: LyricsGap?, now: Int): Float {
                if (gap == null) return 0f
                val items = listState.layoutInfo.visibleItemsInfo
                val height = mainHeights[target]?.toFloat() ?: items.firstOrNull { it.index == target }?.size?.toFloat()
                    ?: items.map { it.size }.average().toFloat().takeIf { it.isFinite() } ?: leadPadding * 2f
                return (height / 2f + leadPadding) * countdownFollowOffset(gap, now)
            }
            var previousTime = clock.read()
            var lastFollowIndex = timeline.frame(previousTime).anchor
            snapshotFlow { clock.read() }.collect { now ->
                val current = timeline.frame(now)
                val sourceTarget = current.gap?.nextLine
                    ?: current.anchor.takeIf { it in current.active }
                    ?: current.active.firstOrNull()
                    ?: if (positioned) lastFollowIndex else current.anchor
                lastFollowIndex = sourceTarget
                if (sourceTarget !in lyrics.lines.indices) return@collect
                if (positioned && now.toLong() >= followEnd) return@collect
                val target = display.lineToRow[sourceTarget]
                val lead = leadFor(target, current.gap, now)
                if (!positioned) {
                    motion.reset()
                    listState.scrollToItem(target, -(center() + lead).roundToInt())
                    withFrameNanos { }
                    listState.scrollBy(distance(target, leadFor(target, current.gap, now)))
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
            contentPadding = PaddingValues(top = maxHeight / 2, bottom = if (credits.isEmpty) maxHeight / 2 else 24.dp)
        ) {
            itemsIndexed(display.rows, key = { _, row -> row.sourceIndex }, contentType = { _, _ -> "lyric-group" }) { index, row ->
                Column(Modifier.fillMaxWidth()) {
                    val main = row.main
                    if (main is KaraokeLine) CenteredKaraokeLine(main, clock.read, motionState,
                        normalStyle, onSeek, onMainHeight = { mainHeights[index] = it })
                    else if (main is SyncedLine) LineSyncedLyricsRow(main, clock.read, motionState,
                        normalStyle, onMainHeight = { mainHeights[index] = it }) { onSeek(main) }
                    row.backing.forEach { backing ->
                        key(backing) {
                            val voices = remember(backing) { LyricsVocalWindows(backing) }
                            TimedVocalContent(voices, clock.read, motionState) {
                                CenteredKaraokeLine(backing, clock.read, motionState, backgroundStyle, onSeek)
                            }
                        }
                    }
                }
            }
            if (!credits.isEmpty) item(key = "credits", contentType = "credits") {
                LyricsCredits(credits, Modifier.padding(horizontal = 16.dp))
            }
        }
        frame.gap?.let { gap ->
            LyricsGapDots(gap, clock.read, Modifier.align(Alignment.CenterStart).offset(y = (-28).dp).padding(start = 16.dp))
        }
    }
}
