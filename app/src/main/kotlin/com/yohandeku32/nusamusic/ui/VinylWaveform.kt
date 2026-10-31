package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.data.AudioWaveform
import com.yohandeku32.nusamusic.data.AudioWaveformLoader
import com.yohandeku32.nusamusic.model.Song
import kotlin.math.sqrt

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
internal fun VinylWaveformTrack(waveform: AudioWaveform?, fraction: () -> Float) {
    Box(Modifier.fillMaxWidth().height(24.dp).drawWithCache {
        // The invisible 4 dp thumb reserves 2 dp at either edge of Material's track.
        val start = -2.dp.toPx()
        val end = size.width + 2.dp.toPx()
        val middle = size.height / 2f
        val peaks = waveform?.peaks
        val maximum = peaks?.maxOrNull()?.takeIf { it.isFinite() && it > 0f }
        val amplitudes = if (peaks != null && maximum != null && peaks.size >= 2) {
            FloatArray(peaks.size) { index ->
                val value = peaks[index].takeIf { it.isFinite() }?.coerceIn(0f, maximum) ?: 0f
                sqrt(value / maximum) * (size.height / 2f - 1.dp.toPx()).coerceAtLeast(0f)
            }
        } else null
        val envelope = amplitudes?.let { heights ->
            Path().apply {
                val step = (end - start) / (heights.size - 1)
                moveTo(start, middle)
                lineTo(start, middle - heights[0])
                for (index in 1 until heights.size) {
                    val previousX = start + (index - 1) * step
                    val x = start + index * step
                    quadraticTo(previousX, middle - heights[index - 1],
                        (previousX + x) / 2f, middle - (heights[index - 1] + heights[index]) / 2f)
                }
                lineTo(end, middle - heights.last())
                lineTo(end, middle + heights.last())
                for (index in heights.lastIndex - 1 downTo 0) {
                    val previousX = start + (index + 1) * step
                    val x = start + index * step
                    quadraticTo(previousX, middle + heights[index + 1],
                        (previousX + x) / 2f, middle + (heights[index + 1] + heights[index]) / 2f)
                }
                lineTo(start, middle + heights[0]); close()
            }
        }
        onDrawBehind {
            val played = fraction().coerceIn(0f, 1f)
            val cursor = start + (end - start) * played
            if (envelope == null) {
                drawLine(Color(0xFF85847E), Offset(start, middle), Offset(end, middle), 3.dp.toPx(), StrokeCap.Butt)
                drawLine(Color(0xFF171717), Offset(start, middle), Offset(cursor, middle), 3.dp.toPx(), StrokeCap.Butt)
            } else {
                drawPath(envelope, Color(0xFF85847E))
                clipRect(left = start, right = cursor) { drawPath(envelope, Color(0xFF171717)) }
                drawLine(Color(0xFF171717), Offset(cursor, 0f), Offset(cursor, size.height), 1.dp.toPx())
            }
        }
    })
}
