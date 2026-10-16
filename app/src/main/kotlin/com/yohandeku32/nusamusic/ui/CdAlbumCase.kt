package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.data.ArtworkLoader
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos

internal const val CD_SHELF_REST_ANGLE = -76f

/** Two independently projected faces keep the spine readable on the angled shelf. */
@Composable
internal fun CdAlbumCase(
    song: Song,
    artwork: @Composable (Song?, Modifier) -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    angle: () -> Float = { CD_SHELF_REST_ANGLE }
) {
    val fallback = remember(song.albumId, song.album) {
        val palette = listOf(Color(0xFF53657C), Color(0xFFD2D4C9), Color(0xFFA15C45), Color(0xFF657B40), Color(0xFF4B575B))
        palette[(song.album.hashCode() and Int.MAX_VALUE) % palette.size]
    }
    // Sample already decoded cover edges; never reopen audio files for a spine colour.
    val spineColor by produceState(fallback, song.uri, song.albumId) {
        repeat(6) {
            val bitmap = ArtworkLoader.cached(song.uri, 256, song.albumId.takeIf { it > 0L }?.toString())
            if (bitmap != null) {
                value = withContext(Dispatchers.Default) {
                    try {
                        var red = 0
                        var green = 0
                        var blue = 0
                        for (sample in 0 until 8) {
                            val pixel = bitmap.getPixel(
                                (bitmap.width * 0.08f).toInt().coerceIn(0, bitmap.width - 1),
                                ((sample + 0.5f) / 8f * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                            )
                            red += (pixel shr 16) and 255
                            green += (pixel shr 8) and 255
                            blue += pixel and 255
                        }
                        Color(red / 8, green / 8, blue / 8)
                    } catch (_: RuntimeException) {
                        fallback
                    }
                }
                return@produceState
            }
            delay(80)
        }
    }
    val spineInk = if (spineColor.luminance() > 0.42f) Color(0xFF171B1C) else Color(0xFFF3F3EC)
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val cameraDistancePx = with(density) { maxOf(maxWidth * 3.2f, maxHeight * 4f).toPx() }
        val spineLabelWidth = maxHeight - 12.dp
        val shape = RoundedCornerShape(3.dp)
        Box(
            Modifier.matchParentSize().graphicsLayer {
                val degrees = angle()
                val projectedWidth = size.width * cos(Math.toRadians(degrees.toDouble())).toFloat()
                transformOrigin = TransformOrigin(0f, 0.5f)
                rotationY = degrees
                translationX = (size.width - projectedWidth) / 2f
                // Account for the long cases in the vertical rack so their
                // far edge stays within the shelf instead of flaring outward.
                cameraDistance = cameraDistancePx
                this.shape = shape
                shadowElevation = 3.dp.toPx()
            }.background(Color(0xFF15191B), shape)
                .border(0.8.dp, if (selected) Color(0xFFE9EBDD) else Color.White.copy(alpha = 0.25f), shape)
                .clip(shape)
        ) {
            artwork(song, Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(1.dp)))
            Spacer(Modifier.matchParentSize().drawWithCache {
                val sheen = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.06f)),
                    start = Offset.Zero, end = Offset(size.width, size.height)
                )
                onDrawBehind { drawRect(sheen) }
            })
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))))
                    .padding(start = 8.dp, end = 6.dp, top = 16.dp, bottom = 6.dp)
            ) {
                Text(song.title, color = Color.White, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.White.copy(alpha = 0.82f), fontSize = 7.sp, lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier.width(8.dp).fillMaxHeight().graphicsLayer {
                val degrees = angle()
                val projectedWidth = widthPx * cos(Math.toRadians(degrees.toDouble())).toFloat()
                transformOrigin = TransformOrigin(1f, 0.5f)
                rotationY = degrees + 90f
                translationX = (widthPx - projectedWidth) / 2f - size.width
                cameraDistance = cameraDistancePx
            }.background(spineColor).drawWithCache {
                val edge = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.18f)))
                onDrawWithContent {
                    drawContent()
                    drawRect(edge)
                }
            },
            contentAlignment = Alignment.Center
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(song.title) }
                    append("  ")
                    append(song.artist)
                },
                modifier = Modifier.requiredSize(width = spineLabelWidth, height = 8.dp).graphicsLayer { rotationZ = -90f },
                color = spineInk, fontSize = 7.sp, lineHeight = 8.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
