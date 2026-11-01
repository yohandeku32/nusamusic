package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.data.LyricsPronunciation
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.*
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl

internal fun lyricsReadingStyle(style: TextStyle, alignEnd: Boolean): TextStyle = style.copy(
    fontSize = (style.fontSize.value * 0.55f).coerceAtLeast(14f).sp,
    lineHeight = (style.lineHeight.value * 0.55f).coerceAtLeast(20f).sp,
    fontWeight = FontWeight.Bold,
    textDirection = TextDirection.Content,
    textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
    color = Color.White
)

@Composable
internal fun LyricsPronunciationRow(
    pronunciation: LyricsPronunciation, style: TextStyle, alignEnd: Boolean,
    time: () -> Int, motion: LyricsMotionState,
    sourcePose: () -> LyricsCompanionPose = { LyricsCompanionPose.Rest }
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val timedLine = remember(pronunciation) { pronunciationKaraokeLine(pronunciation) }
    val textStyle = remember(style, alignEnd) { lyricsReadingStyle(style, alignEnd) }
    if (timedLine != null) {
        val renderScale = textStyle.fontSize.value / style.fontSize.value.coerceAtLeast(1f)
        val measuredStyle = remember(textStyle, renderScale) { textStyle.copy(
            fontSize = (textStyle.fontSize.value / renderScale).sp,
            lineHeight = (textStyle.lineHeight.value / renderScale).sp
        ) }
        val expressions = remember(timedLine) { lyricsAdaptiveLineExpressions(listOf(timedLine), emptySet())[timedLine] }
        val animated = durationScale?.scaleFactor != 0f
        val rtl = remember(timedLine) { timedLine.syllables.any { it.content.isRtl() } }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            val width = constraints.maxWidth.toFloat() / renderScale
            val wrapped = remember(timedLine, measuredStyle, width, density, measurer) {
                val space = measurer.measure(" ", measuredStyle).size.width.toFloat()
                val syllables = measureSyllablesAndDetermineAnimation(timedLine.syllables, measurer, measuredStyle, true, space)
                calculateBalancedLines(syllables, width, measurer, measuredStyle)
            }
            val lineHeight = remember(measuredStyle, density, measurer) { measurer.measure("M", measuredStyle).size.height.toFloat() }
            val layouts = remember(wrapped, width, lineHeight, rtl) { calculateStaticLineLayout(wrapped, false, width, lineHeight, rtl) }
            val painter = remember(layouts, density, expressions, alignEnd, width, animated, measurer) {
                SyllableLyricsPainter(layouts, density.density, expressions, alignEnd, width, textMeasurer = measurer,
                    baseAlpha = 0.58f, highlightAlpha = 1f, motionEnabled = animated)
            }
            val height = with(density) { ((lineHeight * wrapped.size + 8.dp.toPx()) * renderScale).toDp() }
            Canvas(Modifier.fillMaxWidth().height(height).clearAndSetSemantics { contentDescription = pronunciation.text }) {
                val now = time()
                val focus = lyricPresence(timedLine.start, timedLine.end, now, motion.resetAt)
                // Scale the existing renderer as a whole, including its motion and halo.
                withTransform({ scale(renderScale, renderScale, Offset.Zero) }) {
                    painter.draw(this, now, focus, now, motion.revision)
                }
            }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        .lyricsCompanionMotion(sourcePose, 0.55f)) {
        val width = constraints.maxWidth
        val layout = remember(pronunciation, textStyle, width, density) {
            measurer.measure(pronunciation.text, textStyle, constraints = Constraints.fixedWidth(width))
        }
        val words = remember(pronunciation, layout) {
            pronunciation.words.map { word ->
                word to layout.getPathForRange(word.range.first, word.range.last + 1)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(with(density) { layout.size.height.toDp() })
            .clearAndSetSemantics { contentDescription = pronunciation.text }) {
            drawText(layout, color = Color.White, alpha = 0.58f, topLeft = Offset.Zero)
            val now = time()
            val first = pronunciation.words.minOfOrNull { it.start }
            val last = pronunciation.words.maxOfOrNull { it.end }
            if (first != null && last != null && now in first until last) {
                for ((word, path) in words) {
                    val progress = ((now.toLong() - word.start).toFloat() / (word.end - word.start))
                        .coerceIn(0f, 1f)
                    if (progress > 0f) clipPath(path) {
                        drawText(layout, color = Color.White, alpha = progress, topLeft = Offset.Zero)
                    }
                }
            }
        }
    }
}
