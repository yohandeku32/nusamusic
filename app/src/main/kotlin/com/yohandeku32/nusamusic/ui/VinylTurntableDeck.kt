package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    height: Dp = size,
    theme: VinylMachineTheme = VinylMachineTheme.CLASSIC
) {
    val mechanismSize = minOf(size, height)
    Box(
        Modifier.size(size, height).testTag("vinyl-deck")
            .drawWithCache {
                val painter = VinylDeckPainter(this.size, theme)
                val ambient = vinylShadowPaint(26.dp.toPx(), 29)
                val cast = vinylShadowPaint(18.dp.toPx(), 70)
                val contact = vinylShadowPaint(3.dp.toPx(), 41)
                val radius = mechanismSize.toPx() * if (theme == VinylMachineTheme.STUDIO_BLACK) 0.03f else 0.065f
                val padding = 52.dp.toPx()
                val bounds = android.graphics.RectF(-padding, -padding,
                    this.size.width + padding, this.size.height + padding)
                val shadow = vinylShadowLayer(kotlin.math.ceil(bounds.width() / 2).toInt(),
                    kotlin.math.ceil(bounds.height() / 2).toInt()) { canvas ->
                    canvas.scale(0.5f, 0.5f)
                    canvas.translate(padding, padding)
                    canvas.drawRoundRect(0f, 12.dp.toPx(),
                        this.size.width, this.size.height + 12.dp.toPx(), radius, radius, ambient)
                    canvas.drawRoundRect(8.dp.toPx(), 18.dp.toPx(),
                        this.size.width + 8.dp.toPx(), this.size.height + 18.dp.toPx(), radius, radius, cast)
                    canvas.drawRoundRect(0f, 3.dp.toPx(), this.size.width,
                        this.size.height + 3.dp.toPx(), radius, radius, contact)
                }
                onDrawBehind {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawBitmap(shadow, null, bounds, null)
                    }
                    with(painter) { drawDeck(isPlaying, repeatMode) }
                }
            }
    ) {
        record(Modifier.offset((size - mechanismSize) / 2 + mechanismSize * 0.105f,
            (height - mechanismSize) / 2 + mechanismSize * 0.105f).size(mechanismSize * 0.79f))
        VinylTonearm(
            isPlaying = isPlaying && isVisible, hasSong = hasSong,
            positionMsState = positionMsState, durationMs = durationMs,
            modifier = Modifier.size(mechanismSize).align(Alignment.Center), theme = theme
        )
        val playLabel = if (isPlaying) labels.pause else labels.play
        DeckControl(
            name = "${labels.turntable}: $playLabel", state = playLabel,
            onClick = onPlayPause,
            focusColor = if (theme == VinylMachineTheme.STUDIO_BLACK) Color(0xFFE4E7E9) else Color(0xFF171B1A),
            modifier = Modifier.offset(
                (size * 0.075f - 24.dp).coerceIn(0.dp, size - 48.dp),
                (height * 0.86f - 28.dp).coerceIn(0.dp, height - 56.dp)
            )
        )
        DeckControl(
            name = "${labels.turntable}: ${labels.repeat}",
            state = when (repeatMode) { 1 -> labels.repeatOne; 2 -> labels.repeatAll; else -> labels.repeatOff },
            enabled = hasSong, onClick = onRepeat,
            focusColor = if (theme == VinylMachineTheme.STUDIO_BLACK) Color(0xFFE4E7E9) else Color(0xFF171B1A),
            modifier = Modifier.offset(
                (size * 0.90f - 24.dp).coerceIn(0.dp, size - 48.dp),
                (height * 0.86f - 28.dp).coerceIn(0.dp, height - 56.dp)
            )
        )
    }
}

@Composable
private fun DeckControl(name: String, state: String, modifier: Modifier, enabled: Boolean = true,
    focusColor: Color = Color(0xFF171B1A), onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = modifier.size(48.dp, 56.dp).onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = name; stateDescription = state },
        shape = RoundedCornerShape(7.dp), color = Color.Transparent,
        border = if (focused) BorderStroke(2.dp, focusColor) else null
    ) { Box(Modifier.fillMaxSize()) }
}

internal fun vinylShadowPaint(blur: Float, alpha: Int) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.argb(alpha, 12, 14, 15)
    maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
}

// Blur into a software bitmap once; hardware Canvas does not support Paint mask filters.
internal fun vinylShadowLayer(width: Int, height: Int, draw: (android.graphics.Canvas) -> Unit): android.graphics.Bitmap {
    return android.graphics.Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1),
        android.graphics.Bitmap.Config.ARGB_8888).apply {
        density = android.graphics.Bitmap.DENSITY_NONE
        draw(android.graphics.Canvas(this))
    }
}

