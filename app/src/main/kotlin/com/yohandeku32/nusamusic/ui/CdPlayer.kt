package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale

private val CdBackground = Color(0xFF111111)
private val CdAccent = Color(0xFFB5DF51)
private fun cdText(id: String, en: String) =
    if (Locale.getDefault().language.equals("id", ignoreCase = true)) id else en

/**
 * A font-independent Nothing-inspired dot-matrix title. Render a two-line
 * Unicode text mask only when its text or width changes, then replace the
 * strokes with tiny round LEDs. No external font download or per-frame work.
 */
@Composable
private fun CdDotTitle(
    title: String,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val textPx = with(density) { fontSize.coerceIn(18f, 26f).sp.toPx() }
        val pitchPx = with(density) { 3.dp.toPx() }
        val dots = remember(title, widthPx, textPx, pitchPx) {
            rasterizeDotTitle(title, widthPx, textPx, pitchPx)
        }
        Image(
            bitmap = dots.asImageBitmap(),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { dots.height.toDp() })
        )
    }
}

private fun rasterizeDotTitle(
    title: String,
    widthPx: Int,
    textPx: Float,
    pitchPx: Float
): android.graphics.Bitmap {
    // StaticLayout handles ellipsis, international characters and 2-line
    // wrapping before rasterisation. The user does not need a font asset.
    val ink = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = textPx
        typeface = android.graphics.Typeface.create(
            "sans-serif-medium", android.graphics.Typeface.BOLD
        )
    }
    val layout = android.text.StaticLayout.Builder.obtain(
        title, 0, title.length, ink, widthPx
    )
        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
        .setIncludePad(false)
        .setMaxLines(2)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()
    val heightPx = layout.height.coerceAtLeast(1)
    val mask = android.graphics.Bitmap.createBitmap(
        widthPx, heightPx, android.graphics.Bitmap.Config.ARGB_8888
    )
    layout.draw(android.graphics.Canvas(mask))
    val pixels = IntArray(widthPx * heightPx)
    mask.getPixels(pixels, 0, widthPx, 0, 0, widthPx, heightPx)
    mask.recycle()

    val output = android.graphics.Bitmap.createBitmap(
        widthPx, heightPx, android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(output)
    val pointInk = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(242, 240, 233)
        style = android.graphics.Paint.Style.FILL
    }
    val pitch = pitchPx.toInt().coerceAtLeast(3)
    val half = pitch / 2
    val radius = pitch * 0.33f
    for (y in half until heightPx step pitch) {
        for (x in half until widthPx step pitch) {
            // Sample a tiny cross to preserve fine strokes between dot centres.
            val sampleRadius = (pitch / 4).coerceAtLeast(1)
            var opacity = 0
            for ((dx, dy) in arrayOf(
                0 to 0, -sampleRadius to 0, sampleRadius to 0,
                0 to -sampleRadius, 0 to sampleRadius
            )) {
                val sx = (x + dx).coerceIn(0, widthPx - 1)
                val sy = (y + dy).coerceIn(0, heightPx - 1)
                opacity = maxOf(opacity, (pixels[sy * widthPx + sx] ushr 24) and 0xFF)
            }
            if (opacity > 48) {
                canvas.drawCircle(x.toFloat(), y.toFloat(), radius, pointInk)
            }
        }
    }
    return output
}

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
    thumbnailArtwork: @Composable (Song?, Modifier) -> Unit,
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
                    CdDotTitle(
                        title = song?.title ?: cdText("Pilih lagu", "Choose a song"),
                        fontSize = titleFontSize,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
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
                        CdSongCarousel(songs, song?.id, thumbnailArtwork, onSelectSong)
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
                    rotation = (rotation + (now - previous) / 1_000_000_000f * 36f) % 360f
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

                // A few tiny, irregular hairline scuffs follow the circular
                // grooves. They rotate WITH the printed artwork, stay subtle,
                // and never cover the central molded hub.
                val hairline = 0.55.dp.toPx()
                val scratches = arrayOf(
                    floatArrayOf(0.44f, 32f, 13f),
                    floatArrayOf(0.60f, 161f, 7f),
                    floatArrayOf(0.70f, 278f, 18f),
                    floatArrayOf(0.78f, 73f, 6f),
                    floatArrayOf(0.84f, 207f, 11f),
                    floatArrayOf(0.91f, 322f, 8f),
                    floatArrayOf(0.95f, 121f, 16f),
                    floatArrayOf(0.55f, 340f, 5f),
                    floatArrayOf(0.63f, 106f, 17f),
                    floatArrayOf(0.72f, 12f, 6f),
                    floatArrayOf(0.82f, 254f, 15f),
                    floatArrayOf(0.88f, 149f, 9f)
                )
                scratches.forEachIndexed { index, scratch ->
                    val distance = radius * scratch[0]
                    drawArc(
                        color = if (index % 3 == 0) {
                            Color.Black.copy(alpha = 0.13f)
                        } else {
                            Color.White.copy(alpha = 0.18f)
                        },
                        startAngle = scratch[1],
                        sweepAngle = scratch[2],
                        useCenter = false,
                        topLeft = Offset(center.x - distance, center.y - distance),
                        size = Size(distance * 2f, distance * 2f),
                        style = Stroke(width = hairline, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                // Light radial marks add a barely visible used-disc texture.
                drawLine(
                    Color.White.copy(alpha = 0.09f),
                    start = Offset(center.x + radius * 0.46f, center.y - radius * 0.40f),
                    end = Offset(center.x + radius * 0.56f, center.y - radius * 0.49f),
                    strokeWidth = hairline
                )
                drawLine(
                    Color.Black.copy(alpha = 0.10f),
                    start = Offset(center.x - radius * 0.76f, center.y + radius * 0.18f),
                    end = Offset(center.x - radius * 0.89f, center.y + radius * 0.21f),
                    strokeWidth = hairline
                )

                // Layered polycarbonate centre: an iridescent annular band,
                // transparent inner plastic, a recessed silver clamp and the
                // genuine-sized spindle hole. All radii share one centre.
                val hub = center
                drawCircle(Color.Black.copy(alpha = 0.18f), radius * 0.405f, hub)
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFFC2DDE4),
                            Color(0xFFF5F1CE),
                            Color(0xFFDAB4D7),
                            Color(0xFFB5D1F3),
                            Color(0xFFF3E6B4),
                            Color(0xFFE0EBEE),
                            Color(0xFFC2DDE4)
                        ),
                        center = hub
                    ),
                    radius = radius * 0.370f,
                    center = hub,
                    alpha = 0.78f,
                    style = Stroke(width = radius * 0.068f)
                )
                drawCircle(
                    Color.White.copy(alpha = 0.57f), radius * 0.406f, hub,
                    style = Stroke(0.75.dp.toPx())
                )
                drawCircle(
                    Color(0xFF59656B).copy(alpha = 0.51f), radius * 0.336f, hub,
                    style = Stroke(1.2.dp.toPx())
                )
                // Faintly translucent inner label plastic retains a hint of the cover.
                drawCircle(
                    brush = Brush.radialGradient(
                        0.00f to Color(0xFF858E93).copy(alpha = 0.75f),
                        0.37f to Color(0xFFADB6BA).copy(alpha = 0.67f),
                        0.70f to Color(0xFFE2E7E7).copy(alpha = 0.76f),
                        0.91f to Color(0xFF9DAAB0).copy(alpha = 0.77f),
                        1.00f to Color(0xFFDEE7E9).copy(alpha = 0.84f),
                        center = hub,
                        radius = radius * 0.324f
                    ),
                    radius = radius * 0.324f,
                    center = hub
                )
                drawCircle(
                    Color.White.copy(alpha = 0.60f), radius * 0.320f, hub,
                    style = Stroke(0.65.dp.toPx())
                )
                drawCircle(
                    Color(0xFF495157).copy(alpha = 0.28f), radius * 0.272f, hub,
                    style = Stroke(0.8.dp.toPx())
                )
                drawCircle(
                    Color.White.copy(alpha = 0.30f), radius * 0.249f, hub,
                    style = Stroke(0.65.dp.toPx())
                )
                // Narrow recessed mounting ring rather than a flat silver disc.
                drawCircle(Color.Black.copy(alpha = 0.24f), radius * 0.181f, hub)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFF6C7477),
                            Color(0xFFAEB7BA),
                            Color(0xFFD8DEDF),
                            Color(0xFF7E898D)
                        ),
                        center = hub,
                        radius = radius * 0.157f
                    ),
                    radius = radius * 0.157f,
                    center = hub
                )
                drawCircle(
                    Color.White.copy(alpha = 0.55f), radius * 0.156f, hub,
                    style = Stroke(0.75.dp.toPx())
                )
                drawCircle(
                    Color(0xFF353D42).copy(alpha = 0.34f), radius * 0.095f, hub,
                    style = Stroke(1.dp.toPx())
                )
                drawCircle(Color(0xFF101317), radius * 0.067f, hub)
                drawCircle(
                    Color.White.copy(alpha = 0.37f), radius * 0.070f, hub,
                    style = Stroke(0.75.dp.toPx())
                )
            }
        }
        Box(Modifier.fillMaxSize(0.065f).clip(CircleShape).background(CdBackground).border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape))
    }
}

