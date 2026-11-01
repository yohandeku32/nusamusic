package com.yohandeku32.nusamusic.ui

import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.core.content.edit
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.*
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import com.yohandeku32.nusamusic.data.SongLyrics
import com.yohandeku32.nusamusic.model.Song

internal object MiniLyricsPreference {
    private const val KEY = "mini_lyrics_enabled"
    fun read(preferences: SharedPreferences): Boolean =
        runCatching { preferences.getBoolean(KEY, false) }.getOrDefault(false)
    fun write(preferences: SharedPreferences, enabled: Boolean) { preferences.edit { putBoolean(KEY, enabled) } }
}

/** A single main voice; intros, rests and backing-only passages do not preview future lyrics. */
internal class MiniLyricsTimeline(lines: List<ISyncedLine>) {
    private val main = lines.filterNot { it is KaraokeLine && it.isAccompaniment }
    private val timeline = LyricsTimeline(main)
    private val cues = buildList<MiniLyricsCue> {
        val starts = main.indices.groupBy { main[it].start }
        val ends = main.indices.groupBy { main[it].end }
        val active = java.util.TreeSet(compareByDescending<Int> { main[it].start }.thenBy { it })
        val edges = (starts.keys + ends.keys +
            timeline.gaps.flatMap { listOf(it.start, it.end) }).distinct().sorted()
        var gapIndex = 0
        for ((start, end) in edges.zipWithNext()) {
            ends[start]?.forEach { active.remove(it) }
            starts[start]?.forEach { active.add(it) }
            while (gapIndex < timeline.gaps.size && timeline.gaps[gapIndex].end <= start) gapIndex++
            if (timeline.gaps.getOrNull(gapIndex)?.let { start >= it.start } == true) continue
            val line = active.firstOrNull()?.let { main[it] } ?: continue
            val previous = lastOrNull()
            if (previous != null && previous.line === line && previous.end == start)
                set(lastIndex, previous.copy(end = end))
            else add(MiniLyricsCue(line, start, end))
        }
    }
    fun cueAt(now: Int): MiniLyricsCue? {
        var low = 0
        var high = cues.size
        while (low < high) {
            val middle = (low + high) / 2
            if (cues[middle].start <= now) low = middle + 1 else high = middle
        }
        return cues.getOrNull(low - 1)?.takeIf { now < it.end }
    }
    fun lineAt(now: Int): ISyncedLine? = cueAt(now)?.line
}

internal data class MiniLyricsCue(val line: ISyncedLine, val start: Int, val end: Int)
internal data class MiniLyricsLinePose(val alpha: Float, val offsetDp: Float)

/** A single visible row eases through each authored boundary, using playback rather than UI time. */
internal fun miniLyricsLinePose(cue: MiniLyricsCue, now: Int): MiniLyricsLinePose {
    val third = ((cue.end.toLong() - cue.start) / 3f).coerceAtLeast(1f)
    val entering = heldWordEase((now.toLong() - cue.start) / minOf(180f, third))
    val leaving = heldWordEase((cue.end.toLong() - now) / minOf(120f, third))
    return MiniLyricsLinePose(minOf(entering, leaving), 4f * (1f - entering) - 3f * (1f - leaving))
}

@Composable
internal fun MiniLyricsHost(
    song: Song?, state: ImmersiveLyricsState, positionMs: State<Long>, readPosition: () -> Long,
    readEpoch: () -> Int, isPlaying: Boolean, playbackEpoch: Int, onlineEnabled: Boolean,
    lightMode: Boolean
) {
    if (song == null) return
    val scope = rememberCoroutineScope()
    val entry = state.forSong(song)
    LaunchedEffect(song.uri, onlineEnabled) { state.load(song, scope, onlineEnabled = onlineEnabled) }
    val playback = rememberLyricsFramePlayback(song.uri, positionMs, readPosition, readEpoch, isPlaying, playbackEpoch)
    val data = entry.data?.takeIf {
        onlineEnabled || it.stored.source in setOf("embedded", "saved-in-app")
    }
    MiniLyricsLine(data, playback.position, playback.motion, lightMode)
}

