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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Vinyl playback surface inspired by the approved black/red mobile prototype. */
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
    accentColor: Color,
    sleeve: @Composable (Modifier) -> Unit,
    quality: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    emptyState: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val page = Color(0xFF0B0B0F)
    val panel = Color(0xFF111115)
    val accent = accentColor
    // Scope light-colored controls and pre-existing callback content to Vinyl only.
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            background = page,
            onBackground = Color(0xFFF6F6F8),
            surface = panel,
            onSurface = Color(0xFFF6F6F8),
            surfaceVariant = Color(0xFF222229),
            onSurfaceVariant = Color(0xFFB6B8C1),
            primary = accent,
            onPrimary = Color.White
        )
    ) {
        BoxWithConstraints(modifier.fillMaxWidth().background(page)) {
            // Wider than the panel's inner content, but constrained on short devices.
            val discSize = minOf(
                (maxWidth * 1.12f).coerceAtLeast(0.dp),
                (viewportHeight * 0.60f).coerceAtLeast(240.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth().height(viewportHeight)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // No brand or mode labels above the record: all attention goes to the disc.
                Spacer(Modifier.height(22.dp))

                // A slightly lowered, oversize disc peeks out by roughly half its diameter.
                // The rest remains hidden under the dark playback console.
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(discSize * 0.55f)
                        .clipToBounds(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    record(Modifier.size(discSize).offset(y = discSize * 0.07f))
                }

                // A stationary dark control surface conceals the lower vinyl.
                // It replaces the previous paper sleeve and does not rotate.
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    androidx.compose.ui.graphics.lerp(Color(0xFF17171D), accent, 0.075f),
                                    panel,
                                    Color(0xFF08080B)
                                )
                            )
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VinylWaveform(
                        accentColor = accent,
                        isPlaying = isPlaying,
                        isVisible = isVisible,
                        hasSong = hasSong,
                        positionMsState = positionMsState,
                        durationMs = durationMs,
                        modifier = Modifier.fillMaxWidth().height(108.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = title,
                        fontFamily = titleFontFamily,
                        fontSize = (titleFontSize * 0.78f).coerceIn(18f, 27f).sp,
                        lineHeight = (titleFontSize * 0.88f).coerceIn(22f, 31f).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = artist,
                        color = Color(0xFFA3A3AE),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.align(Alignment.Start)) { quality() }
                    Spacer(Modifier.height(14.dp))

                    TurntableProgress(
                        positionMsState, durationMs, hasSong, labels.seek, onSeek,
                        accentColor = accent
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeckSwitch(
                            Icons.Default.Shuffle, labels.shuffle,
                            if (shuffleEnabled) labels.shuffleOn else labels.shuffleOff,
                            shuffleEnabled, hasSong, onShuffle, accent
                        )
                        TurntableTransportButton(
                            Icons.Default.SkipPrevious, labels.previous, onPrevious, hasSong,
                            realisticControls, Modifier.size(48.dp), 25.dp, accent
                        )
                        TurntableTransportButton(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) labels.pause else labels.play,
                            onPlayPause, true,
                            realisticControls, Modifier.size(70.dp), 30.dp, accent, primary = true
                        )
                        TurntableTransportButton(
                            Icons.Default.SkipNext, labels.next, onNext, hasSong,
                            realisticControls, Modifier.size(48.dp), 25.dp, accent
                        )
                        DeckSwitch(
                            if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            labels.repeat,
                            when (repeatMode) {
                                1 -> labels.repeatOne
                                2 -> labels.repeatAll
                                else -> labels.repeatOff
                            },
                            repeatMode != 0, hasSong, onRepeat, accent
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    footer()
                    emptyState()
                }
            }
        }
    }
}

/**
 * Lightweight visualizer-style movement, not a fabricated representation
 * of the song's actual decoded frequency spectrum.
 */
