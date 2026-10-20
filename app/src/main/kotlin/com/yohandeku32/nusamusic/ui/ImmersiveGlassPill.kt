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
 * On Artist/Library overlays, use a near-opaque satin-glass surface so
 * background artwork doesn't show through the controls. This is opacity
 * tuning rather than expensive live backdrop blurring; other badges keep
 * their existing lightweight finish.
 */
internal fun Modifier.immersiveGlassPill(
    radius: Dp = 100.dp,
    lightMode: Boolean = false,
    emphasized: Boolean = false
): Modifier {
    val shape = RoundedCornerShape(radius)
    val fill = when {
        emphasized && lightMode -> Brush.verticalGradient(
            listOf(
                Color(0xFFF5F6F9).copy(alpha = 0.96f),
                Color(0xFFE9ECF2).copy(alpha = 0.94f)
            )
        )
        emphasized -> Brush.verticalGradient(
            listOf(
                Color(0xFF383B46).copy(alpha = 0.94f),
                Color(0xFF252832).copy(alpha = 0.93f)
            )
        )
        lightMode -> Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.055f), Color.Black.copy(alpha = 0.055f))
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.09f))
        )
    }
    val outline = when {
        emphasized && lightMode -> Color.Black.copy(alpha = 0.12f)
        emphasized -> Color.White.copy(alpha = 0.18f)
        lightMode -> Color.Black.copy(alpha = 0.09f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    return this.clip(shape).background(fill).border(1.dp, outline, shape)
}
