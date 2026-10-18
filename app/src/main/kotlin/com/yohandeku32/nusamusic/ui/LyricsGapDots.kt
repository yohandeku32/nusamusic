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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

/** Countdown glyphs inside a temporary measured gap row. Every phase follows audio time. */
@Composable
internal fun LyricsGapDots(gap: LyricsGap, time: () -> Int, modifier: Modifier = Modifier) {
    val seconds by remember(gap, time) { derivedStateOf {
        ((gap.end.toLong() - time()).coerceAtLeast(0) + 999) / 1000
    } }
    val description = if (Locale.getDefault().language == "id") "Lirik dalam " + seconds + " detik"
        else "Lyrics in " + seconds + " seconds"
    Canvas(modifier.size(96.dp, 32.dp).graphicsLayer {
        val now = time()
        alpha = countdownOpacity(gap, now)
    }.semantics { contentDescription = description }) {
        val now = time()
        if (countdownOpacity(gap, now) <= 0f) return@Canvas
        val remaining = (gap.end.toLong() - now).coerceAtLeast(0)
        val leadIn = minOf(3000, gap.end - gap.start).coerceAtLeast(1).toFloat()
        val progress = (1f - remaining / leadIn).coerceIn(0f, 1f)
        repeat(3) { index ->
            val fill = (progress * 3 - index).coerceIn(0f, 1f)
            val pose = countdownDotPose(now, index)
            // Keep the existing x positions, spacing, row size and fill
            // progression. The lift/scale are paint-only and audio-time based.
            val center = Offset(
                12.dp.toPx() + index * 24.dp.toPx(),
                size.height / 2f + pose.liftDp * density
            )
            val radius = 5.dp.toPx() * pose.scale
            drawCircle(Color.White.copy(alpha = 0.3f * pose.opacity), radius, center)
            if (fill > 0f) {
                drawCircle(Color.White.copy(alpha = pose.opacity), radius * sqrt(fill), center)
            }
        }
    }
}

/** Pure floating motion: stable for pause/seek; no looping animator or state. */
internal data class CountdownDotPose(val liftDp: Float, val scale: Float, val opacity: Float)

internal fun countdownDotPose(playbackMs: Int, index: Int): CountdownDotPose {
    // One 2.2s continuous wave across all three dots. Stagger only the phase,
    // never the playback clock, to avoid mechanical loading-indicator bounce.
    val phase = playbackMs.toDouble() * 2.0 * PI / 2200.0 - index * 0.65
    val floating = sin(phase)
    val breath = 0.5 + 0.5 * sin(phase - 0.7)
    val light = 0.5 + 0.5 * sin(phase - 1.2)
    return CountdownDotPose(
        liftDp = (-1.5 * floating).toFloat(),
        scale = (1.0 + 0.065 * breath).toFloat(),
        opacity = (0.8 + 0.2 * light).toFloat()
    )
}
