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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import android.os.Build
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.data.SongCredits
import com.yohandeku32.nusamusic.data.LyricsUnitKey
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun SyncedLyricsView(
    lyrics: SyncedLyrics, position: State<Long>, visible: Boolean,
    listState: LazyListState, motionState: LyricsMotionState,
    onSeek: (ISyncedLine) -> Unit, fontSize: Float, credits: SongCredits = SongCredits(), modifier: Modifier = Modifier,
    inferredEnds: Set<LyricsUnitKey> = emptySet()
) {
    val clock = remember { LyricsPlaybackClock(position, visible) }
    SideEffect { clock.update(position, visible) }
    val display = remember(lyrics) { LyricsDisplayRows(lyrics.lines) }
    val firstVocalRow = remember(display) { display.rows.indexOfFirst { it.main != null }.coerceAtLeast(0) }
    // Analyze all timed syllables on Default, not during the first Compose
    // frame of the Lyrics panel. Pure CPU results are published once.
    val expressions by produceState<Map<KaraokeLine, List<LyricsAdaptiveExpression>>>(
        initialValue = emptyMap(), display, inferredEnds
    ) {
        value = withContext(Dispatchers.Default) {
            buildMap {
                putAll(lyricsAdaptiveLineExpressions(
                    display.rows.mapNotNull { it.main as? KaraokeLine }, inferredEnds
                ))
                display.rows.flatMap { it.backing }.forEach {
                    putAll(lyricsAdaptiveLineExpressions(listOf(it.value), inferredEnds))
                }
            }
        }
    }
    val mainHeights = remember(lyrics) { HashMap<Int, Int>() }
    val followEnd = remember(lyrics) { lyrics.lines.maxOfOrNull { it.end }?.toLong()?.plus(160) ?: 0L }
    val timeline = remember(lyrics) { LyricsTimeline(lyrics.lines) }
    val visualFocus = remember(lyrics) { LyricsVisualFocusController(lyrics.lines) }
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
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val gapHeight = with(androidx.compose.ui.platform.LocalDensity.current) { lyricsGapRowHeight.toPx() }

        // Half as many cached GPU blur effects (0.2dp steps across the
        // same 0..5.4dp range). No shader allocation on playback ticks.
        val blurEffects = remember(density) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Array<BlurEffect?>(28) { step ->
                    if (step == 0) null else BlurEffect(
                        radiusX = step * 0.2f * density,
                        radiusY = step * 0.2f * density,
                        edgeTreatment = TileMode.Decal
                    )
                }
            } else emptyArray()
        }
        LaunchedEffect(visible, interaction.canFollow, fontSize, maxHeight, maxWidth, motionState.revision) {
            val motion = LyricsFollowMotion()
            val interactionGeneration = interaction.generation
            if (!visible || !interaction.canFollow) return@LaunchedEffect
            snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it == display.rows.size + if (credits.isEmpty) 0 else 1 }
            fun center(target: Int) = with(listState.layoutInfo) {
                lyricsFollowAnchor(viewportStartOffset, viewportEndOffset, target == firstVocalRow,
                    mainHeight = mainHeights[target]?.toFloat() ?: 0f, minimumPadding = 24f * density)
            }
            fun distance(target: Int, offset: Float): Float {
                val items = listState.layoutInfo.visibleItemsInfo
                val item = items.firstOrNull { it.index == target }
                val actualCenter = if (item != null) item.offset + offset else {
                    val first = items.firstOrNull() ?: return 0f
                    var total = 0
                    var count = 0
                    for (visibleItem in items) if (visibleItem.index < display.rows.size) { total += visibleItem.size; count++ }
                    val average = if (count > 0) (total.toFloat() / count).coerceAtLeast(1f) else gapHeight
                    first.offset + (target - first.index) * average + offset
                }
                return actualCenter - center(target)
            }
            fun anchorFor(target: Int, gap: LyricsGap?, now: Int): Float {
                val fraction = gap?.let { countdownLayoutFraction(it, now) } ?: 0f
                val measured = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }?.size?.toFloat()
                val mainHeight = mainHeights[target]?.toFloat()
                    ?: measured?.let { (it - gapHeight * fraction).coerceAtLeast(0f) } ?: gapHeight
                return countdownRowAnchor(mainHeight, gapHeight, fraction)
            }
            var targetRow = -1
            var nextRow = -1
            var targetAnchor = 0f
            var nextAnchor = 0f
            var targetBlend = 0f
            var targetResponse = 1f
            fun updateTarget(now: Int): Boolean {
                val current = frame
                visualFocus.update(now)
                val source = current.gap?.nextLine ?: visualFocus.fromSource
                if (source !in lyrics.lines.indices) return false
                targetRow = display.lineToRow[source]
                targetAnchor = anchorFor(targetRow, current.gap, now)
                val next = if (current.gap == null) visualFocus.toSource else source
                nextRow = display.lineToRow[next]
                nextAnchor = anchorFor(nextRow, current.gap, now)
                targetBlend = if (current.gap == null) visualFocus.blend else 0f
                targetResponse = if (current.gap == null) visualFocus.responsiveness else 1f
                return true
            }
            fun rejoinRow() = if (targetBlend > 0.5f) nextRow else targetRow
            fun requestedOffset(): Int {
                val row = rejoinRow()
                val anchor = if (targetBlend > 0.5f) nextAnchor else targetAnchor
                return (anchor - center(row)).roundToInt()
            }
            fun targetError(): Float {
                val from = distance(targetRow, targetAnchor)
                if (targetBlend <= 0f || targetRow == nextRow) return from
                return from + (distance(nextRow, nextAnchor) - from) * targetBlend
            }
            try {
                val now = clock.read()
                if (!updateTarget(now)) return@LaunchedEffect
                if (!positioned) {
                    // One initial placement on opening; seeks and manual resumption never repeat this snap.
                    listState.scrollToItem(rejoinRow(), requestedOffset())
                    positioned = true
                } else if (now.toLong() < followEnd &&
                    listState.layoutInfo.visibleItemsInfo.none { it.index == rejoinRow() }) {
                    // One cancellable rejoin for distant seeks/browsing, owned by this same effect.
                    listState.animateScrollToItem(rejoinRow(), requestedOffset())
                }
                while (isActive && interaction.canFollow) {
                    withFrameNanos { }
                    if (!interaction.canFollow) break
                    val audio = clock.read()
                    if (audio.toLong() >= followEnd) break
                    if (!updateTarget(audio)) continue
                    val error = targetError()
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
                            val error = targetError()
                            if (abs(error) <= 0.5f) break
                            val limit = (listState.layoutInfo.viewportSize.height / 6f).coerceAtLeast(1f)
                            val step = motion.step(error, deltaMs, targetResponse).coerceIn(-limit, limit)
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
                    val gap = frame.gap?.takeIf { display.lineToRow[it.nextLine] == index }
                    if (gap != null) LyricsGapRow(gap, clock.read)

                    // Keep the stronger main-text focus separate from backing
                    // vocals and countdown. Layer changes never affect layout.
                    fun visualDistanceDp(): Float {
                        val info = listState.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                        val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        return if (item != null) (item.offset + item.size / 2f - centre) / density else 0f
                    }
                    val main = row.main
                    if (main != null) Column(Modifier.fillMaxWidth().graphicsLayer {
                        val now = clock.read()
                        val focus = lyricsMainFocusDepth(visualDistanceDp(), main.start, main.end, now,
                            visualFocus.focusAmount(row.sourceIndex, now))
                        alpha = focus.opacity
                        // Let upcoming lines rise gently into focus rather than appearing
                        // as a rigid scrolling list. This is a GPU-only transform:
                        // no text remeasurement, extra coroutine, or per-frame allocation.
                        translationY = if (visualDistanceDp() > 0f) {
                            (1f - focus.opacity).coerceIn(0f, 1f) * 14f * density
                        } else 0f
                        renderEffect = if (blurEffects.isNotEmpty()) {
                            blurEffects[(focus.blurDp * 5f).roundToInt().coerceIn(0, blurEffects.lastIndex)]
                        } else null
                        // Existing 16dp horizontal / 8dp vertical text padding
                        // exceeds the maximum 5.5dp blur; no clipping or resizing.
                        clip = false
                    }) {
                        if (main is KaraokeLine) CenteredKaraokeLine(main, clock.read, motionState,
                            normalStyle, seekLine, onMainHeight = { mainHeights[index] = it }, expressions = expressions[main])
                        else if (main is SyncedLine) LineSyncedLyricsRow(main, clock.read, motionState,
                            normalStyle, onMainHeight = { mainHeights[index] = it }) { seekLine(main) }
                    }
                    // Preserve the original backing-vocal blur and opacity.
                    if (row.backing.isNotEmpty()) Column(Modifier.fillMaxWidth().graphicsLayer {
                        val focus = lyricsFocusDepth(visualDistanceDp())
                        alpha = focus.opacity
                        renderEffect = if (blurEffects.isNotEmpty()) {
                            blurEffects[(focus.blurDp * 5f).roundToInt().coerceIn(0, blurEffects.lastIndex)]
                        } else null
                        clip = false
                    }) {
                        row.backing.forEach { backing ->
                            key(backing.index) {
                                val voice = backing.value
                                val voices = remember(voice) { LyricsVocalWindows(voice) }
                                TimedVocalContent(voices, clock.read, motionState) {
                                    CenteredKaraokeLine(voice, clock.read, motionState, backgroundStyle, seekLine,
                                        expressions = expressions[voice])
                                }
                            }
                        }
                    }
                }
            }
            if (!credits.isEmpty) item(key = "credits", contentType = "credits") {
                LyricsCredits(credits, Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
