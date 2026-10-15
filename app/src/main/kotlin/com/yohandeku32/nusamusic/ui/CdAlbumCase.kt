package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.Song

/** The selected disc is in the main player, leaving its open case empty here. */
@Composable
internal fun CdAlbumCase(
    song: Song,
    artwork: @Composable (Song?, Modifier) -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val opening = remember(song.id) { Animatable(0f) }
    // Selection owns the lid state: pausing leaves the disc in the main player
    // and its case open. A different selection closes the previous case.
    LaunchedEffect(song.id, selected) {
        val target = if (selected) 1f else 0f
        if (opening.value != target) {
            opening.animateTo(target, animationSpec = tween(560, easing = FastOutSlowInEasing))
        }
    }
    Box(modifier.shadow(6.dp, RoundedCornerShape(4.dp), clip = false)) {
        EmptyCdTray(selected, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().graphicsLayer {
            transformOrigin = TransformOrigin(0.075f, 0.5f)
            rotationY = -125f * opening.value
            cameraDistance = size.width * 8f
            shape = RoundedCornerShape(4.dp)
            shadowElevation = 3.dp.toPx()
        }) {
            CdAlbumCoverLid(
                song, artwork, selected,
                Modifier.matchParentSize().graphicsLayer {
                    alpha = if (opening.value <= 0.72f) 1f else 0f
                }
            )
            // At 90 degrees the lid is edge-on; swap to its inside face so
            // the opened cover does not show a mirrored album image.
            InsideCdLid(Modifier.matchParentSize().graphicsLayer {
                rotationY = 180f
                alpha = if (opening.value > 0.72f) 1f else 0f
            })
        }
    }
}

@Composable
private fun EmptyCdTray(selected: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF14191B), RoundedCornerShape(4.dp)).drawWithCache {
        val radius = size.minDimension * 0.40f
        val trayCenter = Offset(size.width * 0.54f, size.height * 0.5f)
        val plastic = Brush.linearGradient(
            listOf(Color(0xFF30383C), Color(0xFF101517), Color(0xFF22292D)),
            start = Offset.Zero, end = Offset(size.width, size.height)
        )
        val recess = Brush.radialGradient(
            0f to Color(0xFF0D1214),
            0.85f to Color(0xFF12181B),
            0.96f to Color(0xFF090E10),
            1f to Color(0xFF394246),
            center = trayCenter, radius = radius
        )
        onDrawBehind {
            drawRoundRect(plastic, cornerRadius = CornerRadius(4.dp.toPx()))
            // This is the empty moulded recess and black retaining hub, not
            // another printed or spinning disc inside the carousel case.
            drawCircle(recess, radius, trayCenter)
            drawCircle(Color.White.copy(alpha = 0.13f), radius, trayCenter, style = Stroke(0.6.dp.toPx()))
            drawCircle(Color(0xFF080D0F), radius * 0.13f, trayCenter)
            for (tooth in 0 until 8) {
                rotate(tooth * 45f, trayCenter) {
                    drawLine(
                        Color(0xFF646F72),
                        Offset(trayCenter.x, trayCenter.y - radius * 0.07f),
                        Offset(trayCenter.x, trayCenter.y - radius * 0.14f),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
            drawCircle(Color(0xFF1B2326), radius * 0.065f, trayCenter)
            drawLine(
                Color(0xFF667176).copy(alpha = 0.55f),
                Offset(8.dp.toPx(), size.height * 0.08f),
                Offset(8.dp.toPx(), size.height * 0.92f), 1.dp.toPx()
            )
            drawRoundRect(
                if (selected) Color(0xFFD5DDD8).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.18f),
                topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
                size = Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(0.8.dp.toPx())
            )
        }
    })
}

@Composable
private fun InsideCdLid(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(4.dp)).drawWithCache {
        val innerPlastic = Brush.linearGradient(
            listOf(Color(0xFF798581), Color(0xFF3E4A48), Color(0xFF222D2D)),
            start = Offset.Zero, end = Offset(size.width, size.height)
        )
        val paper = Brush.linearGradient(
            listOf(Color(0xFFC3C9BE), Color(0xFF929D93)),
            start = Offset.Zero, end = Offset(size.width, size.height)
        )
        onDrawBehind {
            drawRoundRect(innerPlastic, cornerRadius = CornerRadius(4.dp.toPx()))
            drawRoundRect(
                paper, topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                size = Size(size.width - 15.dp.toPx(), size.height - 10.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
            drawRoundRect(
                Color.White.copy(alpha = 0.28f),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(0.7.dp.toPx())
            )
        }
    })
}

@Composable
private fun CdAlbumCoverLid(
    song: Song,
    artwork: @Composable (Song?, Modifier) -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier.background(Color(0xFF171B1D), shape)
            .border(
                0.8.dp,
                if (selected) Color(0xFFE3E8E6).copy(alpha = 0.65f)
                else Color(0xFFA8B7BD).copy(alpha = 0.30f),
                shape
            ).clip(shape)
    ) {
        Box(
            Modifier.fillMaxSize().padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            artwork(song, Modifier.fillMaxSize())
        }
        Spacer(Modifier.matchParentSize().drawWithCache {
            val w = size.width
            val h = size.height
            val spine = Brush.horizontalGradient(listOf(
                Color(0xFF394146), Color(0xFF101416), Color(0xFF30383B)
            ), endX = 9.dp.toPx())
            val lid = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.17f), Color.Transparent, Color.Black.copy(alpha = 0.05f)),
                start = Offset.Zero, end = Offset(w, h)
            )
            val reflection = Brush.linearGradient(
                0f to Color.Transparent,
                0.43f to Color.Transparent,
                0.48f to Color.White.copy(alpha = 0.10f),
                0.54f to Color.Transparent,
                1f to Color.Transparent,
                start = Offset.Zero, end = Offset(w, h * 0.7f)
            )
            onDrawBehind {
                drawRoundRect(lid, cornerRadius = CornerRadius(4.dp.toPx()))
                drawRoundRect(reflection, cornerRadius = CornerRadius(4.dp.toPx()))
                drawRect(spine, size = Size(9.dp.toPx(), h))
                for (rib in 1..3) {
                    val x = rib * 2.dp.toPx()
                    drawLine(
                        Color.White.copy(alpha = 0.14f),
                        Offset(x, h * 0.10f), Offset(x, h * 0.90f), 0.5.dp.toPx()
                    )
                }
                for (hinge in listOf(0.20f, 0.72f)) {
                    drawRoundRect(
                        Color(0xFF586269).copy(alpha = 0.75f),
                        topLeft = Offset(1.dp.toPx(), h * hinge),
                        size = Size(6.dp.toPx(), h * 0.08f),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                }
                drawRoundRect(
                    Color.White.copy(alpha = 0.20f),
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(0.6.dp.toPx())
                )
            }
        })
    }
}