@Composable
private fun VinylWaveform(
    accentColor: Color,
    isPlaying: Boolean,
    isVisible: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, isVisible, hasSong) {
        if (isPlaying && isVisible && hasSong) {
            phase.snapTo(0f)
            phase.animateTo(
                (2 * PI).toFloat(),
                infiniteRepeatable(tween(2700, easing = LinearEasing), RepeatMode.Restart)
            )
        } else {
            phase.stop()
            phase.snapTo(0f)
        }
    }
    val position by positionMsState
    val fraction = if (durationMs > 0L) {
        (position.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f
    val accent = accentColor
    Canvas(modifier) {
        val count = 70
        val step = size.width / count
        val center = size.height / 2f
        for (i in 0 until count) {
            val x = (i + 0.5f) * step
            val envelope = 0.14f + 0.86f *
                kotlin.math.abs(sin(i * 0.109f) * sin(i * 0.047f + 0.7f))
            val pulse = if (isPlaying && isVisible && hasSong) {
                0.82f + 0.18f * sin(phase.value + i * 0.22f)
            } else 0.82f
            val half = (size.height * 0.44f * envelope * pulse).coerceAtLeast(1.5f)
            val nearPlayhead = kotlin.math.abs((i + 0.5f) / count - fraction) < 0.037f
            drawLine(
                color = if (nearPlayhead) accent else Color(0xFFE8E8EF).copy(alpha = 0.79f),
                start = Offset(x, center - half),
                end = Offset(x, center + half),
                strokeWidth = (step * 0.31f).coerceAtLeast(1.dp.toPx()),
                cap = StrokeCap.Round
            )
        }
        val cursorX = (fraction * size.width).coerceIn(0f, size.width)
        drawLine(
            accent.copy(alpha = 0.95f),
            Offset(cursorX, size.height * 0.06f),
            Offset(cursorX, size.height * 0.94f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(accent, radius = 3.5.dp.toPx(), center = Offset(cursorX, size.height * 0.06f))
    }
}

@Composable
private fun DeckSwitch(
    icon: ImageVector,
    label: String,
    state: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    accent: Color
) {
    val tint = if (active) accent else Color(0xFFC5C6CE)
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp).semantics { stateDescription = state }
    ) {
        Icon(
            icon, label, Modifier.size(21.dp),
            tint = tint.copy(alpha = if (enabled) 1f else 0.35f)
        )
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
    accent: Color,
    primary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f, tween(140), label = "turntableButtonPress"
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = if (primary) androidx.compose.ui.graphics.lerp(Color(0xFF19191F), accent, 0.18f) else Color.Transparent,
        border = if (primary) BorderStroke(2.dp, accent) else null,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon, label, Modifier.size(iconSize),
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.34f)
            )
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
private fun TurntableProgress(
    positionMsState: State<Long>,
    durationMs: Long,
    hasSong: Boolean,
    label: String,
    onSeek: (Long) -> Unit,
    accentColor: Color
) {
    val positionMs by positionMsState
    val enabled = hasSong && durationMs > 0L
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val ink = Color(0xFFE9E9EF)
    val accent = accentColor
    fun time(ms: Long): String {
        val seconds = ms.coerceAtLeast(0L) / 1000L
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
    Canvas(
        Modifier.fillMaxWidth().padding(horizontal = 38.dp).height(30.dp)
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
        val stroke = 3.dp.toPx()
        val start = Offset(stroke / 2f, size.height / 2f)
        val end = Offset((size.width - stroke / 2f).coerceAtLeast(start.x), start.y)
        drawLine(ink.copy(alpha = 0.20f), start, end, stroke, StrokeCap.Round)
        if (fraction > 0f) drawLine(accent, start, Offset(start.x + (end.x - start.x) * fraction, start.y), stroke, StrokeCap.Round)
    }
    // Compact timestamps at the left and right ends, just below the seek bar.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time(positionMs),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = ink.copy(alpha = 0.70f)
        )
        Text(
            text = time(durationMs),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = ink.copy(alpha = 0.70f)
        )
    }
}
