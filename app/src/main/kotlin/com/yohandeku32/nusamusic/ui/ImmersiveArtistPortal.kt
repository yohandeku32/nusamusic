package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
 * One 660ms compositor-only timeline. The artist circle rises smoothly from
 * its measured bottom pill coordinate into its own black 76dp toolbar; the
 * full-width artwork begins below that toolbar so the icon never covers it.
 *
 * No blur, layout-frame animation, whole-page scaling, bounce or overlaid
 * artwork leaks. The independently sliding pill is still the REAL pill,
 * with the real touch targets moved by an offset placement modifier.
 */
@Composable
internal fun ImmersiveArtistPortal(
    open: Boolean,
    progress: () -> Float,
    source: Rect?,
    host: Rect?,
    topInsetPx: Float,
    onClose: () -> Unit,
    profile: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val anchor = source ?: return
    val root = host ?: return
    val density = LocalDensity.current
    val sourceX = anchor.left - root.left
    val sourceY = anchor.top - root.top
    val destX = with(density) { 20.dp.toPx() }
    val destY = topInsetPx + with(density) { 10.dp.toPx() }
    val pillTop = (sourceY - with(density) { 10.dp.toPx() })
        .coerceIn(0f, root.height)
    val toolbarHeight = topInsetPx + with(density) { 78.dp.toPx() }
    val panelHeight = pillTop.coerceAtLeast(toolbarHeight)

    Box(modifier) {
        // The entire underlying screen transitions to opaque black; the
        // profile stays fully clipped above the four-button pill.
        Box(
            Modifier.fillMaxWidth()
                .height(with(density) { panelHeight.toDp() })
                .graphicsLayer {
                    val p = progress().coerceIn(0f, 1f)
                    translationY = (1f - p) * size.height * 0.32f
                    alpha = ((p - 0.06f) / 0.88f).coerceIn(0f, 1f)
                    clip = true
                }
                .padding(top = with(density) { toolbarHeight.toDp() })
        ) {
            profile()
        }

        Box(
            Modifier.offset {
                IntOffset(sourceX.roundToInt(), sourceY.roundToInt())
            }
                .size(56.dp)
                .graphicsLayer {
                    val p = progress().coerceIn(0f, 1f)
                    translationX = (destX - sourceX) * p
                    translationY = (destY - sourceY) * p
                    alpha = p
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
