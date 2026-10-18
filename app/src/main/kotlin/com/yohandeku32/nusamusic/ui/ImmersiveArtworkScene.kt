package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Full-screen real artwork blur with a feathered sharp hero and protected control contrast. */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun ImmersiveArtworkScene(
    artwork: Bitmap?, blur: Bitmap?, backdrop: Color, heroHeight: Dp,
    modifier: Modifier = Modifier,
    heroWidthFraction: Float = 1f, scrimOpacity: Float = 0.2f,
    heroOpacity: Float = 1f,
    animateChanges: Boolean = true, description: String? = null,
    animatedArtwork: @Composable BoxScope.() -> Unit = {}
) {
    // Never fill the screen with a sampled palette colour. In the absence of
    // artwork, black stays black; real album pixels alone create the ambience.
    BoxWithConstraints(modifier.background(Color.Black)) {
        val height = heroHeight.coerceAtMost(maxHeight)
        // Draw the actual blurred cover through Compose's Image renderer,
        // just like the visible album artwork. The previous native BitmapShader
        // path could render a black ambient on some devices even when the blur
        // bitmap existed, and its cover-scale crop discarded most of the image.
        // FillBounds preserves the cover's complete colour areas and faint
        // silhouette across the tall display; the image is already diffused.
        // Crossfade the real images so track changes remain fluid.
        Crossfade(
            targetState = blur,
            animationSpec = tween(if (animateChanges) 700 else 0),
            label = "immersiveAmbient",
            modifier = Modifier.matchParentSize()
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // One gentle darkening pass over the REAL artwork. The separate
        // readability floor below already shades the control area, so these
        // alphas must stay low: stacking opaque gradients erased the album
        // colours and made the background appear solid black.
        Box(Modifier.matchParentSize().drawWithCache {
            val falloff = Brush.verticalGradient(
                0f to Color.Transparent,
                0.20f to Color.Transparent,
                0.43f to Color.Black.copy(alpha = 0.035f),
                0.64f to Color.Black.copy(alpha = 0.12f),
                0.82f to Color.Black.copy(alpha = 0.24f),
                1f to Color.Black.copy(alpha = 0.36f)
            )
            onDrawBehind { drawRect(falloff) }
        })

        Box(Modifier.fillMaxWidth(heroWidthFraction).height(height).align(Alignment.TopStart)
            .graphicsLayer {
                // Only the sharp artwork fades in lyric focus mode. The
                // full-screen artwork-derived ambient stays visible beneath.
                alpha = heroOpacity
                clip = true
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val painter = ImmersiveArtworkPainter(size, size.height, size.height * 0.27f)
                val sideFade = if (heroWidthFraction < 1f) Brush.horizontalGradient(
                    0f to Color.White, 0.72f to Color.White, 1f to Color.Transparent) else null
                onDrawWithContent {
                    drawContent()
                    with(painter) { fadeArtwork() }
                    if (sideFade != null) drawRect(sideFade, blendMode = BlendMode.DstIn)
                }
            }) {
            Crossfade(targetState = artwork, animationSpec = tween(if (animateChanges) 360 else 0), label = "immersiveHero",
                modifier = Modifier.fillMaxSize()) { cover ->
                if (cover != null) Image(cover.asImageBitmap(), description,
                    contentScale = ContentScale.Crop, alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.size(52.dp).align(Alignment.Center))
            }
            animatedArtwork()
        }

        // Neutral readability floor, not a sampled-colour wash. The
        // structured album blur must remain visible behind the playback area
        // while white labels keep readable contrast on bright covers.
        Box(Modifier.matchParentSize().drawWithCache {
            val metadata = ((height.toPx() - 64.dp.toPx()) / size.height).coerceIn(0.28f, 0.82f)
            val fadeStart = (metadata - 0.18f).coerceAtLeast(0.16f)
            val floor = Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.20f),
                0.12f to Color.Transparent,
                fadeStart to Color.Transparent,
                metadata to Color.Black.copy(alpha = 0.35f + scrimOpacity * 0.28f),
                1f to Color.Black.copy(alpha = 0.50f + scrimOpacity * 0.18f))
            val side = if (heroWidthFraction < 1f) Brush.horizontalGradient(
                0f to Color.Transparent,
                (heroWidthFraction * 0.62f) to Color.Transparent,
                heroWidthFraction to Color.Black.copy(alpha = 0.43f + scrimOpacity * 0.22f),
                1f to Color.Black.copy(alpha = 0.52f + scrimOpacity * 0.18f)) else null
            onDrawBehind {
                drawRect(floor)
                if (side != null) drawRect(side)
            }
        })
    }
}
