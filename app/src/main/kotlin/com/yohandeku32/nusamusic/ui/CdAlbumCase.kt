package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.Song

/** The selected disc is in the main player, leaving its open case empty here. */
@Composable
internal fun CdAlbumCase(
    song: Song,
    artwork: @Composable (Song?, Modifier) -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    showDisc: Boolean = false,
    discRotation: () -> Float = { 0f },
    onDiscBounds: (Rect) -> Unit = {}
) {
    val opening = remember(song.id) { Animatable(0f) }
    // Selection owns the lid state: pausing leaves the disc in the main player
    // and its case open. A different selection closes the previous case.
    LaunchedEffect(song.id, selected) {
        val target = if (selected) 1f else 0f
        if (opening.value != target) {
            opening.animateTo(target, animationSpec = tween(560, easing = FastOutSlowInEasing))
        }
    }
    BoxWithConstraints(modifier.shadow(6.dp, RoundedCornerShape(3.dp), clip = false)) {
        EmptyCdTray(selected, Modifier.matchParentSize())
        val diameter = maxHeight * 0.85f
        Box(Modifier.offset(x = maxWidth * 0.55f - diameter / 2, y = (maxHeight - diameter) / 2)
            .size(diameter).onGloballyPositioned { onDiscBounds(it.boundsInRoot()) }) {
            if (showDisc) CdDiscSurface(song, artwork, Modifier.matchParentSize(), rotationDegrees = discRotation, shadowElevation = 0.dp)
        }
        Box(Modifier.matchParentSize().graphicsLayer {
            transformOrigin = TransformOrigin(0.075f, 0.5f)
            rotationY = -125f * opening.value
            cameraDistance = size.width * 8f
            shape = RoundedCornerShape(3.dp)
            shadowElevation = 3.dp.toPx()
        }) {
            CdAlbumCoverLid(
                song, artwork,
                Modifier.matchParentSize().graphicsLayer {
                    alpha = if (opening.value <= 0.72f) 1f else 0f
                }
            )
            // The inside face stays clear when the lid passes 90 degrees,
            // instead of showing a mirrored album cover or an opaque panel.
            InsideCdLid(Modifier.matchParentSize().graphicsLayer {
                rotationY = 180f
                alpha = if (opening.value > 0.72f) 1f else 0f
            })
        }
    }
}

@Composable
private fun EmptyCdTray(selected: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.drawWithCache {
        onDrawBehind {
            drawClearTray()
            drawSpindle(Offset(size.width * 0.55f, size.height * 0.5f))
        }
    }.border(
        0.8.dp,
        Color(0xFFD7E6EB).copy(alpha = if (selected) 0.60f else 0.18f),
        RoundedCornerShape(3.dp)
    ))
}

@Composable
private fun InsideCdLid(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(3.dp)).drawWithCache {
        onDrawBehind {
            drawClearLid()
            drawClearCaseReflections()
        }
    })
}

@Composable
private fun CdAlbumCoverLid(
    song: Song,
    artwork: @Composable (Song?, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(3.dp))) {
        // Keep the sleeve inside the moulded clear frame and ribbed hinge.
        artwork(
            song,
            Modifier.fillMaxSize().padding(
                start = maxWidth * 0.11f, end = maxWidth * 0.04f,
                top = maxHeight * 0.035f, bottom = maxHeight * 0.035f
            ).clip(RoundedCornerShape(1.dp))
        )
        CdClearLid(Modifier.matchParentSize(), showStickers = false)
        CdHiResBadge(song, Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(6.dp))
    }
}
