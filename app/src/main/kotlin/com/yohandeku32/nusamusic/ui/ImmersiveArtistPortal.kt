package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Artist expands vertically from the measured 56dp circular player action.
 * The four other actions remain in their original bottom pill; the profile
 * panel terminates immediately above that pill.
 */
@Composable
internal fun ImmersiveArtistPortal(
    open: Boolean,
    progress: Float,
    source: Rect?,
    host: Rect?,
    topInsetPx: Float,
    onClose: () -> Unit,
    profile: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!open && progress <= 0.001f) return
    val anchor = source ?: return
    val root = host ?: return
    val density = LocalDensity.current
    val fraction = progress.coerceIn(0f, 1f)
    val startX = anchor.left - root.left
    val startY = anchor.top - root.top
    val destinationY = topInsetPx + with(density) { 12.dp.toPx() }
    val currentY = startY + (destinationY - startY) * fraction
    val diameter = with(density) { 56.dp.toPx() }
    val panelBottom = (startY - with(density) { 8.dp.toPx() })
        .coerceIn(diameter * 2f, root.height)
    val panelAlpha = ((fraction - 0.18f) / 0.72f).coerceIn(0f, 1f)
    val lift = with(density) { 16.dp.toPx() }

    Box(modifier) {
        if (panelAlpha > 0.001f) {
            Box(
                Modifier.fillMaxWidth()
                    .height(with(density) { panelBottom.toDp() })
                    .graphicsLayer {
                        alpha = panelAlpha
                        translationY = (1f - fraction) * lift
                    }
            ) {
                profile()
            }
        }
        Box(
            Modifier.offset {
                IntOffset(startX.roundToInt(), currentY.roundToInt())
            }
                .size(56.dp)
                .graphicsLayer {
                    alpha = (fraction * 2f).coerceIn(0f, 1f)
                }
                .immersiveGlassPill(50.dp)
                .clickable(enabled = open, onClick = onClose)
                .semantics { contentDescription = "Close artist profile" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = NusaPlayerIcons.Artist,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}
