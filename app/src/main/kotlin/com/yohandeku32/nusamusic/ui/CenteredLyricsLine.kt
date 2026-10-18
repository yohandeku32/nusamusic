package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.*
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl

/** Reuse Accompanist's wrapping, then paint each syllable from its own timestamps. */
@Composable
internal fun CenteredKaraokeLine(
    line: KaraokeLine,
    focused: Boolean,
    time: () -> Int,
    style: TextStyle,
    onSeek: (ISyncedLine) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = remember(style) { style.copy(textDirection = TextDirection.Content) }
    val rtl = remember(line) { line.syllables.any { it.content.isRtl() } }
    LyricsLineContainer(focused, { onSeek(line) }, if (line.isAccompaniment) 0.6f else 1f, if (line.isAccompaniment) 0.2f else 0.4f) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = constraints.maxWidth.toFloat()
            // Every measurement depends on text size, available width, and system font scale.
            val wrapped = remember(line, textStyle, textMeasurer, density, width) {
                val space = textMeasurer.measure(" ", textStyle).size.width.toFloat()
                // Our painter owns animation; skip upstream whole-word character measurements.
                val syllables = measureSyllablesAndDetermineAnimation(line.syllables, textMeasurer, textStyle, true, space)
                calculateBalancedLines(syllables, width, textMeasurer, textStyle)
            }
            val lineHeight = remember(textMeasurer, textStyle, density) { textMeasurer.measure("M", textStyle).size.height.toFloat() }
            val layouts = remember(wrapped, width, lineHeight, rtl) {
                calculateStaticLineLayout(wrapped, false, width, lineHeight, rtl)
            }
            val height = with(density) { (lineHeight * wrapped.size).toDp() } + 8.dp
            Canvas(Modifier.fillMaxWidth().height(height).semantics {
                contentDescription = line.syllables.joinToString("") { it.content }
            }) {
                drawSyllableLyrics(layouts, if (focused) time() else 0, focused)
            }
        }
        line.translation?.let { Translation(it, style) }
    }
}

@Composable
private fun LyricsLineContainer(
    focused: Boolean,
    onClick: () -> Unit,
    activeOpacity: Float = 1f,
    inactiveOpacity: Float = 0.4f,
    content: @Composable ColumnScope.() -> Unit
) {
    val opacity = animateFloatAsState(if (focused) activeOpacity else inactiveOpacity,
        tween(420, easing = FastOutSlowInEasing), label = "lyricOpacity")
    val scale = animateFloatAsState(if (focused) 1f else 0.97f,
        tween(420, easing = FastOutSlowInEasing), label = "lyricScale")
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp).graphicsLayer {
        alpha = opacity.value
        transformOrigin = TransformOrigin(0f, 0.5f)
        scaleX = scale.value
        scaleY = scale.value
    }, content = content)
}

@Composable
private fun Translation(text: String, style: TextStyle) {
    Text(text, Modifier.fillMaxWidth(), style = style.copy(fontSize = style.fontSize * 0.65f, lineHeight = style.lineHeight * 0.65f),
        color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Start)
}

@Composable
internal fun LineSyncedLyricsRow(
    line: com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine,
    focused: Boolean, style: TextStyle, onSeek: () -> Unit
) {
    LyricsLineContainer(focused, onSeek) {
        Text(line.content, Modifier.fillMaxWidth(), style = style, color = Color.White, textAlign = TextAlign.Start)
        line.translation?.let { Translation(it, style) }
    }
}
