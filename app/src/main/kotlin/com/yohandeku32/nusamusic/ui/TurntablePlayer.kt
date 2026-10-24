package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

/**
 * Vinyl Now Playing mirrors the supplied Pinterest turntable reference:
 * warm paper, an oversized left-clipped record, and a long silver-white tonearm.
 * Player callbacks and existing rotating VinylRecord are supplied by the caller.
 */
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
    val paper = Color(0xFFFAF9F4)
    val ink = Color(0xFF292A29)
    val muted = Color(0xFF898A87)
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            background = paper,
            onBackground = ink,
            surface = paper,
            onSurface = ink,
            surfaceVariant = Color(0xFFF0F0EB),
            onSurfaceVariant = muted,
            primary = ink,
            onPrimary = Color.White
        )
    ) {
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth().height(viewportHeight)
                .background(paper)
                .clipToBounds()
        ) {
            // The record bleeds beyond the left edge, as in the reference.
            // Short phones reserve enough room for usable playback controls below.
            val stageHeight = minOf(
                maxWidth * 1.26f,
                (viewportHeight - 250.dp).coerceAtLeast(maxWidth * 0.78f)
            )
            val discDiameter = maxWidth * 1.31f
            val discX = maxWidth * 0.265f - discDiameter / 2f
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(stageHeight).clipToBounds()
                ) {
                    // Real vinyl and artwork label rotate together; the arm stays on top.
                    record(
                        Modifier.size(discDiameter)
                            .offset(x = discX, y = 28.dp)
                    )
                    VinylTonearm(
                        isPlaying = isPlaying && isVisible,
                        hasSong = hasSong,
                        positionMsState = positionMsState,
                        durationMs = durationMs,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = title,
                    fontFamily = titleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (titleFontSize * 0.78f).coerceIn(22f, 31f).sp,
                    lineHeight = (titleFontSize * 0.91f).coerceIn(27f, 35f).sp,
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = artist,
                    fontSize = 14.sp,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp)
                )
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().padding(horizontal = 30.dp)) { quality() }
                Spacer(Modifier.height(11.dp))
                TurntableProgress(positionMsState, durationMs, hasSong, labels.seek, onSeek)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TurntableModeButton(
                        Icons.Default.Shuffle, labels.shuffle,
                        if (shuffleEnabled) labels.shuffleOn else labels.shuffleOff,
                        shuffleEnabled, hasSong, onShuffle
                    )
                    TurntableButton(
                        Icons.Default.SkipPrevious, labels.previous, onPrevious, hasSong,
                        modifier = Modifier.size(52.dp), size = 25.dp
                    )
                    TurntableButton(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) labels.pause else labels.play,
                        onPlayPause, true,
                        modifier = Modifier.size(68.dp), size = 31.dp, primary = true
                    )
                    TurntableButton(
                        Icons.Default.SkipNext, labels.next, onNext, hasSong,
                        modifier = Modifier.size(52.dp), size = 25.dp
                    )
                    TurntableModeButton(
                        if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        labels.repeat,
                        when (repeatMode) {
                            1 -> labels.repeatOne
                            2 -> labels.repeatAll
                            else -> labels.repeatOff
                        }, repeatMode != 0, hasSong, onRepeat
                    )
                }
                Spacer(Modifier.height(12.dp))
                footer()
                emptyState()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TurntableModeButton(
    icon: ImageVector,
    label: String,
    state: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val ink = Color(0xFF363936)
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp).semantics { stateDescription = state }
    ) {
        Icon(
            icon, label, Modifier.size(21.dp),
            tint = ink.copy(alpha = if (!enabled) 0.3f else if (active) 1f else 0.55f)
        )
    }
}

@Composable
private fun TurntableButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    size: Dp,
    primary: Boolean = false
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f, tween(140), label = "paperVinylButton"
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = CircleShape,
        color = if (primary) Color(0xFF292B2B) else Color.Transparent,
        border = if (primary) BorderStroke(1.dp, Color(0xFF292B2B)) else null,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                label,
                Modifier.size(size),
                tint = if (primary) Color.White
                    else Color(0xFF303332).copy(alpha = if (enabled) 0.9f else 0.3f)
            )
        }
    }
}

@Composable
private fun TurntableProgress(
    positionMsState: State<Long>,
    durationMs: Long,
    hasSong: Boolean,
    label: String,
    onSeek: (Long) -> Unit
) {
    val positionMs by positionMsState
    val enabled = hasSong && durationMs > 0L
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f
    fun time(ms: Long): String {
        val sec = ms.coerceAtLeast(0L) / 1000L
        return "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
    }
    Canvas(
        Modifier.fillMaxWidth().padding(horizontal = 30.dp).height(28.dp)
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
                    onDragStart = { offset ->
                        onSeek(((offset.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeek(((change.position.x / size.width).coerceIn(0f, 1f) * durationMs).toLong())
                    }
                )
            }
    ) {
        val start = Offset(0f, size.height / 2f)
        val end = Offset(size.width, size.height / 2f)
        drawLine(
            Color(0xFF303230).copy(alpha = 0.15f),
            start, end, 3.dp.toPx(), StrokeCap.Round
        )
        if (fraction > 0f) {
            drawLine(
                Color(0xFF333633),
                start, Offset(size.width * fraction, start.y),
                3.dp.toPx(), StrokeCap.Round
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(time(positionMs), fontSize = 12.sp, color = Color(0xFF818481))
        Text(time(durationMs), fontSize = 12.sp, color = Color(0xFF818481))
    }
}
