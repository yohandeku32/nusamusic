package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import java.util.Locale

private val ControlAccent = Color(0xFFB5DF51)

private fun controlText(id: String, en: String): String =
    if (Locale.getDefault().language.equals("id", ignoreCase = true)) id else en

@Composable
internal fun CdPlaybackControls(
    isPlaying: Boolean,
    hasSong: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    realistic: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.widthIn(max = 360.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CdPlaybackButton(
            icon = Icons.Default.Shuffle,
            label = controlText("Acak", "Shuffle"),
            enabled = hasSong,
            active = shuffleEnabled,
            toggleState = shuffleEnabled,
            utility = true,
            realistic = realistic,
            onClick = onShuffle
        )
        CdPlaybackButton(
            icon = Icons.Default.SkipPrevious,
            label = controlText("Sebelumnya", "Previous"),
            enabled = hasSong,
            realistic = realistic,
            onClick = onPrevious
        )
        // Keep Play available with an empty library: its existing action starts
        // the first song, requests permission, or opens the collection.
        CdPlaybackButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            label = if (isPlaying) controlText("Jeda", "Pause") else controlText("Putar", "Play"),
            primary = true,
            realistic = realistic,
            onClick = onPlayPause
        )
        CdPlaybackButton(
            icon = Icons.Default.SkipNext,
            label = controlText("Berikutnya", "Next"),
            enabled = hasSong,
            realistic = realistic,
            onClick = onNext
        )
        CdPlaybackButton(
            icon = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
            label = controlText("Ulangi", "Repeat"),
            stateLabel = when (repeatMode) {
                1 -> controlText("Ulangi satu lagu", "Repeat one song")
                2 -> controlText("Ulangi semua lagu", "Repeat all songs")
                else -> controlText("Nonaktif", "Off")
            },
            enabled = hasSong,
            active = repeatMode != 0,
            utility = true,
            realistic = realistic,
            onClick = onRepeat
        )
    }
}

@Composable
private fun CdPlaybackButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    utility: Boolean = false,
    active: Boolean = false,
    toggleState: Boolean? = null,
    stateLabel: String? = null,
    realistic: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressDepth by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(if (pressed) 90 else 160),
        label = "CD button press"
    )
    val faceTop by animateColorAsState(
        targetValue = when {
            primary -> Color(0xFFDEF2AD)
            active -> Color(0xFF3E4D2C)
            utility -> Color(0xFF303633)
            else -> Color(0xFF4B514D)
        }, animationSpec = tween(180), label = "CD button highlight"
    )
    val faceBottom by animateColorAsState(
        targetValue = when {
            primary -> Color(0xFF8FB13E)
            active -> Color(0xFF202A19)
            else -> Color(0xFF1B201E)
        }, animationSpec = tween(180), label = "CD button face"
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            primary -> Color(0xFF1A250D)
            active -> ControlAccent
            utility -> Color(0xFFB8C0B4)
            else -> Color(0xFFF0F2E8)
        }, animationSpec = tween(180), label = "CD button icon"
    )
    val action = if (toggleState != null) {
        Modifier.toggleable(
            value = toggleState, enabled = enabled, role = Role.Switch,
            interactionSource = interaction, indication = null,
            onValueChange = { onClick() }
        )
    } else {
        Modifier.clickable(
            enabled = enabled, role = Role.Button,
            interactionSource = interaction, indication = null, onClick = onClick
        )
    }
    // The touch target stays fixed at 48dp or larger; only the drawn face sinks.
    Box(
        Modifier.size(if (primary) 64.dp else 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                if (stateLabel != null) stateDescription = stateLabel
            }.then(action),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(if (primary) 64.dp else if (utility) 40.dp else 48.dp)
                .graphicsLayer {
                    val scale = 1f - pressDepth * 0.055f
                    scaleX = scale
                    scaleY = scale
                    translationY = pressDepth * 1.5.dp.toPx()
                    alpha = if (enabled) 1f else 0.38f
                    shape = CircleShape
                    shadowElevation = if (realistic && enabled) {
                        ((if (primary) 9f else 5f) * (1f - pressDepth * 0.7f)).dp.toPx()
                    } else 0f
                    ambientShadowColor = Color.Black
                    spotShadowColor = if (primary) ControlAccent.copy(alpha = 0.35f) else Color.Black
                }
                .drawWithCache {
                    val radius = size.minDimension / 2f
                    val rim = Brush.linearGradient(listOf(
                        if (primary) Color(0xFFECFBC4) else Color.White.copy(alpha = if (active) 0.28f else 0.20f),
                        if (active) ControlAccent.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.5f)
                    ), start = Offset.Zero, end = Offset(size.width, size.height))
                    val face = Brush.verticalGradient(listOf(faceTop, if (primary) ControlAccent else faceBottom, faceBottom))
                    val reflection = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = if (primary) 0.22f else 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.30f, size.height * 0.18f), radius = size.width * 0.8f
                    )
                    onDrawBehind {
                        drawCircle(if (primary) Color(0xFF5A7425) else Color(0xFF0D110E), radius)
                        if (realistic) {
                            drawCircle(rim, radius * 0.98f)
                            drawCircle(face, radius * 0.92f)
                            drawCircle(reflection, radius * 0.92f)
                            drawCircle(Color.Black.copy(alpha = 0.12f), radius * 0.82f, style = Stroke(0.6.dp.toPx()))
                            drawArc(
                                color = Color.White.copy(alpha = if (primary) 0.50f else 0.20f),
                                startAngle = 205f, sweepAngle = 100f, useCenter = false,
                                topLeft = Offset(radius * 0.14f, radius * 0.14f),
                                size = Size(radius * 1.72f, radius * 1.72f),
                                style = Stroke(0.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            drawCircle(if (primary) ControlAccent else faceBottom, radius)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(if (primary) 32.dp else if (utility) 20.dp else 26.dp), tint = iconColor)
        }
        if (utility && active && enabled) {
            Box(
                Modifier.align(Alignment.BottomCenter).offset(y = 4.dp).size(4.dp)
                    .drawWithCache { onDrawBehind { drawCircle(ControlAccent) } }
            )
        }
    }
}
