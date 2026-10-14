package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale

private val CdBackground = Color(0xFF111111)
private val CdAccent = Color(0xFFB5DF51)
private fun cdText(id: String, en: String) =
    if (Locale.getDefault().language.equals("id", ignoreCase = true)) id else en

/** A presentation of the existing player, with no separate audio or queue state. */
@Composable
fun CdPlayer(
    song: Song?,
    songs: List<Song>,
    isPlaying: Boolean,
    isVisible: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    viewportHeight: Dp,
    titleFontFamily: FontFamily,
    titleFontSize: Float,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSelectSong: (Song) -> Unit,
    onSettings: () -> Unit,
    onLibrary: () -> Unit,
    artwork: @Composable (Song?, Modifier) -> Unit,
    emptyState: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = darkColorScheme(
        background = CdBackground,
        surface = Color(0xFF191919),
        primary = CdAccent,
        onPrimary = CdBackground,
        onSurface = Color(0xFFF2F0E9),
        onSurfaceVariant = Color(0xFFA5A5A0)
    )) {
        CompositionLocalProvider(LocalContentColor provides Color(0xFFF2F0E9)) {
        BoxWithConstraints(modifier.background(CdBackground)) {
            // Reserve the transport and collection first; the parent scrolls
            // naturally for compact screens or enlarged accessibility text.
            val discSize = minOf(maxWidth * 0.76f, (viewportHeight - 520.dp).coerceIn(160.dp, 340.dp))
            Column(
                Modifier.fillMaxWidth().heightIn(min = viewportHeight).statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NUSA", fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("COMPACT DISC", color = Color(0xFF92928D), fontSize = 9.sp, letterSpacing = 2.sp)
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.MoreHoriz, cdText("Pengaturan", "Settings"))
                    }
                }
                key(song?.id) {
                    CdTimeline(positionMsState, durationMs, song != null, onSeek)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(18.dp))
                    CdJewelCase(Modifier.size(width = discSize * 1.12f, height = discSize)) { discModifier ->
                        CdDisc(song, isPlaying && isVisible, artwork, discModifier)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        song?.title ?: cdText("Pilih lagu", "Choose a song"),
                        fontFamily = titleFontFamily,
                        fontSize = titleFontSize.coerceIn(18f, 26f).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        song?.artist?.takeIf { it.isNotBlank() } ?: cdText("Koleksi musik Anda", "Your music collection"),
                        color = Color(0xFFA5A5A0), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                    Row(
                        Modifier.padding(top = 12.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconToggleButton(checked = shuffleEnabled, onCheckedChange = { onShuffle() }, enabled = song != null) {
                            Icon(Icons.Default.Shuffle, cdText("Acak", "Shuffle"), Modifier.size(19.dp), tint = if (shuffleEnabled) CdAccent else Color.Gray)
                        }
                        IconButton(onClick = onPrevious, enabled = song != null) {
                            Icon(Icons.Default.SkipPrevious, cdText("Sebelumnya", "Previous"), Modifier.size(27.dp))
                        }
                        FilledIconButton(
                            onClick = onPlayPause, modifier = Modifier.size(54.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEDEDE7), contentColor = CdBackground)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) cdText("Jeda", "Pause") else cdText("Putar", "Play"), Modifier.size(29.dp))
                        }
                        IconButton(onClick = onNext, enabled = song != null) {
                            Icon(Icons.Default.SkipNext, cdText("Berikutnya", "Next"), Modifier.size(27.dp))
                        }
                        IconButton(onClick = onRepeat, enabled = song != null) {
                            Icon(if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                when (repeatMode) {
                                    1 -> cdText("Ulangi satu lagu", "Repeat one")
                                    2 -> cdText("Ulangi semua", "Repeat all")
                                    else -> cdText("Ulangi nonaktif", "Repeat off")
                                }, Modifier.size(19.dp), tint = if (repeatMode != 0) CdAccent else Color.Gray)
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B1B1B), Color(0xFF151515))))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                        .navigationBarsPadding().padding(top = 12.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(28.dp, 3.dp).clip(CircleShape).background(Color(0xFF343434)))
                    Spacer(Modifier.height(12.dp))
                    if (songs.isNotEmpty()) {
                        CdSongCarousel(songs, song?.id, artwork, onSelectSong)
                        Text(cdText("Geser untuk memilih · ketuk untuk putar", "Swipe to browse · tap to play"),
                            color = Color(0xFF92928D), fontSize = 11.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    } else {
                        emptyState()
                    }
                    TextButton(onClick = onLibrary) {
                        Text(cdText("Buka koleksi", "Open library"), color = Color(0xFFEAEAE4), fontSize = 12.sp)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CdDisc(
    song: Song?,
    spinning: Boolean,
    artwork: @Composable (Song?, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            var previous = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    rotation = (rotation + (now - previous) / 1_000_000_000f * 18f) % 360f
                    previous = now
                }
            }
        }
    }
    Box(modifier.shadow(18.dp, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }.clip(CircleShape)) {
            artwork(song, Modifier.fillMaxSize())
            Canvas(Modifier.matchParentSize()) {
                val radius = size.minDimension / 2f
                drawCircle(Brush.sweepGradient(listOf(
                    Color.Transparent, Color.White.copy(alpha = 0.24f), Color.Transparent,
                    Color.Black.copy(alpha = 0.15f), Color.Transparent,
                    Color.White.copy(alpha = 0.18f), Color.Transparent
                )))
                for (ring in 1..9) {
                    drawCircle(Color.White.copy(alpha = 0.045f), radius * (0.68f + ring * 0.03f), style = Stroke(0.5.dp.toPx()))
                }
                drawCircle(Color.White.copy(alpha = 0.3f), radius - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.14f), radius * 0.18f)
                drawCircle(Color.White.copy(alpha = 0.25f), radius * 0.18f, style = Stroke(0.8.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.1f), radius * 0.12f, style = Stroke(1.dp.toPx()))
            }
        }
        Box(Modifier.fillMaxSize(0.065f).clip(CircleShape).background(CdBackground).border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape))
    }
}