private class VinylDeckPainter(size: Size, theme: VinylMachineTheme) {
    private val studio = theme == VinylMachineTheme.STUDIO_BLACK
    private val width = size.width
    private val height = size.height
    private val edge = size.minDimension
    private val corner = CornerRadius(edge * if (studio) 0.03f else 0.065f)
    private val body = Brush.linearGradient(
        if (studio) listOf(Color(0xFF343638), Color(0xFF242628), Color(0xFF181A1C))
        else listOf(Color(0xFFD4D6D4), Color(0xFFBFC2C0), Color(0xFFA9ADAA)),
        Offset.Zero, Offset(width, height)
    )
    private val rim = Brush.linearGradient(
        if (studio) listOf(Color(0xFF777C80), Color(0xFF272B2E), Color(0xFF555B60), Color(0xFF181B1E))
        else listOf(Color(0xFFEDEFEE), Color(0xFF878D90), Color(0xFFBCC1C2), Color(0xFF777E83)),
        Offset(edge * 0.2f, edge * 0.06f), Offset(edge * 0.8f, edge * 0.94f)
    )
    private val center = Offset(width * 0.5f, height * 0.5f)
    private val well = Brush.radialGradient(
        0f to Color(0xFF111416), 0.88f to Color(0xFF292D30),
        0.94f to (if (studio) Color(0xFF3E454A) else Color(0xFF747B80)),
        0.98f to (if (studio) Color(0xFF777E83) else Color(0xFFC0C5C7)),
        1f to (if (studio) Color(0xFF292F33) else Color(0xFF6D757B)),
        center = center, radius = edge * 0.444f
    )
    private val grain = android.graphics.Paint().apply {
        val random = kotlin.random.Random(741)
        val pixels = IntArray(128 * 128) {
            val ink = if (random.nextBoolean()) 255 else 0
            android.graphics.Color.argb(if (studio) random.nextInt(2, 7) else random.nextInt(3, 19), ink, ink, ink)
        }
        val bitmap = android.graphics.Bitmap.createBitmap(pixels, 128, 128, android.graphics.Bitmap.Config.ARGB_8888)
        shader = android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.REPEAT,
            android.graphics.Shader.TileMode.REPEAT)
    }
    private val knob = Brush.horizontalGradient(
        listOf(Color(0xFF292D30), Color(0xFF7A8083), Color(0xFF30363A)),
        width * 0.075f - edge * 0.015f, width * 0.075f + edge * 0.015f
    )
    private val lamp = if (studio) Offset(width * 0.14f, height * 0.86f)
        else Offset(width * 0.075f, height * 0.75f)
    private val lampRim = Brush.linearGradient(
        listOf(Color(0xFFD0D1C9), Color(0xFF74756C), Color(0xFF383B35)),
        lamp - Offset(edge * 0.012f, edge * 0.016f), lamp + Offset(edge * 0.014f, edge * 0.016f))
    private val litLens = Brush.radialGradient(
        0f to Color(0xFFFF7867), 0.35f to Color(0xFFFF2119),
        0.7f to Color(0xFFE30808), 1f to Color(0xFF850500),
        center = lamp - Offset(edge * 0.003f, edge * 0.003f), radius = edge * 0.017f)
    private val idleLens = Brush.radialGradient(
        listOf(Color(0xFFEB2520), Color(0xFFC40908), Color(0xFF730805)),
        center = lamp - Offset(edge * 0.003f, edge * 0.003f), radius = edge * 0.017f)
    private val litReflection = Brush.radialGradient(
        listOf(Color(0xFFFF2416).copy(alpha = 0.28f), Color.Transparent),
        center = lamp, radius = edge * 0.036f)
    private val idleReflection = Brush.radialGradient(
        listOf(Color(0xFFD61C12).copy(alpha = 0.09f), Color.Transparent),
        center = lamp, radius = edge * 0.032f)

    fun DrawScope.drawDeck(playing: Boolean, repeatMode: Int) {
        drawRoundRect(if (studio) Color(0xFF111315) else Color(0xFF898F93), cornerRadius = corner)
        if (studio) drawRoundRect(body, topLeft = Offset(0f, -edge * 0.006f),
            size = Size(width, height - edge * 0.018f), cornerRadius = corner)
        else drawRoundRect(body, topLeft = Offset(0f, -edge * 0.006f), cornerRadius = corner)
        drawRoundRect(Color.White.copy(alpha = if (studio) 0.12f else 0.28f),
            topLeft = Offset(edge * 0.003f, edge * 0.003f),
            size = Size(width - edge * 0.006f, height - edge * 0.013f), cornerRadius = corner,
            style = Stroke(edge * 0.003f))
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(0f, 0f, width, height - edge * 0.006f,
                corner.x, corner.y, grain)
        }
        if (studio) {
            drawRoundRect(Color.Black.copy(alpha = 0.6f), Offset(edge * 0.013f, edge * 0.013f),
                Size(width - edge * 0.026f, height - edge * 0.044f), CornerRadius(edge * 0.018f),
                style = Stroke(edge * 0.003f))
        }
        drawCircle(rim, edge * 0.45f, center)
        drawCircle(well, edge * 0.444f, center)
        drawCircle(Color(0xFF30363B), edge * 0.414f, center)
        drawCircle(Color.Black.copy(alpha = 0.45f), edge * 0.405f, center + Offset(0f, edge * 0.002f))

        val screw = Offset(width * 0.072f, height * 0.078f)
        if (studio) drawStudioDial(screw + Offset(edge * 0.015f, edge * 0.012f), edge * 0.039f, -65f)
        else {
            drawCircle(Color(0xFF7C858B), edge * 0.009f, screw)
            drawCircle(Color(0xFFDEE1E2), edge * 0.0065f, screw)
            drawLine(Color(0xFF6A747C), screw - Offset(edge * 0.004f, 0f),
                screw + Offset(edge * 0.004f, 0f), edge * 0.002f)
        }

        val power = Offset(width * 0.075f, height * 0.86f)
        drawCircle(if (playing) litReflection else idleReflection, edge * 0.036f, lamp)
        drawCircle(Color.Black.copy(alpha = 0.22f), edge * 0.017f, lamp + Offset(0f, edge * 0.002f))
        drawCircle(lampRim, edge * 0.017f, lamp)
        drawCircle(Color(0xFF4C130E), edge * 0.013f, lamp)
        drawCircle(if (playing) litLens else idleLens, edge * 0.0115f, lamp)
        drawOval(Color(0xFFFFE4D8).copy(alpha = if (playing) 0.72f else 0.38f),
            lamp - Offset(edge * 0.006f, edge * 0.007f), Size(edge * 0.008f, edge * 0.0035f))
        if (studio) {
            drawCircle(Color(0xFF080A0B), edge * 0.023f, power + Offset(0f, edge * 0.003f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFABB0B4), Color(0xFF454C52), Color(0xFF14181B)),
                center = power - Offset(edge * 0.006f, edge * 0.006f), radius = edge * 0.03f), edge * 0.023f, power)
            val tip = power + Offset(0f, if (playing) -edge * 0.032f else edge * 0.025f)
            drawLine(Color(0xFF1D2226), power, tip, edge * 0.012f, StrokeCap.Round)
            drawLine(Color(0xFFD1D5D7), power - Offset(edge * 0.002f, 0f),
                tip - Offset(edge * 0.002f, 0f), edge * 0.005f, StrokeCap.Round)
            val dial = Offset(width * 0.90f, height * 0.86f)
            drawStudioDial(dial, edge * 0.032f, when (repeatMode) { 1 -> -90f; 2 -> -35f; else -> -145f })
            for (index in 0..2) {
                val angle = (-145 + index * 55) * kotlin.math.PI.toFloat() / 180f
                val point = dial + Offset(kotlin.math.cos(angle), kotlin.math.sin(angle)) * (edge * 0.048f)
                drawCircle(Color(0xFFB0B6BA), edge * 0.003f, point)
            }
            return
        }
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

    private fun DrawScope.drawStudioDial(center: Offset, radius: Float, angle: Float) {
        drawCircle(Color.Black.copy(alpha = 0.55f), radius * 1.12f, center + Offset(0f, radius * 0.13f))
        drawCircle(Brush.sweepGradient(listOf(Color(0xFF555C61), Color(0xFF15191C),
            Color(0xFF787E82), Color(0xFF252A2E), Color(0xFF555C61)), center), radius, center)
        drawCircle(Color(0xFF15191C).copy(alpha = 0.3f), radius * 0.77f, center, style = Stroke(radius * 0.05f))
        val radians = angle * kotlin.math.PI.toFloat() / 180f
        val direction = Offset(kotlin.math.cos(radians), kotlin.math.sin(radians))
        drawLine(Color(0xFFC7CBCE), center + direction * (radius * 0.65f),
            center + direction * (radius * 0.88f), (radius * 0.07f).coerceAtLeast(0.7f), StrokeCap.Round)
    }
}
