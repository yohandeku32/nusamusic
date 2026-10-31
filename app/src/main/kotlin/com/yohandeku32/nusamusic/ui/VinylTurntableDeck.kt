package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
internal fun VinylTurntableDeck(
    size: Dp,
    isPlaying: Boolean,
    isVisible: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    repeatMode: Int,
    labels: TurntableLabels,
    onPlayPause: () -> Unit,
    onRepeat: () -> Unit,
    record: @Composable (Modifier) -> Unit,
    height: Dp = size
) {
    val mechanismSize = minOf(size, height)
    val shape = RoundedCornerShape(mechanismSize * 0.065f)
    Box(
        Modifier.size(size, height).testTag("vinyl-deck")
            .shadow(7.dp, shape, clip = false)
            .drawWithCache {
                val painter = VinylDeckPainter(this.size)
                onDrawBehind { with(painter) { drawDeck(isPlaying, repeatMode) } }
            }
    ) {
        record(Modifier.offset((size - mechanismSize) / 2 + mechanismSize * 0.105f,
            (height - mechanismSize) / 2 + mechanismSize * 0.105f).size(mechanismSize * 0.79f))
        VinylTonearm(
            isPlaying = isPlaying && isVisible, hasSong = hasSong,
            positionMsState = positionMsState, durationMs = durationMs,
            modifier = Modifier.size(mechanismSize).align(Alignment.Center)
        )
        val playLabel = if (isPlaying) labels.pause else labels.play
        DeckControl(
            name = "${labels.turntable}: $playLabel", state = playLabel,
            onClick = onPlayPause,
            modifier = Modifier.offset(
                (size * 0.075f - 24.dp).coerceIn(0.dp, size - 48.dp),
                (height * 0.86f - 28.dp).coerceIn(0.dp, height - 56.dp)
            )
        )
        DeckControl(
            name = "${labels.turntable}: ${labels.repeat}",
            state = when (repeatMode) { 1 -> labels.repeatOne; 2 -> labels.repeatAll; else -> labels.repeatOff },
            enabled = hasSong, onClick = onRepeat,
            modifier = Modifier.offset(
                (size * 0.90f - 24.dp).coerceIn(0.dp, size - 48.dp),
                (height * 0.86f - 28.dp).coerceIn(0.dp, height - 56.dp)
            )
        )
    }
}

@Composable
private fun DeckControl(name: String, state: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = modifier.size(48.dp, 56.dp).onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = name; stateDescription = state },
        shape = RoundedCornerShape(7.dp), color = Color.Transparent,
        border = if (focused) BorderStroke(2.dp, Color(0xFF171B1A)) else null
    ) { Box(Modifier.fillMaxSize()) }
}

private class VinylDeckPainter(size: Size) {
    private val width = size.width
    private val height = size.height
    private val edge = size.minDimension
    private val corner = CornerRadius(edge * 0.065f)
    private val body = Brush.linearGradient(
        listOf(Color(0xFFD2D5D5), Color(0xFFBFC4C5), Color(0xFFA5ABAD)),
        Offset.Zero, Offset(width, height)
    )
    private val rim = Brush.linearGradient(
        listOf(Color(0xFFEDEFEE), Color(0xFF878D90), Color(0xFFBCC1C2), Color(0xFF777E83)),
        Offset(edge * 0.2f, edge * 0.06f), Offset(edge * 0.8f, edge * 0.94f)
    )
    private val center = Offset(width * 0.5f, height * 0.5f)
    private val well = Brush.radialGradient(
        0f to Color(0xFF111416), 0.88f to Color(0xFF292D30),
        0.94f to Color(0xFF747B80), 0.98f to Color(0xFFC0C5C7), 1f to Color(0xFF6D757B),
        center = center, radius = edge * 0.444f
    )
    private val grain = Random(741).let { random ->
        List(900) { Offset(random.nextFloat() * width, random.nextFloat() * height) }
    }
    private val knob = Brush.horizontalGradient(
        listOf(Color(0xFF292D30), Color(0xFF7A8083), Color(0xFF30363A)),
        width * 0.075f - edge * 0.015f, width * 0.075f + edge * 0.015f
    )

