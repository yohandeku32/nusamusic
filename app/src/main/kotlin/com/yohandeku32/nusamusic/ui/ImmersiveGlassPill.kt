package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
    liquidGlass: Boolean = true,
    hazeState: HazeState? = null
): Modifier {
    val shape = RoundedCornerShape(radius)
    if (emphasized && liquidGlass && hazeState != null) {
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
                        Color.White.copy(alpha = 0.072f),
                        Color.White.copy(alpha = 0.010f),
                        Color.Black.copy(alpha = 0.058f)
                    )
                )
            )
            // Refractive-looking meniscus: the thin curved edge catches light
            // while soft radial light pools make the capsule read as wet
            // optical glass rather than a flat translucent grey rectangle.
            // It is draw-cache only; no shader, video capture or heavy blur.
            .liquidGlassOptics()
            .border(0.45.dp, Color.White.copy(alpha = 0.045f), shape)
    }

    // The lightweight fallback avoids backdrop recording entirely during
    // entrance/exit gestures and whenever Liquid Glass is disabled.
    // Its restrained specular gradient keeps the floating-controls feeling.
    if (emphasized) return this.clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(Color(0xC82B303C), Color(0xAF1D222D))
            )
        )
        // During a swipe the inexpensive fallback still has the same
        // rounded lens silhouette so switching into live Haze is unobtrusive.
        .then(if (liquidGlass) Modifier.liquidGlassOptics() else Modifier)
        .border(0.45.dp, Color.White.copy(alpha = 0.045f), shape)

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

/**
 * Cached micro-highlights for the navigation layer. Water droplets have
 * a brighter outer meniscus and broad, faint internal reflections, unlike
 * a uniformly white stroke. Keep these deliberately restrained so moving
 * song covers remain legible behind the Haze source.
 */
private fun Modifier.liquidGlassOptics(): Modifier = this.drawWithCache {
    val radius = CornerRadius(size.height * 0.5f, size.height * 0.5f)
    val topPool = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.105f),
            Color.White.copy(alpha = 0.027f),
            Color.Transparent
        ),
        center = Offset(size.width * 0.18f, size.height * 0.02f),
        radius = size.width.coerceAtLeast(size.height) * 0.60f
    )
    val bottomPool = Brush.radialGradient(
        colors = listOf(
            Color(0xFFBDD9FF).copy(alpha = 0.060f),
            Color.Transparent
        ),
        center = Offset(size.width * 0.89f, size.height * 0.99f),
        radius = size.width.coerceAtLeast(size.height) * 0.47f
    )
    val meniscus = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.20f),
        0.26f to Color.White.copy(alpha = 0.065f),
        0.55f to Color.Transparent,
        1f to Color.White.copy(alpha = 0.045f)
    )
    val meniscusWidth = 1.05.dp.toPx()
    onDrawWithContent {
        drawContent()
        drawRoundRect(brush = topPool, cornerRadius = radius)
        drawRoundRect(brush = bottomPool, cornerRadius = radius)
        drawRoundRect(
            brush = meniscus,
            cornerRadius = radius,
            style = Stroke(width = meniscusWidth)
        )
    }
}

/**
 * A single navigation material used by both Now Playing controls and the
 * floating Artist/Library controls. When enabled the optical fallback remains
 * visible during gestures and GPU Haze is added only after the content settles.
 * When disabled the pre-existing simple pill is used on every page.
 *
 * The backdrop fades independently of position, so there is no sudden
 * opaque-to-glass switch at the last frame of a portal transition.
 */
@Composable
internal fun ContinuousLiquidGlassSurface(
    radius: Dp,
    lightMode: Boolean,
    liquidGlassEnabled: Boolean,
    hazeState: HazeState?,
    hazeAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Box(
            Modifier.matchParentSize().immersiveGlassPill(
                radius = radius,
                lightMode = lightMode,
                emphasized = liquidGlassEnabled,
                liquidGlass = liquidGlassEnabled,
                hazeState = null
            )
        )
        if (liquidGlassEnabled && hazeState != null) {
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer {
                        alpha = hazeAlpha.coerceIn(0f, 1f)
                    }
                    .immersiveGlassPill(
                        radius = radius,
                        lightMode = lightMode,
                        emphasized = true,
                        liquidGlass = true,
                        hazeState = hazeState
                    )
            )
        }
    }
}

