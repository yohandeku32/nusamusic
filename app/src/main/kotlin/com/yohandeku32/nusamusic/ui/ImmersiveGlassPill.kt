package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The emphasized variant provides a calmer frosted appearance for the two
 * in-player overlays, without an extra live RenderEffect/blur render pass on
 * every animation frame. Existing badges retain the original subtle finish.
 */
internal fun Modifier.immersiveGlassPill(
    radius: Dp = 100.dp,
    lightMode: Boolean = false,
    emphasized: Boolean = false
): Modifier {
    val shape = RoundedCornerShape(radius)
    val fill = when {
        emphasized && lightMode -> Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.13f), Color.Black.copy(alpha = 0.095f))
        )
        emphasized -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.145f))
        )
        lightMode -> Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.055f), Color.Black.copy(alpha = 0.055f))
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.09f))
        )
    }
    val outline = when {
        emphasized && lightMode -> Color.Black.copy(alpha = 0.15f)
        emphasized -> Color.White.copy(alpha = 0.16f)
        lightMode -> Color.Black.copy(alpha = 0.09f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    return this.clip(shape).background(fill).border(1.dp, outline, shape)
}
