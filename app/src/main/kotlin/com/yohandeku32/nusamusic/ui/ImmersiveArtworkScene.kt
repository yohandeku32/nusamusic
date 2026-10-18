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

/** Full-screen artwork-derived blur, without any black wash or palette fill. */
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
    // Only real blurred cover pixels create the ambience; black is used only
    // if the device has not loaded album artwork at all.
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
        // Intentionally no colour or black-gradient overlay: the ambient IS
        // the blurred album-art bitmap, retaining its real colours and shapes.
        // Adding scrims here makes vibrant covers look like a flat dark fill.

        Box(Modifier.fillMaxWidth(heroWidthFraction).height(height).align(Alignment.TopStart)
            .graphicsLayer {
                // Only the sharp artwork fades in lyric focus mode. The
                // full-screen artwork-derived ambient stays visible beneath.
                alpha = heroOpacity
                clip = true
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                // Feather the sharp cover into its own blurred artwork more gradually.
                // This alpha mask introduces no black or palette gradient.
                val painter = ImmersiveArtworkPainter(size, size.height, size.height * 0.44f)
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


    }
}
