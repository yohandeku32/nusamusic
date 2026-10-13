package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/** A browsable record wheel. Scrolling changes focus; only a tap starts playback. */
@Composable
fun VinylSongList(
    songs: List<Song>,
    state: LazyListState,
    currentSongId: Long?,
    isPlaying: Boolean,
    backgroundColor: Color,
    playLabel: String,
    pauseLabel: String,
    unknownArtistLabel: String,
    onPlay: (Song) -> Unit,
    onTogglePlay: () -> Unit,
    artwork: @Composable (Song, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
    val itemHeight = 104.dp * fontScale
    val cardColor = MaterialTheme.colorScheme.surface
    val focusedKey by remember(state) {
        derivedStateOf {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            layout.visibleItemsInfo.minByOrNull { item ->
                abs(item.offset + item.size / 2f - center)
            }?.key
        }
    }

    BoxWithConstraints(modifier.clipToBounds()) {
        // Equal padding lets the first and last records also settle in the center.
        val centerPadding = ((maxHeight - itemHeight) / 2f).coerceAtLeast(0.dp)
        val edgeFadeHeight = (maxHeight * 0.16f).coerceAtMost(80.dp)
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            contentPadding = PaddingValues(
                top = centerPadding,
                bottom = centerPadding,
                start = 26.dp,
                end = 30.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "vinyl-song" }) { index, song ->
                val focused = focusedKey == song.id
                VinylSongRow(
                    song = song,
                    focused = focused,
                    current = currentSongId == song.id,
                    playing = currentSongId == song.id && isPlaying,
                    cardColor = cardColor,
                    playLabel = playLabel,
                    pauseLabel = pauseLabel,
                    unknownArtistLabel = unknownArtistLabel,
                    onClick = {
                        scope.launch { state.animateScrollToItem(index) }
                        if (currentSongId == song.id) onTogglePlay() else onPlay(song)
                    },
                    artwork = artwork,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            // Read scrolling offsets in the drawing phase so each scroll
                            // frame only moves the visible layers, without reloading artwork.
                            val layout = state.layoutInfo
                            val item = layout.visibleItemsInfo.firstOrNull { it.key == song.id }
                            val viewportHeight = (layout.viewportEndOffset - layout.viewportStartOffset)
                                .toFloat().coerceAtLeast(1f)
                            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                            val distance = if (item != null) {
                                ((item.offset + item.size / 2f - center) / (viewportHeight / 2f))
                                    .coerceIn(-1f, 1f)
                            } else 0f
                            val edgeDistance = abs(distance)
                            val scale = 1f - 0.12f * edgeDistance

                            transformOrigin = TransformOrigin(40.dp.toPx() / size.width, 0.5f)
                            translationX = size.width * 0.43f * distance * distance
                            rotationZ = -distance * 42f
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.94f * edgeDistance.pow(1.6f)
                        }
                )
            }
        }

        // Fade into the page itself at both ends of the wheel.
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(edgeFadeHeight)
                .background(Brush.verticalGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0f))))
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(edgeFadeHeight)
                .background(Brush.verticalGradient(listOf(backgroundColor.copy(alpha = 0f), backgroundColor)))
        )
    }
}

@Composable
private fun VinylSongRow(
    song: Song,
    focused: Boolean,
    current: Boolean,
    playing: Boolean,
    cardColor: Color,
    playLabel: String,
    pauseLabel: String,
    unknownArtistLabel: String,
    onClick: () -> Unit,
    artwork: @Composable (Song, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(180),
        label = "vinylSongFocus"
    )
    val cardEdge = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    Box(modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(72.dp).graphicsLayer { alpha = focusAlpha },
            shape = RoundedCornerShape(50),
            color = cardColor,
            border = BorderStroke(1.dp, cardEdge),
            shadowElevation = 3.dp
        ) {}

        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClickLabel = if (playing) pauseLabel else playLabel, onClick = onClick)
                .semantics { selected = current }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp)
                    .shadow(4.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .border(1.5.dp, cardColor, CircleShape)
            ) {
                artwork(song, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = song.title,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (focused || current) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { unknownArtistLabel },
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (focused) {
                Box(
                    modifier = Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardEdge),
                        shadowElevation = 2.dp,
                        modifier = Modifier.graphicsLayer { alpha = focusAlpha }
                    ) {
                        Text(
                            text = if (playing) pauseLabel else playLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}
