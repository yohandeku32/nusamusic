package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Opaque, inexpensive navigation material shared by the Now Playing,
 * Library and Artist pages. Drawing a single black shape requires neither
 * source capture nor offscreen backdrop blur, even while a portal moves.
 */
internal fun Modifier.immersiveSolidPill(radius: Dp = 100.dp): Modifier =
    clip(RoundedCornerShape(radius)).background(Color.Black)

/** The same stable 100% opaque material behind every floating pill. */
@Composable
internal fun SolidNavigationSurface(
    radius: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier.immersiveSolidPill(radius))
}