/** Lightweight static carousel preview: no rotating frame effects or large artwork. */
@Composable
private fun CdThumbnail(
    song: Song,
    artwork: @Composable (Song?, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.clip(CircleShape), contentAlignment = Alignment.Center) {
        artwork(song, Modifier.fillMaxSize())
        Canvas(Modifier.matchParentSize()) {
            val radius = size.minDimension / 2f
            drawCircle(
                Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                ),
                radius = radius
            )
            drawCircle(Color.Black.copy(alpha = 0.23f), radius * 0.36f)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF8C979D), Color(0xFFD0D8DB), Color(0xFF7E878A))
                ),
                radius * 0.31f
            )
            drawCircle(Color(0xFF1A1D20), radius * 0.065f)
        }
    }
}

@Composable
private fun CdSongCarousel(
    songs: List<Song>, currentId: Long?,
    artwork: @Composable (Song?, Modifier) -> Unit,
    onSelectSong: (Song) -> Unit
) {
    val currentIndex = remember(songs, currentId) {
        songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    // A song change (especially Shuffle) can jump across thousands of items:
    // snap directly rather than animating every intermediate row and decoding
    // their covers just to scroll past them.
    LaunchedEffect(currentId, songs) {
        listState.scrollToItem(currentIndex)
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
                    CdThumbnail(item, artwork, Modifier.fillMaxSize())
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
