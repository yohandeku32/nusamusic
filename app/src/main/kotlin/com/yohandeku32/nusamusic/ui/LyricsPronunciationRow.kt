package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
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

@Composable
internal fun LyricsPronunciationRow(
    pronunciation: LyricsPronunciation, style: TextStyle, alignEnd: Boolean,
    time: () -> Int
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = remember(style, alignEnd) { style.copy(
        fontSize = (style.fontSize.value * 0.5f).coerceAtLeast(14f).sp,
        lineHeight = (style.lineHeight.value * 0.5f).coerceAtLeast(20f).sp,
        fontWeight = FontWeight.Medium,
        textDirection = TextDirection.Content,
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        color = Color.White
    ) }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
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
            drawText(layout, color = Color.White, alpha = 0.82f, topLeft = Offset.Zero)
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
