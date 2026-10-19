package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared translucent frosted-glass finish for all Immersive controls.
 *
 * Reuses the exact 9%-white fill and 8%-white outline of the bottom action
 * pill, so the small audio-quality badge matches without a separate opaque
 * card. Never apply a new blur RenderEffect per chip during animations.
 */
internal fun Modifier.immersiveGlassPill(radius: Dp = 100.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape)
        .background(Color.White.copy(alpha = 0.09f))
        .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
}
