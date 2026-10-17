package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

private data class LyricsDisplayRow(val line: Int, val gap: LyricsGap? = null)

/** Compose centered syllable rows lazily and follow the active line or countdown. */
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
    val drawTime = rememberSyllableClock(clock, visible)
    val timeline = remember(lyrics) { LyricsTimeline(lyrics.lines) }
    val rows = remember(timeline) {
        val gaps = timeline.gaps.associateBy { it.nextLine }
        buildList {
            lyrics.lines.indices.forEach { index ->
                gaps[index]?.let { add(LyricsDisplayRow(index, it)) }
                add(LyricsDisplayRow(index))
            }
        }
    }
    val lineRows = remember(rows) { rows.indices.filter { rows[it].gap == null }.toIntArray() }
    val gapRows = remember(rows) { rows.indices.filter { rows[it].gap != null }.associateBy { rows[it].gap } }
    // List composition changes only when focus changes; word drawing reads the live clock.
    val frame by remember(timeline, clock) { derivedStateOf(structuralEqualityPolicy()) { timeline.frame(clock.read()) } }
    val followRow = frame.gap?.let { gapRows[it] } ?: lineRows.getOrNull(frame.anchor) ?: -1
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
        LaunchedEffect(followRow, visible, browsing, fontSize, maxHeight, maxWidth) {
            if (!visible || browsing || dragging || followRow < 0) {
                motion.reset()
                if (!visible) positioned = false
                return@LaunchedEffect
            }
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > followRow }
            // Wait for the current font size and viewport padding to be measured.
            withFrameNanos { }
            fun viewportCenter(): Float = with(listState.layoutInfo) {
                (viewportStartOffset + viewportEndOffset) / 2f
            }
            if (!positioned) {
                motion.reset()
                listState.scrollToItem(followRow, -viewportCenter().roundToInt())
            } else if (listState.layoutInfo.visibleItemsInfo.none { it.index == followRow }) {
                // A seek or a return from manual browsing may be far away. Animate there,
                // then refine the center using the real wrapped row height.
                motion.reset()
                listState.animateScrollToItem(followRow, -viewportCenter().roundToInt())
            }
            val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == followRow }
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
            itemsIndexed(rows, key = { _, row -> "${if (row.gap == null) "line" else "gap"}:${row.line}" },
                contentType = { _, row -> if (row.gap != null) "gap" else if (lyrics.lines[row.line] is KaraokeLine) "karaoke" else "line" }) { _, row ->
                if (row.gap != null) {
                    LyricsGapDots(row.gap, clock.read, active = frame.gap == row.gap)
                    return@itemsIndexed
                }
                val index = row.line
                val original = lyrics.lines[index]
                val line = remember(original) { SyllableTiming.forDisplay(original) }
                CenteredKaraokeLine(
                    line = line,
                    onSeek = { onSeek(original) },
                    time = drawTime,
                    focused = index in frame.active,
                    style = if (line.isAccompaniment) backgroundStyle else normalStyle
                )
            }
        }
    }
}

/** Interpolate playback samples in drawing, while seeks and hidden pages update immediately. */
@Composable
private fun rememberSyllableClock(clock: LyricsPlaybackClock, visible: Boolean): () -> Int {
    val animated = remember(clock) { Animatable(clock.read(), Int.VectorConverter) }
    LaunchedEffect(clock, visible) {
        snapshotFlow { clock.read() }.collectLatest { target ->
            if (!visible || target < animated.value || abs(target.toLong() - animated.value) > 250) animated.snapTo(target)
            else if (target != animated.value) animated.animateTo(target, tween(50, easing = LinearEasing))
        }
    }
    return remember(animated) { { animated.value } }
}
