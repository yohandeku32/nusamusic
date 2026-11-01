package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun StudioBlackTurntableDeck(
    width: Dp, height: Dp, isPlaying: Boolean, hasSong: Boolean,
    repeatMode: Int, labels: TurntableLabels,
    onPlayPause: () -> Unit, onRepeat: () -> Unit, record: @Composable (Modifier) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val assets by produceState(StudioBlackAssetLoader.peek(), context) {
        value = StudioBlackAssetLoader.load(context)
    }
    val unit = minOf(width / 499f, height / 382f)
    val frameWidth = unit * 499f
    val frameHeight = unit * 382f
    val x = (width - frameWidth) / 2
    val y = (height - frameHeight) / 2
    Box(Modifier.size(width, height).testTag("vinyl-deck")) {
        assets?.let { raster ->
            Image(raster.machine, null,
                Modifier.offset(x, y).size(frameWidth, frameHeight).testTag("studio-black-raster"),
                contentScale = ContentScale.FillBounds)
            CompositionLocalProvider(LocalVinylRecordTexture provides raster.record) {
                record(Modifier.offset(x + unit * 47.5f, y - unit * 23f).size(unit * 342f)
                    .studioRecordPerspective())
            }
            Image(raster.foreground, null, Modifier.offset(x, y).size(frameWidth, frameHeight),
                contentScale = ContentScale.FillBounds)
        }
        val focus = Color(0xFFE4E7E9)
        val playLabel = if (isPlaying) labels.pause else labels.play
        DeckControl("${labels.turntable}: $playLabel", playLabel,
            Modifier.offset((x + unit * 48f - 24.dp).coerceIn(0.dp, width - 48.dp),
                (y + unit * 286f - 28.dp).coerceIn(0.dp, height - 56.dp)),
            focusColor = focus, onClick = onPlayPause)
        DeckControl("${labels.turntable}: ${labels.repeat}",
            when (repeatMode) { 1 -> labels.repeatOne; 2 -> labels.repeatAll; else -> labels.repeatOff },
            Modifier.offset((x + unit * 424f - 24.dp).coerceIn(0.dp, width - 48.dp),
                (y + unit * 286f - 28.dp).coerceIn(0.dp, height - 56.dp)),
            hasSong, focus, onRepeat)
    }
}

internal fun Modifier.studioRecordPerspective(): Modifier = drawWithCache {
    val radius = size.width / 2f
    val source = floatArrayOf(0f, 0f, size.width, 0f, size.width, size.height, 0f, size.height)
    val target = FloatArray(8)
    for (index in 0..3) {
        val u = if (index == 0 || index == 3) -1f else 1f
        val v = if (index < 2) -1f else 1f
        val denominator = 1 - 0.102f * v
        target[index * 2] = radius + radius * u / denominator
        target[index * 2 + 1] = radius + radius * (141f / 171f) * v / denominator
    }
    val matrix = android.graphics.Matrix().apply { setPolyToPoly(source, 0, target, 0, 4) }
    onDrawWithContent {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.concat(matrix)
            drawContent()
            canvas.nativeCanvas.restore()
        }
    }
}