@Composable
private fun CdSongCarousel(
    songs: List<Song>, currentId: Long?,
    artwork: @Composable (Song?, Modifier) -> Unit,
    onSelectSong: (Song) -> Unit
) {
    val currentIndex = songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    LaunchedEffect(currentId, songs) {
        listState.animateScrollToItem(currentIndex)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = ((maxWidth - 88.dp) / 2).coerceAtLeast(0.dp)),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(94.dp)
        ) {
            items(songs, key = { it.id }) { item ->
                val active = item.id == currentId
                val scale by animateFloatAsState(if (active) 1f else 0.76f, label = "CD selection")
                Box(
                    Modifier.size(88.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (active) 1f else 0.55f }
                        .clip(CircleShape)
                        .border(1.dp, if (active) Color(0xFF999991) else Color.Transparent, CircleShape)
                        .clickable(role = Role.Button) { onSelectSong(item) }
                        .semantics { selected = active; contentDescription = "${item.title}, ${item.artist}" }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CdDisc(item, false, artwork, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CdTimeline(position: State<Long>, durationMs: Long, hasSong: Boolean, onSeek: (Long) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(0L)
    val value = drag ?: if (duration > 0) (position.value.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val enabled = hasSong && duration > 0L
    LaunchedEffect(durationMs, hasSong) { drag = null }
    Row(
        Modifier.padding(horizontal = 24.dp).fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
            .background(Color(0xFF181818), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(cdTime((value * duration).toLong()), color = Color(0xFFC9C9C1), fontSize = 11.sp)
        Slider(
            value = value, onValueChange = { drag = it },
            onValueChangeFinished = { drag?.let { onSeek((it * duration).toLong()) }; drag = null },
            enabled = enabled,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                .semantics { contentDescription = cdText("Posisi pemutaran", "Playback position") },
            thumb = { Box(Modifier.size(3.dp, 20.dp).background(if (enabled) CdAccent else Color.Gray, CircleShape)) },
            track = {
                Canvas(Modifier.fillMaxWidth().height(20.dp)) {
                    for (tick in 0..32) {
                        val x = size.width * tick / 32f
                        val height = if (tick % 4 == 0) size.height * 0.8f else size.height * 0.5f
                        drawLine(if (enabled && tick / 32f <= value) CdAccent.copy(alpha = 0.6f) else Color(0xFF41413D),
                            Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2), 1.dp.toPx())
                    }
                }
            }
        )
        Text(cdTime(duration), color = Color(0xFF92928D), fontSize = 11.sp)
    }
}

private fun cdTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