    fun DrawScope.drawDeck(playing: Boolean, repeatMode: Int) {
        drawRoundRect(Color(0xFF898F93), cornerRadius = corner)
        drawRoundRect(body, topLeft = Offset(0f, -edge * 0.006f), cornerRadius = corner)
        drawRoundRect(Color.White.copy(alpha = 0.28f),
            topLeft = Offset(edge * 0.003f, edge * 0.003f),
            size = Size(width - edge * 0.006f, height - edge * 0.013f), cornerRadius = corner,
            style = Stroke(edge * 0.003f))
        for ((index, point) in grain.withIndex()) {
            // Keep grain inside the rounded shell without an offscreen blur layer.
            if (point.x in edge * 0.025f..width - edge * 0.025f && point.y in edge * 0.025f..height - edge * 0.025f) {
                drawCircle(if (index % 2 == 0) Color.White.copy(alpha = 0.095f)
                    else Color(0xFF343D43).copy(alpha = 0.07f), edge * 0.0009f, point)
            }
        }
        drawCircle(rim, edge * 0.45f, center)
        drawCircle(well, edge * 0.444f, center)
        drawCircle(Color(0xFF30363B), edge * 0.414f, center)
        drawCircle(Color.Black.copy(alpha = 0.45f), edge * 0.405f, center + Offset(0f, edge * 0.002f))

        val screw = Offset(width * 0.072f, height * 0.078f)
        drawCircle(Color(0xFF7C858B), edge * 0.009f, screw)
        drawCircle(Color(0xFFDEE1E2), edge * 0.0065f, screw)
        drawLine(Color(0xFF6A747C), screw - Offset(edge * 0.004f, 0f),
            screw + Offset(edge * 0.004f, 0f), edge * 0.002f)

        val power = Offset(width * 0.075f, height * 0.86f)
        drawCircle(Color(0xFF6B747A), edge * 0.017f, Offset(power.x, height * 0.75f))
        drawCircle(if (playing) Color(0xFFDBD8AD) else Color(0xFF444C53), edge * 0.010f,
            Offset(power.x, height * 0.75f))
        drawRoundRect(Color(0xFF3C4349), power - Offset(edge * 0.016f, edge * 0.054f),
            Size(edge * 0.032f, edge * 0.111f), CornerRadius(edge * 0.012f))
        val lever = Path().apply {
            moveTo(power.x - edge * 0.012f, power.y - edge * 0.046f)
            quadraticTo(power.x, power.y - edge * 0.056f, power.x + edge * 0.012f, power.y - edge * 0.046f)
            lineTo(power.x + edge * 0.006f, power.y + edge * 0.045f)
            quadraticTo(power.x, power.y + edge * 0.057f, power.x - edge * 0.006f, power.y + edge * 0.045f)
            close()
        }
        drawPath(lever, knob)
        val glyphY = power.y + if (playing) -edge * 0.024f else edge * 0.018f
        val glyphColor = Color(0xFFE3E6E7)
        if (playing) {
            for (x in listOf(-0.004f, 0.004f)) drawLine(glyphColor,
                Offset(power.x + edge * x, glyphY - edge * 0.007f),
                Offset(power.x + edge * x, glyphY + edge * 0.007f), edge * 0.003f, StrokeCap.Round)
        } else {
            drawPath(Path().apply {
                moveTo(power.x - edge * 0.004f, glyphY - edge * 0.007f)
                lineTo(power.x + edge * 0.006f, glyphY)
                lineTo(power.x - edge * 0.004f, glyphY + edge * 0.007f); close()
            }, glyphColor)
        }

        val rocker = Offset(width * 0.90f, height * 0.86f)
        drawRoundRect(Color(0xFF4E5860), rocker - Offset(edge * 0.023f, edge * 0.069f),
            Size(edge * 0.046f, edge * 0.138f), CornerRadius(edge * 0.009f))
        drawRoundRect(Color(0xFF292F34), rocker - Offset(edge * 0.018f, edge * 0.062f),
            Size(edge * 0.036f, edge * 0.124f), CornerRadius(edge * 0.005f))
        val thumbY = rocker.y + edge * when (repeatMode) { 2 -> -0.04f; 1 -> 0f; else -> 0.04f }
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF8A9298), Color(0xFF58616A), Color(0xFF222A31)),
            thumbY - edge * 0.024f, thumbY + edge * 0.024f),
            Offset(rocker.x - edge * 0.019f, thumbY - edge * 0.024f),
            Size(edge * 0.038f, edge * 0.048f), CornerRadius(edge * 0.004f))
        drawLine(Color(0xFFB7BEC2), Offset(rocker.x - edge * 0.013f, thumbY - edge * 0.017f),
            Offset(rocker.x + edge * 0.013f, thumbY - edge * 0.017f), edge * 0.002f)
        for (index in 0..2) {
            drawCircle(if (index == when (repeatMode) { 2 -> 0; 1 -> 1; else -> 2 }) Color(0xFF323D47)
                else Color(0xFF707B83), edge * 0.004f,
                Offset(rocker.x + edge * 0.038f, rocker.y + (index - 1) * edge * 0.046f))
        }
    }
}
