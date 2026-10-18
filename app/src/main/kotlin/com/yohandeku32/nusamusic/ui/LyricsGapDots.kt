package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/** Playback time drives both breathing and the final three-second countdown. */
@Composable
internal fun LyricsGapDots(gap: LyricsGap, time: () -> Int, active: Boolean) {
    val opacity = animateFloatAsState(if (active) 1f else 0f, tween(220), label = "gapOpacity")
    val heldTime = remember(gap) { intArrayOf(gap.start) }
    val seconds by remember(gap, time, active) {
        derivedStateOf { if (active) ((gap.end.toLong() - time()).coerceAtLeast(0) + 999) / 1_000 else 0 }
    }
    val description = if (!active) {
        if (Locale.getDefault().language == "id") "Jeda lirik" else "Lyric break"
    } else if (Locale.getDefault().language == "id") "Lirik dalam $seconds detik" else "Lyrics in $seconds seconds"
    // Inactive indicators fade to zero and lose their accessibility label. Keep only
    // the empty slot so dismissing the dots cannot shift the following lyric row.
    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp, 32.dp).graphicsLayer { alpha = opacity.value }
            .semantics { if (active) contentDescription = description }) {
            if (opacity.value <= 0f) return@Canvas
            val now = if (active) time().also { heldTime[0] = it } else heldTime[0]
            val remaining = (gap.end.toLong() - now).coerceAtLeast(0)
            val leadIn = minOf(3_000, gap.end - gap.start).coerceAtLeast(1).toFloat()
            val progress = (1f - remaining / leadIn).coerceIn(0f, 1f)
            repeat(3) { index ->
                val fill = (progress * 3 - index).coerceIn(0f, 1f)
                val breath = sin((now.toLong() - gap.start).toDouble() / 1_000 * PI * 2 - index * 0.5).toFloat()
                val pulse = if (fill > 0f && fill < 1f) sin(fill * PI).toFloat() * 0.18f else 0f
                val radius = 5.5.dp.toPx() * (1f + breath * 0.08f + pulse)
                val center = Offset(size.width / 2 + (index - 1) * 24.dp.toPx(), size.height / 2)
                drawCircle(Color.White.copy(alpha = 0.3f), radius, center)
                if (fill > 0f) drawCircle(Color.White, radius * sqrt(fill), center)
            }
        }
    }
}