@Composable
internal fun MiniLyricsLine(data: SongLyrics?, position: State<Long>, motion: LyricsMotionState, lightMode: Boolean) {
    val current = data ?: return
    val lines = current.synced?.lines.orEmpty()
    val timeline = remember(lines) { MiniLyricsTimeline(lines) }
    val active by remember(timeline, position) { derivedStateOf {
        timeline.cueAt(position.value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
    } }
    val time = remember(position) { { position.value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt() } }
    val ink = if (lightMode) Color(0xFF171A20) else Color.White
    val cue = active ?: return
    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        Box(Modifier.fillMaxWidth().graphicsLayer {
            val pose = if (durationScale?.scaleFactor == 0f) MiniLyricsLinePose(1f, 0f)
                else miniLyricsLinePose(cue, time())
            alpha = pose.alpha
            translationY = pose.offsetDp * density
        }) {
            when (val line = cue.line) {
                is KaraokeLine -> key(line, current.inferredEnds) {
                    MiniKaraokeLine(line, current, time, motion, ink)
                }
                is SyncedLine -> Text(line.content, color = ink.copy(alpha = 0.9f), fontSize = 18.sp,
                    lineHeight = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = line.content })
            }
        }
    }
}

@Composable
private fun MiniKaraokeLine(line: KaraokeLine, data: SongLyrics, time: () -> Int,
                            motion: LyricsMotionState, ink: Color) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val animate = durationScale?.scaleFactor != 0f
    val style = remember { TextStyle(fontSize = 34.sp, lineHeight = 43.sp,
        fontWeight = FontWeight.ExtraBold, textDirection = TextDirection.Content) }
    val scale = 18f / 34f
    val measured = remember(line, style, measurer, density) {
        measureSyllablesAndDetermineAnimation(line.syllables, measurer, style, true,
            measurer.measure(" ", style).size.width.toFloat())
    }
    val width = remember(measured) { measured.sumOf { it.width.toDouble() }.toFloat() }
    val height = remember(measured) { measured.maxOfOrNull { it.textLayoutResult.size.height }?.toFloat() ?: 0f }
    val rtl = remember(line) { line.syllables.any { it.content.isRtl() } }
    val layouts = remember(measured, width, height, rtl) {
        calculateStaticLineLayout(listOf(WrappedLine(measured, width)), false, width, height, rtl)
    }
    val expressions = remember(line, data.inferredEnds) {
        lyricsAdaptiveLineExpressions(listOf(line), data.inferredEnds)[line]
    }
    val painter = remember(layouts, density.density, expressions, data.inferredEnds, animate, measurer, ink) {
        SyllableLyricsPainter(layouts, density.density, expressions, inferredEnds = data.inferredEnds,
            textMeasurer = measurer, motionEnabled = animate,
            baseAlpha = if (ink == Color.White) 0.58f else 0.68f, highlightAlpha = 1f)
    }
    val spans = remember(layouts) { layouts.flatten().map {
        LyricsVocalSpan(it.syllable.start, it.syllable.end, it.position.x, it.position.x + it.width)
    } }
    val front = remember(spans) { LyricsTravelingWaveMask(spans) }
    val tint = remember(ink) { Paint().apply {
        colorFilter = PorterDuffColorFilter(ink.toArgb(), PorterDuff.Mode.SRC_IN)
    } }
    BoxWithConstraints(Modifier.fillMaxWidth().height(with(density) { 23.sp.toDp() + 12.dp }).clipToBounds()) {
        val viewport = constraints.maxWidth.toFloat() / scale
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {
            contentDescription = line.syllables.joinToString("") { it.content }
        }) {
            val now = time()
            val span = spans.lastOrNull { it.startMs <= now }
            val cursor = if (rtl && span != null) {
                val fraction = ((now.toLong() - span.startMs).toFloat() /
                    (span.endMs.toLong() - span.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
                span.endDp - (span.endDp - span.startDp) * fraction
            } else front.front(now)?.positionDp ?: span?.endDp ?: 0f
            val pan = (cursor - viewport * 0.55f).coerceIn(0f, (width - viewport).coerceAtLeast(0f))
            val canvas = drawContext.canvas.nativeCanvas
            val saved = if (ink != Color.White) canvas.saveLayer(0f, 0f, size.width, size.height, tint) else -1
            val top = 6.dp.toPx()
            try {
                withTransform({
                    translate(0f, top)
                    scale(scale, scale, Offset.Zero)
                    translate(-pan, 0f)
                }) { painter.draw(this, now, 1f, now, motion.revision) }
            } finally { if (saved >= 0) canvas.restoreToCount(saved) }
        }
    }
}
