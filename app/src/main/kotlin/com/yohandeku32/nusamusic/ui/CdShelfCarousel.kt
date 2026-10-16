package com.yohandeku32.nusamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

private fun shelfText(id: String, en: String) =
    if (Locale.getDefault().language.equals("id", ignoreCase = true)) id else en

@Composable
internal fun CdShelfCarousel(
    songs: List<Song>,
    currentId: Long?,
    artwork: @Composable (Song?, Modifier) -> Unit,
    onPlaySong: (Song) -> Unit
) {
    if (songs.isEmpty()) return
    val songIndex = remember(songs, currentId) { songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialCdCarouselIndex(songIndex, songs.size))
    val preview = remember { CdShelfPreviewState() }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val selection = preview.selection
    val phase = preview.phase
    val looping = songs.size >= CD_CAROUSEL_VISIBLE_COUNT

    fun returnToShelf(play: Boolean) {
        val item = preview.selection ?: return
        if (!preview.beginReturn(item, play)) return
        scope.launch {
            progress.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            preview.completeReturn(item)?.let(onPlaySong)
        }
    }

    BackHandler(enabled = selection != null) { returnToShelf(false) }
    LaunchedEffect(selection) {
        if (selection != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            preview.ready(selection)
        }
    }
    LaunchedEffect(songs) {
        val item = preview.selection
        if (item != null && songs.none { it.id == item.song.id }) preview.discard()
    }
    LaunchedEffect(currentId, songs) {
        if (preview.selection != null) return@LaunchedEffect
        val layout = listState.layoutInfo
        val center = (layout.viewportStartOffset + layout.viewportEndOffset) * 0.5f
        val around = layout.visibleItemsInfo.minByOrNull {
            abs(it.offset + it.size * 0.5f - center)
        }?.index ?: listState.firstVisibleItemIndex
        val target = if (looping && around < CD_CAROUSEL_VISIBLE_COUNT) {
            initialCdCarouselIndex(songIndex, songs.size)
        } else nearestCdCarouselIndex(songIndex, songs.size, around)
        listState.scrollToItem(target)
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val caseWidth = minOf(132.dp, maxWidth * 0.36f)
        val caseHeight = caseWidth * 0.90f
        // Turn the angled cases onto their sides, keeping the spine text
        // horizontal as the shelf stacks and scrolls from top to bottom.
        val slotHeight = caseWidth * 0.30f + 2.dp
        val rackHeight = slotHeight * CD_CAROUSEL_VISIBLE_COUNT
        val rackWidth = caseHeight + 24.dp
        val centeringPadding = (rackHeight - slotHeight) / 2
        val stageHeight = maxOf(rackHeight + 16.dp, caseHeight * 1.45f + 20.dp)
        val density = LocalDensity.current
        val caseWidthPx = with(density) { caseWidth.toPx() }
        val caseHeightPx = with(density) { caseHeight.toPx() }
        val stageHeightPx = with(density) { stageHeight.toPx() }
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val rackTopPx = with(density) { (stageHeight - rackHeight).toPx() / 2f }
        Box(Modifier.fillMaxWidth().height(stageHeight)) {
            LazyColumn(
                state = listState,
                userScrollEnabled = selection == null,
                flingBehavior = rememberSnapFlingBehavior(listState),
                contentPadding = PaddingValues(vertical = centeringPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center).width(rackWidth).height(rackHeight).graphicsLayer {
                    clip = true
                    compositingStrategy = CompositingStrategy.Offscreen
                }.drawWithCache {
                    val edgeFade = Brush.verticalGradient(
                        0f to Color.Transparent, 0.10f to Color.Black,
                        0.90f to Color.Black, 1f to Color.Transparent
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(edgeFade, blendMode = BlendMode.DstIn)
                    }
                }
            ) {
                items(count = if (looping) Int.MAX_VALUE else songs.size, key = { it }) { index ->
                    val song = songs[index % songs.size]
                    val playing = song.id == currentId
                    val lifted = selection?.index == index
                    Box(
                        Modifier.fillMaxWidth().height(slotHeight)
                            .clickable(
                                enabled = selection == null,
                                interactionSource = null, indication = null, role = Role.Button
                            ) {
                                scope.launch {
                                    listState.stopScroll()
                                    val layout = listState.layoutInfo
                                    val item = layout.visibleItemsInfo.firstOrNull { it.index == index }
                                    val sourceY = item?.let {
                                        rackTopPx + it.offset - layout.viewportStartOffset + it.size * 0.5f
                                    } ?: stageHeightPx / 2f
                                    preview.show(CdShelfSelection(index, song, viewportWidthPx / 2f, sourceY))
                                }
                            }
                            .semantics {
                                selected = playing
                                contentDescription = "${song.title}, ${song.artist}"
                                stateDescription = shelfText("Ketuk untuk memilih", "Tap to preview")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CdAlbumCase(
                            song, artwork, playing,
                            Modifier.requiredSize(caseWidth, caseHeight).graphicsLayer {
                                rotationZ = 90f
                                alpha = if (lifted) 0f else 1f
                            }
                        )
                    }
                }
            }
            if (selection != null) {
                Box(
                    Modifier.matchParentSize().graphicsLayer { alpha = progress.value * 0.64f }
                        .background(Color(0xFF0B0D0D))
                        .clickable(interactionSource = null, indication = null) { returnToShelf(false) }
                )
                Box(
                    Modifier.requiredSize(caseWidth, caseHeight)
                        .graphicsLayer {
                            val p = progress.value
                            translationX = selection.originCenterX - caseWidthPx / 2f +
                                (viewportWidthPx / 2f - selection.originCenterX) * p
                            translationY = selection.originCenterY - caseHeightPx / 2f +
                                (stageHeightPx / 2f - selection.originCenterY) * p
                            rotationZ = 90f * (1f - p)
                            scaleX = 1f + 0.30f * p
                            scaleY = 1f + 0.30f * p
                        }
                        .pointerInput(selection) { detectTapGestures(onTap = {}) }
                ) {
                    CdAlbumCase(
                        selection.song, artwork, selection.song.id == currentId,
                        modifier = Modifier.matchParentSize(),
                        angle = { CD_SHELF_REST_ANGLE * (1f - progress.value) }
                    )
                    // Actions arrive only after the case has come to rest.
                    // Confirming playback first completes the return animation.
                    if (phase == CdShelfPreviewPhase.READY || phase == CdShelfPreviewPhase.RETURNING) {
                        FilledIconButton(
                            onClick = { returnToShelf(true) },
                            enabled = phase == CdShelfPreviewPhase.READY,
                            modifier = Modifier.align(Alignment.Center).size(48.dp)
                                .graphicsLayer { alpha = progress.value }
                                .shadow(6.dp, CircleShape),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFF4F3EA), contentColor = Color(0xFF171B1C),
                                disabledContainerColor = Color(0xFFF4F3EA), disabledContentColor = Color(0xFF171B1C)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, shelfText("Putar ${selection.song.title}", "Play ${selection.song.title}"), Modifier.size(28.dp))
                        }
                    }
                    if (phase != CdShelfPreviewPhase.RETURNING) {
                        IconButton(
                            onClick = { returnToShelf(false) },
                            modifier = Modifier.align(Alignment.TopEnd).size(48.dp)
                                .graphicsLayer { alpha = progress.value }
                        ) {
                            Icon(
                                Icons.Default.Close, shelfText("Kembalikan ke rak", "Return to shelf"),
                                Modifier.background(Color.Black.copy(alpha = 0.60f), CircleShape).padding(4.dp).size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
