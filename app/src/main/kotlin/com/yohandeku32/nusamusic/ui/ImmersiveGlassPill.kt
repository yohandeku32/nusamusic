package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The Hi-Res badge and navigation share one thin material over the artwork blur. */
internal fun Modifier.immersiveGlassPill(
    radius: Dp = 100.dp,
    lightMode: Boolean = false
): Modifier {
    val shape = RoundedCornerShape(radius)
    return clip(shape)
        .background(if (lightMode) Color.Black.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.09f))
        .border(1.dp, if (lightMode) Color.Black.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.08f), shape)
}

@Composable
internal fun ImmersiveNavigationSurface(
    radius: Dp,
    lightMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier.immersiveGlassPill(radius, lightMode))
}

