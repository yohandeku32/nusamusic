package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

data class TurntableLabels(
    val play: String,
    val pause: String,
    val previous: String,
    val next: String,
    val shuffle: String,
    val shuffleOn: String,
    val shuffleOff: String,
    val repeat: String,
    val repeatOff: String,
    val repeatAll: String,
    val repeatOne: String,
    val seek: String
)

/** The caller supplies real playback state and artwork; the deck owns presentation only. */
@Composable
fun TurntablePlayer(
    title: String,
    artist: String,
    titleFontFamily: FontFamily,
    titleFontSize: Float,
    viewportHeight: Dp,
    isPlaying: Boolean,
    isVisible: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    realisticControls: Boolean,
    labels: TurntableLabels,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    record: @Composable (Modifier) -> Unit,
    quality: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    emptyState: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val page = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val edge = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    BoxWithConstraints(modifier.background(page)) {
        // Keep transport controls in view on compact phones; extra actions can
        // continue below in the parent list when text scaling needs more room.
        val deckSize = minOf((maxWidth - 32.dp).coerceAtLeast(0.dp), (viewportHeight - 392.dp).coerceAtLeast(160.dp))
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Retain the upper safe spacing without back/settings buttons.
                Spacer(Modifier.height(52.dp))

                Box(
                    modifier = Modifier.size(deckSize)
                        // The connectors sit on the REAR EDGE and protrude behind the chassis.
                        // Draw them outside the rounded clip; the deck covers their lower halves.
                        .drawWithCache {
                            val edgeY = -4.dp.toPx()
                            val pinHeight = 6.dp.toPx()
                            val pinWidth = 3.2.dp.toPx()
                            val blackWidth = 8.dp.toPx()
                            val rearBlack = Brush.verticalGradient(
                                colors = listOf(Color(0xFF686C6E), Color(0xFF111518)),
                                startY = edgeY,
                                endY = 2.dp.toPx()
                            )
                            val rearGold = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF80591F), Color(0xFFEAC16D), Color(0xFFAA762A))
                            )
                            onDrawBehind {
                                // Short real-looking leads curve away BEHIND the upper deck rim.
                                // Keep the cables outside the record/artwork area.
                                val dcX = size.width * 0.405f
                                val dcCable = Path().apply {
                                    moveTo(dcX, edgeY)
                                    cubicTo(
                                        dcX - 1.dp.toPx(), -13.dp.toPx(),
                                        dcX - 15.dp.toPx(), -19.dp.toPx(),
                                        dcX - 27.dp.toPx(), -31.dp.toPx()
                                    )
                                }
                                drawPath(
                                    dcCable, Color.Black.copy(alpha = 0.17f),
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawPath(
                                    dcCable, Color(0xFF202629),
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawPath(
                                    dcCable, Color(0xFF7A8386).copy(alpha = 0.38f),
                                    style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round)
                                )

                                listOf(0.535f, 0.575f).forEachIndexed { index, fraction ->
                                    val x = size.width * fraction
                                    val bend = (15 + index * 8).dp.toPx()
                                    val rcaCable = Path().apply {
                                        moveTo(x, -pinHeight)
                                        cubicTo(
                                            x + 2.dp.toPx(), -13.dp.toPx(),
                                            x + bend * 0.65f, -20.dp.toPx(),
                                            x + bend, -32.dp.toPx()
                                        )
                                    }
                                    drawPath(
                                        rcaCable, Color.Black.copy(alpha = 0.14f),
                                        style = Stroke(width = 4.4.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawPath(
                                        rcaCable, Color(0xFF35393C),
                                        style = Stroke(width = 2.7.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawPath(
                                        rcaCable, Color(0xFF90969A).copy(alpha = 0.35f),
                                        style = Stroke(width = 0.55.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                // Low-profile black power connector, partially hidden by the rim.
                                drawRoundRect(
                                    brush = rearBlack,
                                    topLeft = Offset(size.width * 0.405f - blackWidth / 2f, edgeY),
                                    size = Size(blackWidth, 6.dp.toPx()),
                                    cornerRadius = CornerRadius(1.dp.toPx())
                                )
                                // Two metallic contact pins emerging from the back edge.
                                listOf(0.535f, 0.575f).forEach { fraction ->
                                    val x = size.width * fraction
                                    drawRoundRect(
                                        color = Color(0xFF3A3227),
                                        topLeft = Offset(x - 2.6.dp.toPx(), -1.5.dp.toPx()),
                                        size = Size(5.2.dp.toPx(), 3.5.dp.toPx()),
                                        cornerRadius = CornerRadius(0.7.dp.toPx())
                                    )
                                    drawRoundRect(
                                        brush = rearGold,
                                        topLeft = Offset(x - pinWidth / 2f, -pinHeight),
                                        size = Size(pinWidth, pinHeight + 1.dp.toPx()),
                                        cornerRadius = CornerRadius(0.8.dp.toPx())
                                    )
                                }
                            }
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(surface, page, surface)))
                        .border(1.dp, edge, RoundedCornerShape(18.dp))
                        .drawWithCache {
                            onDrawBehind {
                                // Small countersunk fittings remain on the deck surface.
                                listOf(
                                    Offset(size.width * 0.075f, size.height * 0.075f),
                                    Offset(size.width * 0.77f, size.height * 0.075f)
                                ).forEach { screw ->
                                    drawCircle(edge, 2.dp.toPx(), screw)
                                    drawCircle(surface, 1.dp.toPx(), screw - Offset(0.4f, 0.4f))
                                }
                            }
                        }
                ) {
                    record(Modifier.size(deckSize * 0.78f).offset(x = deckSize * 0.10f, y = deckSize * 0.12f))
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DeckSwitch(
                            icon = Icons.Default.Shuffle,
                            label = labels.shuffle,
                            state = if (shuffleEnabled) labels.shuffleOn else labels.shuffleOff,
                            active = shuffleEnabled,
                            enabled = hasSong,
                            onClick = onShuffle
                        )
                        DeckSwitch(
                            icon = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            label = labels.repeat,
                            state = when (repeatMode) {
                                1 -> labels.repeatOne
                                2 -> labels.repeatAll
                                else -> labels.repeatOff
                            },
                            active = repeatMode != 0,
                            enabled = hasSong,
                            onClick = onRepeat
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    fontFamily = titleFontFamily,
                    fontSize = (titleFontSize * 0.68f).sp,
                    lineHeight = (titleFontSize * 0.79f).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = artist,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                )
                Spacer(Modifier.height(8.dp))
                quality()
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TurntableProgress(positionMsState, durationMs, hasSong, labels.seek, onSeek)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TurntableTransportButton(
                        Icons.Default.SkipPrevious, labels.previous, onPrevious, hasSong,
                        realisticControls, Modifier.weight(1f).height(52.dp), 21.dp
                    )
                    TurntableTransportButton(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) labels.pause else labels.play,
                        onPlayPause, true, realisticControls, Modifier.size(76.dp), 32.dp, primary = true
                    )
                    TurntableTransportButton(
                        Icons.Default.SkipNext, labels.next, onNext, hasSong,
                        realisticControls, Modifier.weight(1f).height(52.dp), 21.dp
                    )
                }
                footer()
                emptyState()
            }
        }
    }
}

@Composable
private fun DeckSwitch(icon: ImageVector, label: String, state: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { stateDescription = state }
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .border(0.7.dp, ink.copy(alpha = if (active) 0.65f else 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, Modifier.size(16.dp), tint = ink.copy(alpha = if (enabled) 0.85f else 0.35f))
        }
    }
}

