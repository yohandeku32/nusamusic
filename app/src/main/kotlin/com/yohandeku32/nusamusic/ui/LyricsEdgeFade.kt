package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/** Mask only the lyrics layer, preserving the player's prepared background. */
internal fun Modifier.lyricsEdgeFade(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val mask = Brush.verticalGradient(
        0f to Color.Transparent,
        0.22f to Color.White,
        0.78f to Color.White,
        1f to Color.Transparent,
        startY = 0f,
        endY = size.height.coerceAtLeast(1f)
    )
    onDrawWithContent {
        drawContent()
        drawRect(mask, blendMode = BlendMode.DstIn)
    }
}
