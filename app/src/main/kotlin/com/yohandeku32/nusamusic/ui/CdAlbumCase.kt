package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.Song

/** A compact album-cover jewel case for the stack; the main CD stays separate. */
@Composable
internal fun CdAlbumCase(
    song: Song,
    artwork: @Composable (Song?, Modifier) -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier.shadow(6.dp, shape, clip = false)
            .background(Color(0xFF171B1D), shape)
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
