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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Frosted controls use REAL source-backed backdrop blur. Haze samples the
 * artwork or the scrolling Artist/Library profile behind the actual pill,
 * so the color/detail moves naturally beneath the glass as the list scrolls.
 *
 * Android 12+ renders the blur; Haze provides an inexpensive tinted fallback
 * on earlier releases. Restrict hazeEffect to the ~56dp-high navigation pills
 * instead of applying expensive RenderEffects to the entire page.
 *
 * When the Library itself is light, its floating controls deliberately keep
 * dark glass and light icons rather than switching to a white opaque capsule.
 */
internal fun Modifier.immersiveGlassPill(
    radius: Dp = 100.dp,
    lightMode: Boolean = false,
    emphasized: Boolean = false,
    hazeState: HazeState? = null
): Modifier {
    val shape = RoundedCornerShape(radius)
    if (emphasized && hazeState != null) {
        // Dark, tinted material with light passing through from the live
        // backdrop; a translucent colored rectangle is not a blur.
        val style = HazeStyle(
            backgroundColor = Color(0xFF131720),
            // Less diffusion lets artwork colors and scrolling rows remain
            // perceptible, while the glass is still a real backdrop blur.
            tints = listOf(HazeTint(Color(0x781B202C))),
            blurRadius = 20.dp,
            noiseFactor = 0.035f,
            fallbackTint = HazeTint(Color(0xCD232833))
        )
        return this
            .clip(shape)
            .hazeEffect(state = hazeState, style = style)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.075f),
                        Color.White.copy(alpha = 0.017f),
                        Color.Black.copy(alpha = 0.055f)
                    )
                )
            )
            // Keep only a hairline glint: a bright outline looks like an
            // old bordered capsule rather than floating frosted glass.
            .border(0.6.dp, Color.White.copy(alpha = 0.085f), shape)
    }

    // The original lighter Now Playing control and quality badges remain as
    // they were before the new Artist/Library glass treatments.
    return this.clip(shape)
        .background(
            if (lightMode) Color.Black.copy(alpha = 0.055f)
            else Color.White.copy(alpha = 0.09f)
        )
        .border(
            1.dp,
            if (lightMode) Color.Black.copy(alpha = 0.09f)
            else Color.White.copy(alpha = 0.08f),
            shape
        )
}
