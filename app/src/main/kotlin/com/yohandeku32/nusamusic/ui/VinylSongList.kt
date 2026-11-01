package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yohandeku32.nusamusic.model.Song

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VinylSongList(
    songs: List<Song>, state: LazyListState, currentSongId: Long?, isPlaying: Boolean,
    backgroundColor: Color, playLabel: String, pauseLabel: String, unknownArtistLabel: String,
    onPlay: (Song) -> Unit, onRequestDelete: (Song) -> Unit, onTogglePlay: () -> Unit,
    artwork: @Composable (Song, Modifier) -> Unit, modifier: Modifier = Modifier,
    deleteLabel: String = "Delete"
) {
    val fontScale = LocalDensity.current.fontScale
    val motion = (rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) > 0f
    val stacked = fontScale < 1.6f
    val shape = RoundedCornerShape(6.dp)
    val scrim = remember {
        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.72f),
            0.85f to Color.Black.copy(alpha = 0.65f), 1f to Color.Transparent)
    }
    BoxWithConstraints(modifier.background(backgroundColor).testTag("vinyl-crate")) {
        val sleeveSize = (maxWidth - 48.dp).coerceIn(160.dp, 420.dp)
        val exposedHeight = 80.dp * fontScale.coerceAtLeast(1f)
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(if (stacked) exposedHeight - sleeveSize else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().testTag("vinyl-sleeves")
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "record-sleeve" }) { index, song ->
                val current = currentSongId == song.id
                val playing = current && isPlaying
                val artist = song.artist.ifBlank { unknownArtistLabel }
                Box(
                    Modifier.size(sleeveSize).zIndex(index.toFloat())
                        .graphicsLayer {
                            scaleX = 1f
                            rotationX = 0f
                            if (stacked && motion) {
                                val layout = state.layoutInfo
                                val item = layout.visibleItemsInfo.firstOrNull { it.key == song.id }
                                val room = (layout.viewportEndOffset - layout.viewportStartOffset).coerceAtLeast(1)
                                val depth = ((item?.offset ?: 0) - layout.viewportStartOffset).toFloat() / room
                                scaleX = 0.82f + 0.18f * depth.coerceIn(0f, 1f)
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                rotationX = -10f
                                cameraDistance = size.height * 2.5f
                            }
                        }
                        .shadow(8.dp, shape, clip = false).clip(shape).background(Color(0xFF282824))
                        .combinedClickable(
                            role = Role.Button, onClickLabel = if (playing) pauseLabel else playLabel,
                            onLongClickLabel = "$deleteLabel ${song.title}",
                            onClick = { if (current) onTogglePlay() else onPlay(song) },
                            onLongClick = { onRequestDelete(song) }
                        )
                        .semantics { selected = current; contentDescription = "${song.title}, $artist" }
                        .testTag("vinyl-sleeve-${song.id}")
                ) {
                    artwork(song, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxWidth().height(if (stacked) exposedHeight + 48.dp else sleeveSize)
                        .background(scrim))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(2.dp))) {
                            artwork(song, Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, fontSize = 17.sp, lineHeight = 22.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = if (stacked) 1 else 2,
                                overflow = TextOverflow.Ellipsis)
                            Text(artist, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp,
                                maxLines = if (stacked) 1 else 2, overflow = TextOverflow.Ellipsis)
                            if (current && !stacked) Text(if (playing) pauseLabel else playLabel,
                                color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                        if (current && stacked) {
                            Spacer(Modifier.width(12.dp))
                            Text(if (playing) pauseLabel else playLabel, color = Color.White,
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (current) Box(Modifier.matchParentSize().border(2.dp, Color.White, shape))
                }
            }
        }
    }
}
