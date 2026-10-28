package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.*
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import com.yohandeku32.nusamusic.data.LyricsUnitKey
import com.yohandeku32.nusamusic.data.LyricsPronunciation

/** Measurements are retained; playback-time effects invalidate drawing/layers only. */
@Composable
internal fun CenteredKaraokeLine(
    line: KaraokeLine, time: () -> Int, motion: LyricsMotionState,
    style: TextStyle, onSeek: (ISyncedLine) -> Unit, onMainHeight: (Int) -> Unit = {},
    expressions: List<LyricsAdaptiveExpression>? = null,
    alignEnd: Boolean = false,
    inferredEnds: Set<LyricsUnitKey> = emptySet(),
    pronunciation: LyricsPronunciation? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = remember(style) { style.copy(textDirection = TextDirection.Content) }
    val rtl = remember(line) { line.syllables.any { it.content.isRtl() } }
    val voices = remember(line) { LyricsVocalWindows(line) }
    val live by remember(line, time, motion) { derivedStateOf {
        val now = time()
        if (line.isAccompaniment) voices.previewPresence(now, motion.resetAt) > 0f
        else now >= line.start && now.toLong() < line.end.toLong() + 160L &&
            motion.resetAt < line.end
    } }
    val visualTime = remember(live, time) { { if (live) time() else Int.MIN_VALUE } }
    MotionRow(line, visualTime, motion, voices, line.isAccompaniment, { onSeek(line) }, onMainHeight) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = constraints.maxWidth.toFloat()
            val wrapped = remember(line, textStyle, textMeasurer, density, width) {
                val space = textMeasurer.measure(" ", textStyle).size.width.toFloat()
                val syllables = measureSyllablesAndDetermineAnimation(line.syllables, textMeasurer, textStyle, true, space)
                calculateBalancedLines(syllables, width, textMeasurer, textStyle)
            }
            val lineHeight = remember(textMeasurer, textStyle, density) { textMeasurer.measure("M", textStyle).size.height.toFloat() }
            val layouts = remember(wrapped, width, lineHeight, rtl) {
                calculateStaticLineLayout(wrapped, false, width, lineHeight, rtl)
            }
            val painter = remember(layouts, textMeasurer, density.density, line.start, line.end, expressions, alignEnd, width, inferredEnds) {
                SyllableLyricsPainter(layouts, density.density, expressions,
                    alignEnd = alignEnd, viewportWidthPx = width, inferredEnds = inferredEnds,
                    textMeasurer = textMeasurer)
            }
            val height = with(density) { (lineHeight * wrapped.size).toDp() } + 8.dp
            val readable by remember(line, voices, visualTime, motion) { derivedStateOf {
                !line.isAccompaniment || voices.previewPresence(visualTime(), motion.resetAt) > 0f
            } }
            Canvas(Modifier.fillMaxWidth().height(height).clearAndSetSemantics {
                if (readable) contentDescription = line.syllables.joinToString("") { it.content }
            }) {
                val now = visualTime()
                // Backing vocal must use its OWN 380ms fade rather than the
                // main lyric's 160ms release. The row alpha is already driven
                // by voices.presence; don't fade its highlight a second time.
                val focus = if (line.isAccompaniment) 1f
                    else lyricPresence(line.start, line.end, now, motion.resetAt)
                // Raw playback time drives the whole-line wave even when a
                // syllable is inactive. visualTime retains the old syllable
                // highlight/seek logic, so the two animations stay separate.
                painter.draw(this, now, focus, waveTime = time(),
                    playbackRevision = motion.revision)
            }
        }
        pronunciation?.let { LyricsPronunciationRow(it, style, alignEnd, time) }
        line.translation?.takeIf { it.isNotBlank() }?.let { LyricsTranslation(it, style, alignEnd) }
    }
}

@Composable
private fun MotionRow(
    line: ISyncedLine, time: () -> Int, motion: LyricsMotionState,
    voices: LyricsVocalWindows, mini: Boolean, onSeek: () -> Unit, onMainHeight: (Int) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val rowPaddingPx = with(LocalDensity.current) { 16.dp.roundToPx() }
    val enabled by remember(line, time, motion, mini) { derivedStateOf { !mini || voices.previewPresence(time(), motion.resetAt) > 0f } }
    Column(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onSeek)
        .padding(horizontal = 16.dp, vertical = 8.dp).onSizeChanged { onMainHeight(it.height + rowPaddingPx) }.graphicsLayer {
            val now = time()
            val presence = lyricPresence(line.start, line.end, now, motion.resetAt)
            val vocal = if (mini) voices.previewPresence(now, motion.resetAt) else presence
            compositingStrategy = CompositingStrategy.ModulateAlpha
            clip = false
            alpha = if (mini) vocal * 0.75f else 1f
            transformOrigin = TransformOrigin(0f, 0.5f)
            // Syllables carry the expression; the line baseline stays anchored.
            translationY = if (mini) (1f - vocal) * 4.dp.toPx() else 0f
        }, content = content)
}

/** Remove inactive content entirely; only the timed expansion/collapse is measured. */
@Composable
internal fun TimedVocalContent(
    voices: LyricsVocalWindows, time: () -> Int, motion: LyricsMotionState, content: @Composable () -> Unit
) {
    val extent = remember(voices, time, motion) {
        derivedStateOf(structuralEqualityPolicy()) { voices.previewPresence(time(), motion.resetAt) }
    }
    val present by remember(extent) { derivedStateOf { extent.value > 0f } }
    // Clip to the animating layout height; without it the full-height backing
    // row is drawn outside a nearly zero-height slot and seems to pop on/off.
    if (present) Column(Modifier.fillMaxWidth().clipToBounds().layout { measurable, constraints ->
        val child = measurable.measure(constraints.copy(minHeight = 0))
        val height = vocalLayoutHeight(child.height, extent.value)
        layout(child.width, height) { child.placeRelative(0, 0) }
    }) { content() }
}

@Composable
private fun LyricsTranslation(text: String, style: TextStyle, alignEnd: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        style = style.copy(
            fontSize = (style.fontSize.value * 0.65f).coerceAtLeast(14f).sp,
            lineHeight = (style.lineHeight.value * 0.65f).coerceAtLeast(20f).sp
        ),
        color = Color.White.copy(alpha = 0.82f),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

@Composable
internal fun LineSyncedLyricsRow(
    line: SyncedLine, time: () -> Int, motion: LyricsMotionState, style: TextStyle, onMainHeight: (Int) -> Unit = {},
    alignEnd: Boolean = false, pronunciation: LyricsPronunciation? = null, onSeek: () -> Unit
) {
    val voices = remember(line) { LyricsVocalWindows(line) }
    val live by remember(line, time, motion) { derivedStateOf {
        val now = time()
        now >= line.start && now.toLong() < line.end.toLong() + 160 && motion.resetAt < line.end
    } }
    val visualTime = remember(live, time) { { if (live) time() else Int.MIN_VALUE } }
    MotionRow(line, visualTime, motion, voices, false, onSeek, onMainHeight) {
        Text(line.content, Modifier.fillMaxWidth().graphicsLayer {
            alpha = 0.4f + 0.6f * lyricPresence(line.start, line.end, visualTime(), motion.resetAt)
        }, style = style, color = Color.White, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start)
        pronunciation?.let { LyricsPronunciationRow(it, style, alignEnd, time) }
        line.translation?.takeIf { it.isNotBlank() }?.let { LyricsTranslation(it, style, alignEnd) }
    }
}
