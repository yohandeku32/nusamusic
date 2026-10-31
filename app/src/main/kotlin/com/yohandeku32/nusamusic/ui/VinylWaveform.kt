package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yohandeku32.nusamusic.data.AudioWaveform
import com.yohandeku32.nusamusic.data.AudioWaveformLoader
import com.yohandeku32.nusamusic.model.Song
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.sin

internal data class VinylWaveformState(val data: AudioWaveform? = null, val loading: Boolean = false)

@Composable
internal fun rememberVinylWaveform(
    song: Song?, enabled: Boolean,
    load: suspend (android.content.Context, Song) -> AudioWaveform? = AudioWaveformLoader::load
): VinylWaveformState {
    val context = LocalContext.current.applicationContext
    var state by remember(song?.uri, song?.durationMs) { mutableStateOf(VinylWaveformState()) }
    LaunchedEffect(song?.uri, song?.durationMs, enabled) {
        if (song != null && enabled) {
            state = state.copy(loading = true)
            state = VinylWaveformState(load(context, song))
        } else state = state.copy(loading = false)
    }
    return state
}

@Composable
internal fun VinylWaveformTrack(
    waveform: AudioWaveform?, hasSong: Boolean, isPlaying: Boolean, isVisible: Boolean, fraction: () -> Float
) {
    val phase = remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    LaunchedEffect(isPlaying, isVisible, hasSong, scale, lifecycle) {
        if (isPlaying && isVisible && hasSong && scale > 0f) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var previous = 0L
                while (true) withInfiniteAnimationFrameNanos { now ->
                    if (previous == 0L) previous = now
                    val elapsed = now - previous
                    if (elapsed >= 33_000_000L) {
                        phase.floatValue = (phase.floatValue + elapsed.coerceAtMost(100_000_000L) /
                            1_000_000_000f * (2 * PI / 3.6).toFloat() / scale) % (2 * PI).toFloat()
                        previous = now
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(40.dp).drawWithCache {
        // The invisible 4 dp thumb reserves 2 dp at either edge of Material's track.
        val start = -2.dp.toPx()
        val end = size.width + 2.dp.toPx()
        val middle = size.height / 2f
        val peaks = waveform?.peaks
        val maximum = peaks?.maxOrNull()?.takeIf { it.isFinite() && it > 0f }
        val curves = Array(3) { Path() }
        val offsets = floatArrayOf(0f, 0.65f, -0.55f)
        val heights = floatArrayOf(1f, 0.72f, 0.5f)
        val ink = Color(0xFF171717)
        val unplayed = Color(0xFF85847E)
        val secondary = Color(0xFF77766F)
        val stroke = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round)
        onDrawBehind {
            val played = fraction().coerceIn(0f, 1f)
            val cursor = start + (end - start) * played
            val sourceIndex = played * ((peaks?.size ?: 1) - 1)
            val before = sourceIndex.toInt()
            val after = (before + 1).coerceAtMost((peaks?.lastIndex ?: 0))
            val level = if (peaks != null && maximum != null && peaks.size >= 2) {
                val a = peaks[before].takeIf { it.isFinite() }?.coerceIn(0f, maximum) ?: 0f
                val b = peaks[after].takeIf { it.isFinite() }?.coerceIn(0f, maximum) ?: 0f
                sqrt((a + (b - a) * (sourceIndex - before)) / maximum)
            } else if (waveform == null && hasSong) 0.7f else 0f
            if (level == 0f) {
                drawLine(unplayed, Offset(start, middle), Offset(end, middle), 3.dp.toPx(), StrokeCap.Butt)
                drawLine(ink, Offset(start, middle), Offset(cursor, middle), 3.dp.toPx(), StrokeCap.Butt)
            } else {
                val travel = phase.floatValue
                for (layer in 2 downTo 0) {
                    val curve = curves[layer]
                    curve.reset()
                    for (index in 0..96) {
                        val t = index / 96f
                        val x = start + (end - start) * t
                        val local = (t - played) / 0.16f
                        val taper = if (played >= 1f) 0f else
                            (1f - local * local).coerceAtLeast(0f).let { it * it }
                        val y = middle - sin(local * (PI * 1.4).toFloat() - travel + offsets[layer]) *
                            taper * level * heights[layer] * (middle - 2.dp.toPx()).coerceAtLeast(0f)
                        if (index == 0) curve.moveTo(x, y) else curve.lineTo(x, y)
                    }
                    drawPath(curve, unplayed, style = stroke)
                    clipRect(left = start, right = cursor) {
                        drawPath(curve, if (layer == 0) ink else secondary, style = stroke)
                    }
                }
            }
        }
    })
}
