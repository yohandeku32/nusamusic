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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Presentation only: reuses the prepared cover/blur and existing animated-artwork slot. */
@Composable
internal fun ImmersiveArtworkScene(
    artwork: Bitmap?, blur: Bitmap?, backdrop: Color, heroHeight: Dp,
    modifier: Modifier = Modifier,
    heroWidthFraction: Float = 1f, scrimOpacity: Float = 0.2f,
    animateChanges: Boolean = true, description: String? = null,
    animatedArtwork: @Composable BoxScope.() -> Unit = {}
) {
    BoxWithConstraints(modifier.background(backdrop)) {
        val height = heroHeight.coerceAtMost(maxHeight)
        Crossfade(targetState = blur, animationSpec = tween(if (animateChanges) 360 else 0), label = "immersiveAmbient",
            modifier = Modifier.matchParentSize()) { bitmap ->
            Box(Modifier.matchParentSize().drawWithCache {
                val paint = bitmap?.let {
                    val scale = maxOf(size.width / it.width, size.height / it.height) * 1.10f
                    val matrix = android.graphics.Matrix().apply {
                        setScale(scale, scale)
                        postTranslate((size.width - it.width * scale) / 2f, (size.height - it.height * scale) / 2f)
                    }
                    val shader = android.graphics.BitmapShader(it, android.graphics.Shader.TileMode.CLAMP,
                        android.graphics.Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
                    android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
                }
                onDrawBehind {
                    if (paint != null) drawIntoCanvas {
                        it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                    }
                }
            })
        }
        Box(Modifier.matchParentSize().drawWithCache {
            val aura = Brush.radialGradient(listOf(backdrop.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.35f, size.height * 0.52f), radius = size.width * 1.2f)
            onDrawBehind { drawRect(aura) }
        })

        Box(Modifier.fillMaxWidth(heroWidthFraction).height(height).align(Alignment.TopStart)
            .graphicsLayer { clip = true; compositingStrategy = CompositingStrategy.Offscreen }
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

        // A feathered, translucent floor retains the cover's colour through
        // the controls. No solid black panel or hard artwork/control boundary.
        Box(Modifier.matchParentSize().drawWithCache {
            val metadata = ((height.toPx() - 64.dp.toPx()) / size.height).coerceIn(0.28f, 0.82f)
            val fadeStart = (metadata - 0.18f).coerceAtLeast(0.16f)
            val floor = Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.28f),
                0.12f to Color.Transparent,
                fadeStart to Color.Transparent,
                metadata to Color.Black.copy(alpha = 0.48f + scrimOpacity * 0.30f),
                1f to Color.Black.copy(alpha = 0.60f + scrimOpacity * 0.20f))
            val side = if (heroWidthFraction < 1f) Brush.horizontalGradient(
                0f to Color.Transparent,
                (heroWidthFraction * 0.62f) to Color.Transparent,
                heroWidthFraction to Color.Black.copy(alpha = 0.52f + scrimOpacity * 0.25f),
                1f to Color.Black.copy(alpha = 0.62f + scrimOpacity * 0.20f)) else null
            onDrawBehind {
                drawRect(floor)
                if (side != null) drawRect(side)
            }
        })
    }
}