@Composable
private fun TurntableTransportButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    realistic: Boolean,
    modifier: Modifier,
    iconSize: Dp,
    primary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(130), label = "turntableButtonPress")
    val face = MaterialTheme.colorScheme.surface
    val shade = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(50),
        color = if (primary) Color.Black else if (realistic) face else shade,
        border = if (!primary && realistic) BorderStroke(1.dp, ink.copy(alpha = 0.13f)) else null,
        shadowElevation = if (realistic && !pressed) 4.dp else 0.dp,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Box(
            modifier = Modifier.padding(if (realistic && !primary) 4.dp else 0.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    when {
                        primary -> Brush.linearGradient(listOf(Color.Black, Color.Black))
                        realistic -> Brush.linearGradient(listOf(face, lerp(face, shade, 0.35f), face))
                        else -> Brush.linearGradient(listOf(shade, shade))
                    }
                )
                .border(if (!primary && realistic) 1.dp else 0.dp, ink.copy(alpha = if (!primary && realistic) 0.06f else 0f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, Modifier.size(iconSize), tint = if (primary) Color.White else ink.copy(alpha = if (enabled) 1f else 0.35f))
        }
    }
}

@Composable
private fun PlaybackActivity(active: Boolean) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            phase.snapTo(0f)
            phase.animateTo(
                (2 * PI).toFloat(),
                infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart)
            )
        }
    }
    val ink = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Canvas(Modifier.size(width = 60.dp, height = 30.dp)) {
        // A playback activity animation, independent of the music's audio spectrum.
        for (index in 0..5) {
            val amplitude = 0.30f + 0.55f * ((sin(phase.value + index * 1.9f) + 1f) / 2f)
            val halfHeight = size.height * amplitude / 2f
            val x = (index + 0.5f) * size.width / 6f
            drawLine(ink, Offset(x, size.height / 2f - halfHeight), Offset(x, size.height / 2f + halfHeight), 2.5.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun TurntableProgress(positionMsState: State<Long>, durationMs: Long, hasSong: Boolean, label: String, onSeek: (Long) -> Unit) {
    val positionMs by positionMsState
    val enabled = hasSong && durationMs > 0L
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val ink = MaterialTheme.colorScheme.onBackground
    fun time(ms: Long): String {
        val seconds = ms.coerceAtLeast(0L) / 1000L
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
    Canvas(
        Modifier.fillMaxWidth().padding(horizontal = 38.dp).height(36.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (!enabled) disabled()
                else setProgress { value ->
                    if (!value.isFinite()) false else {
                        onSeek((value.coerceIn(0f, 1f) * durationMs).toLong())
                        true
                    }
                }
            }
            .pointerInput(enabled, durationMs, onSeek) {
                if (enabled) detectTapGestures { offset ->
                    onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                }
            }
            .pointerInput(enabled, durationMs, onSeek) {
                if (enabled) detectHorizontalDragGestures(
                    onDragStart = { offset -> onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong()) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeek(((change.position.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                    }
                )
            }
    ) {
        val stroke = 5.dp.toPx()
        val start = Offset(stroke / 2f, size.height / 2f)
        val end = Offset((size.width - stroke / 2f).coerceAtLeast(start.x), start.y)
        drawLine(ink.copy(alpha = 0.17f), start, end, stroke, StrokeCap.Round)
        if (fraction > 0f) drawLine(ink.copy(alpha = 0.65f), start, Offset(start.x + (end.x - start.x) * fraction, start.y), stroke, StrokeCap.Round)
    }
    // Restore the elapsed / duration clock directly BELOW the seek bar.
    Text(
        "${time(positionMs)} / ${time(durationMs)}",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = ink
    )
}
