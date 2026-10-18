package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
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
    var positioned by remember(listState) { mutableStateOf(false) }
    val interactionScope = rememberCoroutineScope()
    val interaction = remember(listState) { LyricsInteractionState() }
    LaunchedEffect(motionState.revision) { if (!listState.isScrollInProgress) interaction.seek() }
    LaunchedEffect(dragging) { interaction.drag(dragging) }
    LaunchedEffect(interaction.touching, dragging, interaction.generation, interaction.browsing) {
        if (!interaction.touching && !dragging && interaction.browsing) {
            val token = interaction.generation
            snapshotFlow { listState.isScrollInProgress }.first { !it }
            // Existing browsing grace period; timing/highlight never waits for this timer.
            delay(2500)
            interaction.resume(token)
        }
    }
    val seekLine = remember(onSeek, interaction) { { line: ISyncedLine -> interaction.seek(); onSeek(line) } }
    val normalStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = fontSize.sp, lineHeight = (fontSize * 1.25f).sp, fontWeight = FontWeight.Bold) }
    val backgroundStyle = remember(fontSize) { TextStyle(color = Color.White, fontSize = (fontSize * 0.625f).sp, lineHeight = (fontSize * 0.875f).sp, fontWeight = FontWeight.SemiBold) }
    BoxWithConstraints(modifier) {
        val leadPadding = with(androidx.compose.ui.platform.LocalDensity.current) { 28.dp.toPx() }
        LaunchedEffect(visible, interaction.canFollow, fontSize, maxHeight, maxWidth, motionState.revision) {
            val motion = LyricsFollowMotion()
            val interactionGeneration = interaction.generation
            if (!visible || !interaction.canFollow) return@LaunchedEffect
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it == display.rows.size + if (credits.isEmpty) 0 else 1 }
            fun center() = with(listState.layoutInfo) { (viewportStartOffset + viewportEndOffset) / 2f }
            fun distance(target: Int, offset: Float): Float {
                val items = listState.layoutInfo.visibleItemsInfo
                val item = items.firstOrNull { it.index == target }
                val actualCenter = if (item != null) item.offset + (mainHeights[target] ?: item.size) / 2f else {
                    val first = items.firstOrNull() ?: return 0f
                    var total = 0
                    var count = 0
                    for (visibleItem in items) if (visibleItem.index < display.rows.size) { total += visibleItem.size; count++ }
                    val average = if (count > 0) (total.toFloat() / count).coerceAtLeast(1f) else leadPadding * 2f
                    first.offset + (target - first.index) * average + average / 2f
                }
                return actualCenter - center() - offset
            }
            fun leadFor(target: Int, gap: LyricsGap?, now: Int): Float {
                if (gap == null) return 0f
                val height = mainHeights[target]?.toFloat()
                    ?: listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }?.size?.toFloat()
                    ?: leadPadding * 2f
                return (height / 2f + leadPadding) * countdownFollowOffset(gap, now)
            }
            var lastSource = timeline.frame(clock.read()).anchor
            var targetRow = -1
            var targetLead = 0f
            fun updateTarget(now: Int): Boolean {
                val current = frame
                val source = current.gap?.nextLine ?: current.anchor.takeIf { it in current.active }
                    ?: current.active.firstOrNull() ?: lastSource
                if (source !in lyrics.lines.indices) return false
                lastSource = source
                targetRow = display.lineToRow[source]
                targetLead = leadFor(targetRow, current.gap, now)
                return true
            }
            fun requestedOffset() = ((mainHeights[targetRow] ?: 0) / 2f - center() - targetLead).roundToInt()
            try {
                val now = clock.read()
                if (!updateTarget(now)) return@LaunchedEffect
                if (!positioned) {
                    // One initial placement on opening; seeks and manual resumption never repeat this snap.
                    listState.scrollToItem(targetRow, requestedOffset())
                    positioned = true
                } else if (now.toLong() < followEnd &&
                    listState.layoutInfo.visibleItemsInfo.none { it.index == targetRow }) {
                    // One cancellable rejoin for distant seeks/browsing, owned by this same effect.
                    listState.animateScrollToItem(targetRow, requestedOffset())
                }
                while (isActive && interaction.canFollow) {
                    withFrameNanos { }
                    if (!interaction.canFollow) break
                    val audio = clock.read()
                    if (audio.toLong() >= followEnd) break
                    if (!updateTarget(audio)) continue
                    val error = distance(targetRow, targetLead)
                    if (abs(error) <= 0.5f ||
                        (error > 0 && !listState.canScrollForward) || (error < 0 && !listState.canScrollBackward)) {
                        motion.reset()
                        continue
                    }
                    // Keep one Default mutation for a correction. UserInput cancels it through MutatorMutex.
                    listState.scroll(MutatePriority.Default) {
                        var previousFrame = withFrameNanos { it }
                        while (isActive && interaction.canFollow) {
                            val frameNanos = withFrameNanos { it }
                            if (!interaction.canFollow) break
                            val deltaMs = ((frameNanos - previousFrame) / 1_000_000).coerceIn(1, 32)
                            previousFrame = frameNanos
                            val latest = clock.read()
                            if (latest.toLong() >= followEnd) break
                            if (!updateTarget(latest)) break
                            val error = distance(targetRow, targetLead)
                            if (abs(error) <= 0.5f) break
                            val limit = (listState.layoutInfo.viewportSize.height / 6f).coerceAtLeast(1f)
                            val step = motion.step(error, deltaMs).coerceIn(-limit, limit)
                            val consumed = scrollBy(step)
                            if (abs(step - consumed) > 0.5f) break
                        }
                    }
                    motion.reset()
                }
            } catch (cancelled: CancellationException) {
                // A user mutation may cancel only the scroll scope, without cancelling this effect.
                if (!currentCoroutineContext().isActive) throw cancelled
                interaction.cancelledAuto(interactionGeneration)
            } finally { motion.reset() }
        }
        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize().pointerInput(interaction) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    interaction.touchDown()
                    interactionScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        listState.stopScroll(MutatePriority.UserInput)
                    }
                    try {
                        do { val event = awaitPointerEvent(PointerEventPass.Initial) }
                        while (event.changes.any { it.pressed })
                    } finally { interaction.touchUp() }
                }
            }.lyricsEdgeFade(),
            contentPadding = PaddingValues(top = maxHeight / 2, bottom = if (credits.isEmpty) maxHeight / 2 else 24.dp)
        ) {
            itemsIndexed(display.rows, key = { _, row -> row.sourceIndex }, contentType = { _, _ -> "lyric-group" }) { index, row ->
                Column(Modifier.fillMaxWidth()) {
                    val main = row.main
                    if (main is KaraokeLine) CenteredKaraokeLine(main, clock.read, motionState,
                        normalStyle, seekLine, onMainHeight = { mainHeights[index] = it })
                    else if (main is SyncedLine) LineSyncedLyricsRow(main, clock.read, motionState,
                        normalStyle, onMainHeight = { mainHeights[index] = it }) { seekLine(main) }
                    row.backing.forEach { backing ->
                        key(backing.index) {
                            val voice = backing.value
                            val voices = remember(voice) { LyricsVocalWindows(voice) }
                            TimedVocalContent(voices, clock.read, motionState) {
                                CenteredKaraokeLine(voice, clock.read, motionState, backgroundStyle, seekLine)
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
