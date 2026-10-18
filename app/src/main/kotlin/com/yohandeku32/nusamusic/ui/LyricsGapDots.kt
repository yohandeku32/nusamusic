package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/** A temporary overlay, never a persistent list spacer. Every phase follows audio time. */
@Composable
internal fun LyricsGapDots(gap: LyricsGap, time: () -> Int, modifier: Modifier = Modifier) {
    val seconds by remember(gap, time) { derivedStateOf {
        ((gap.end.toLong() - time()).coerceAtLeast(0) + 999) / 1000
    } }
    Canvas(modifier.size(96.dp, 32.dp).graphicsLayer {
        alpha = countdownOpacity(gap, time())
    }.semantics { contentDescription = "Lyrics in " + seconds + " seconds" }) {
        val now = time()
        if (countdownOpacity(gap, now) <= 0f) return@Canvas
        val remaining = (gap.end.toLong() - now).coerceAtLeast(0)
        val leadIn = minOf(3000, gap.end - gap.start).coerceAtLeast(1).toFloat()
        val progress = (1f - remaining / leadIn).coerceIn(0f, 1f)
        repeat(3) { index ->
            val fill = (progress * 3 - index).coerceIn(0f, 1f)
            val center = Offset(12.dp.toPx() + index * 24.dp.toPx(), size.height / 2)
            val radius = 5.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.3f), radius, center)
            if (fill > 0f) drawCircle(Color.White, radius * sqrt(fill), center)
        }
    }
}
