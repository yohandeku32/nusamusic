package com.yohandeku32.nusamusic.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.zIndex
import com.yohandeku32.nusamusic.model.Song
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

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
    titleFontSize: Float,
    titleFontFamily: FontFamily,
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
            // Reserve exactly the same title slot for both one-line and
            // two-line song names. Otherwise SpaceBetween shifts the CD and
            // lower carousel each time a longer title wraps.
            // Preserve the compact CD design at the default 34sp setting,
            // while allowing the entire Settings text-size range to take effect.
            val titleSize = (titleFontSize * 26f / 34f).coerceIn(18f, 34f)
            val titleLineHeight = (titleSize * 1.25f).sp
            // Reserve space for up to two title lines plus the artist.
            // Center the pair inside it: one-line titles and their artist rise
            // together, while two-line titles stay close to their old position.
            val titleAndArtistSlotHeight = with(LocalDensity.current) {
                (titleSize * 2.7f).sp.toDp() + 4.dp + 16.sp.toDp()
            }
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
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.MoreHoriz, cdText("Pengaturan", "Settings"))
                    }
                }
                key(song?.id) {
                    CdTimeline(positionMsState, durationMs, song != null, onSeek)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(18.dp))
                    CdJewelCase(
                        modifier = Modifier.size(width = discSize * 1.12f, height = discSize),
                        artistName = song?.artist
                    ) { discModifier ->
                        // Switch to the new artwork immediately without a slide,
                        // scale or fade; keep the disc's normal rotation intact.
                        CdDisc(
                            song = song,
                            spinning = isPlaying && isVisible,
                            visible = isVisible,
                            artwork = artwork,
                            modifier = discModifier
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    // Center title and artist as one unit within a fixed-height
                    // slot. The gap stays at 4dp for both one- and two-line
                    // titles, without moving the CD or lower controls.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(titleAndArtistSlotHeight)
                            .padding(horizontal = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = song?.title ?: cdText("Pilih lagu", "Choose a song"),
                                fontFamily = titleFontFamily,
                                fontSize = titleSize.sp,
                                lineHeight = titleLineHeight,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = song?.artist?.takeIf { it.isNotBlank() }
                                    ?: cdText("Koleksi musik Anda", "Your music collection"),
                                color = Color(0xFFA5A5A0),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
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
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B1B1B), Color(0xFF151515))))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                        .navigationBarsPadding().padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(28.dp, 3.dp).clip(CircleShape).background(Color(0xFF343434)))
                    Spacer(Modifier.height(8.dp))
                    if (songs.isNotEmpty()) {
                        CdSongCarousel(songs, song?.id, thumbnailArtwork, onSelectSong)
                    } else {
                        emptyState()
                    }
                    OutlinedButton(
                        onClick = onLibrary,
                        modifier = Modifier.padding(top = 10.dp)
                            .defaultMinSize(minWidth = 68.dp, minHeight = 30.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.18f)
                        ))),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1A1A1A),
                            contentColor = Color(0xFFEAEAE4)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Open Library",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
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
    visible: Boolean,
    artwork: @Composable (Song?, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the rotating layer independent from the parent composition and
    // reuse a single frame loop across rapid Play / Pause changes. A finite
    // ease-in/out is important: exponential drag never fully stops and can
    // look like tiny irregular movements at the end on 120 Hz screens.
    val angle = remember { mutableFloatStateOf(0f) }
    val angularSpeed = remember { floatArrayOf(0f) }
    val playbackState = rememberUpdatedState(spinning)

    LaunchedEffect(visible) {
        if (!visible) {
            // Preserve the disc angle when the player goes off-screen.
            angularSpeed[0] = 0f
            return@LaunchedEffect
        }

        while (true) {
            if (!playbackState.value && angularSpeed[0] <= 0f) {
                // Zero frames/CPU work while paused and fully at rest.
                snapshotFlow { playbackState.value }.first { it }
            }
            var lastFrame = withFrameNanos { it }
            var requestedPlaying = playbackState.value
            var startSpeed = angularSpeed[0]
            var elapsed = 0f

            while (true) {
                val now = withFrameNanos { it }
                val dt = ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrame = now

                if (requestedPlaying != playbackState.value) {
                    // Retarget the existing motion without cancelling/restarting
                    // its coroutine, and keep angular velocity continuous.
                    requestedPlaying = playbackState.value
                    startSpeed = angularSpeed[0]
                    elapsed = 0f
                }

                val duration = if (requestedPlaying) 0.75f else 0.85f
                elapsed = (elapsed + dt).coerceAtMost(duration)
                val fraction = elapsed / duration
                val ease = fraction * fraction * (3f - 2f * fraction)
                val targetSpeed = if (requestedPlaying) 48f else 0f
                val nextSpeed = startSpeed + (targetSpeed - startSpeed) * ease
                angle.floatValue = (
                    angle.floatValue + (angularSpeed[0] + nextSpeed) * 0.5f * dt
                ) % 360f
                angularSpeed[0] = nextSpeed

                // End the slowdown at exactly zero speed instead of drifting
                // by sub-pixel amounts for several seconds.
                if (!requestedPlaying && elapsed >= duration) {
                    angularSpeed[0] = 0f
                    break
                }
            }
        }
    }
    CdDiscSurface(song, artwork, modifier, rotationDegrees = { angle.floatValue })
}

/** Shared CD finish; carousel discs reuse the player design without a rotation loop. */
@Composable
private fun CdDiscSurface(
    song: Song?,
    artwork: @Composable (Song?, Modifier) -> Unit,
    modifier: Modifier = Modifier,
    rotationDegrees: () -> Float = { 0f },
    shadowElevation: Dp = 12.dp
) {
    Box(
        if (shadowElevation > 0.dp) modifier.shadow(shadowElevation, CircleShape) else modifier,
        contentAlignment = Alignment.Center
    ) {
        // One cached GPU layer handles both disc clipping and rotation.
        // The album bitmap and hologram are rasterized when content changes,
        // while playback frames only update the transform.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                rotationZ = rotationDegrees()
                shape = CircleShape
                clip = true
                compositingStrategy = CompositingStrategy.Offscreen
            }
        ) {
            artwork(song, Modifier.fillMaxSize())
            // Cache all expensive gradient shaders. Only the already-cached
            // disc layer rotates; a 120 Hz transform never rebuilds its art.
            Spacer(Modifier.matchParentSize().drawWithCache {
                val radius = size.minDimension / 2f
                val discCenter = Offset(size.width / 2f, size.height / 2f)
                val satinFinish = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.13f),
                        Color(0xFFD0EFF5).copy(alpha = 0.045f),
                        Color.Transparent,
                        Color(0xFF060B14).copy(alpha = 0.055f)
                    ),
                    start = Offset(size.width * 0.12f, size.height * 0.05f),
                    end = Offset(size.width * 0.88f, size.height * 0.98f)
                )
                val outerDepth = Brush.radialGradient(
                    0.00f to Color.Transparent,
                    0.70f to Color.Transparent,
                    0.87f to Color.Black.copy(alpha = 0.012f),
                    0.98f to Color.Black.copy(alpha = 0.10f),
                    1.00f to Color.Black.copy(alpha = 0.14f),
                    center = discCenter,
                    radius = radius
                )
                val lightSweep = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent,
                        Color(0xFF8ADBEA).copy(alpha = 0.065f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    ), center = discCenter
                )
                val prismRim = Brush.sweepGradient(
                    listOf(
                        Color(0xFFADE5F2).copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.72f),
                        Color(0xFFBFC2EC).copy(alpha = 0.30f),
                        Color(0xFFF6E6BC).copy(alpha = 0.53f),
                        Color(0xFFADE5F2).copy(alpha = 0.30f)
                    ), center = discCenter
                )
                // Neutral silver highlights replace the rainbow diffraction effect.
                // Cache the shader with the disc artwork, not per rotation frame.
                val metallicHubBand = Brush.sweepGradient(
                    listOf(
                        Color(0xFF80888C),
                        Color(0xFFD3DADE),
                        Color(0xFFF5F7F7),
                        Color(0xFF9BA5AA),
                        Color(0xFF6F787D),
                        Color(0xFFCAD2D5),
                        Color(0xFFE8ECEE),
                        Color(0xFF80888C)
                    ),
                    center = discCenter
                )
                // These two radial gradients depend only on disc dimensions:
                // prepare their shaders with the other cached finishes, never in
                // the onDrawBehind path during a spinning frame.
                val labelPlastic = Brush.radialGradient(
                    0.00f to Color(0xFF858E93).copy(alpha = 0.42f),
                    0.37f to Color(0xFFADB6BA).copy(alpha = 0.34f),
                    0.70f to Color(0xFFE2E7E7).copy(alpha = 0.38f),
                    0.91f to Color(0xFF9DAAB0).copy(alpha = 0.36f),
                    1.00f to Color(0xFFDEE7E9).copy(alpha = 0.44f),
                    center = discCenter,
                    radius = radius * 0.324f
                )
                val innerClamp = Brush.radialGradient(
                    listOf(
                        Color(0xFF6C7477),
                        Color(0xFFAEB7BA),
                        Color(0xFFD8DEDF),
                        Color(0xFF7E898D)
                    ),
                    center = discCenter,
                    radius = radius * 0.157f
                )
                onDrawBehind {
                    // A light satin finish and softer edge vignette keep the album
                    // cover vivid instead of hiding it behind dark shading.
                    drawCircle(satinFinish, radius = radius)
                    drawCircle(lightSweep, radius = radius)
                    drawCircle(outerDepth, radius = radius)
                    for (ring in 1..7) {
                        drawCircle(
                            Color.White.copy(alpha = 0.035f),
                            radius * (0.70f + ring * 0.035f),
                            style = Stroke(0.45.dp.toPx())
                        )
                    }
                    drawCircle(
                        brush = prismRim,
                        radius = radius * 0.970f,
                        style = Stroke(width = radius * 0.019f)
                    )
                    drawCircle(
                        Color.Black.copy(alpha = 0.20f),
                        radius - 0.75.dp.toPx(),
                        style = Stroke(1.15.dp.toPx())
                    )
                    drawCircle(
                        Color.White.copy(alpha = 0.25f),
                        radius - 2.dp.toPx(),
                        style = Stroke(0.65.dp.toPx())
                    )
                    // One broad highlight hugs the curved finish; it is not
                    // a scratch, nor an animation layered over the jewel case.
                    drawArc(
                        Color.White.copy(alpha = 0.20f),
                        startAngle = 205f, sweepAngle = 88f, useCenter = false,
                        topLeft = Offset(radius * 0.07f, radius * 0.07f),
                        size = Size(radius * 1.86f, radius * 1.86f),
                        style = Stroke(width = 1.25.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )

                // Layered polycarbonate centre: a slim brushed-silver band,
                // transparent inner plastic, a recessed silver clamp and the
                // genuine-sized spindle hole. All radii share one centre.
                val hub = center
                drawCircle(Color.Black.copy(alpha = 0.11f), radius * 0.405f, hub)
                drawCircle(
                    brush = metallicHubBand,
                    radius = radius * 0.370f,
                    center = hub,
                    alpha = 0.52f,
                    style = Stroke(width = radius * 0.038f)
                )
                drawCircle(
                    Color.White.copy(alpha = 0.30f), radius * 0.406f, hub,
                    style = Stroke(0.75.dp.toPx())
                )
                drawCircle(
                    Color(0xFF59656B).copy(alpha = 0.22f), radius * 0.336f, hub,
                    style = Stroke(1.0.dp.toPx())
                )
                // Lightly tinted inner plastic lets more of the artwork show through.
                drawCircle(
                    brush = labelPlastic,
                    radius = radius * 0.324f,
                    center = hub
                )
                drawCircle(
                    Color.White.copy(alpha = 0.26f), radius * 0.320f, hub,
                    style = Stroke(0.65.dp.toPx())
                )
                drawCircle(
                    Color(0xFF495157).copy(alpha = 0.14f), radius * 0.272f, hub,
                    style = Stroke(0.8.dp.toPx())
                )
                drawCircle(
                    Color.White.copy(alpha = 0.14f), radius * 0.249f, hub,
                    style = Stroke(0.65.dp.toPx())
                )
                // Narrow recessed mounting ring rather than a flat silver disc.
                drawCircle(Color.Black.copy(alpha = 0.24f), radius * 0.181f, hub)
                drawCircle(
                    brush = innerClamp,
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
            })
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
    if (songs.isEmpty()) return
    val currentIndex = remember(songs, currentId) {
        songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    }
    val looping = songs.size >= CD_CAROUSEL_VISIBLE_COUNT
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialCdCarouselIndex(currentIndex, songs.size)
    )
    // A virtual list supplies two neighbours on each side even for the first
    // and last song. Smaller libraries keep their actual count, without copies.
    LaunchedEffect(currentId, songs) {
        val layout = listState.layoutInfo
        val center = (layout.viewportStartOffset + layout.viewportEndOffset) * 0.5f
        val around = layout.visibleItemsInfo.minByOrNull {
            abs(it.offset + it.size * 0.5f - center)
        }?.index ?: listState.firstVisibleItemIndex
        val target = if (looping && around < CD_CAROUSEL_VISIBLE_COUNT) {
            initialCdCarouselIndex(currentIndex, songs.size)
        } else {
            nearestCdCarouselIndex(currentIndex, songs.size, around)
        }
        listState.scrollToItem(target)
    }
    val centerItemIndex by remember(listState) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) * 0.5f
            layout.visibleItemsInfo.minByOrNull {
                abs(it.offset + it.size * 0.5f - center)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }
    val density = LocalDensity.current
    // Cache a small blur ladder rather than creating GPU effects on every
    // scrolling frame. Older Android versions still get the soft edge fade.
    val edgeBlurEffects = remember(density) {
        List(9) { level ->
            if (level > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val radius = level * 0.5f * density.density
                android.graphics.RenderEffect.createBlurEffect(
                    radius, radius, android.graphics.Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            } else null
        }
    }
    BoxWithConstraints(
        Modifier.fillMaxWidth().clipToBounds().drawWithCache {
            val edgeFade = Brush.horizontalGradient(
                0f to Color(0xFF191919).copy(alpha = 0.75f),
                0.18f to Color.Transparent,
                0.82f to Color.Transparent,
                1f to Color(0xFF191919).copy(alpha = 0.75f)
            )
            onDrawWithContent {
                drawContent()
                drawRect(edgeFade)
            }
        }
    ) {
        val slotWidth = maxWidth / CD_CAROUSEL_VISIBLE_COUNT
        // Overlapping discs retain a prominent centre CD while five centres
        // remain visible on narrow phones. Only visible artwork is composed.
        val discSize = minOf(128.dp, maxWidth * 0.34f)
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = (maxWidth - slotWidth) / 2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(discSize + 64.dp)
        ) {
            items(count = if (looping) Int.MAX_VALUE else songs.size, key = { it }) { index ->
                val item = songs[index % songs.size]
                val active = item.id == currentId
                Box(
                    Modifier.width(slotWidth).height(discSize)
                        .zIndex((3 - abs(index - centerItemIndex)).coerceAtLeast(0).toFloat())
                        .clickable(interactionSource = null, indication = null, role = Role.Button) {
                            onSelectSong(item)
                        }
                        .semantics {
                            selected = active
                            contentDescription = "${item.title}, ${item.artist}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CdDiscSurface(
                        item, artwork,
                        modifier = Modifier.requiredSize(discSize).graphicsLayer {
                            val layout = listState.layoutInfo
                            val center = (layout.viewportStartOffset + layout.viewportEndOffset) * 0.5f
                            val visibleItem = layout.visibleItemsInfo.firstOrNull { it.index == index }
                            val slotPx = with(density) { slotWidth.toPx() }
                            val distance = if (visibleItem != null && slotPx > 0f) {
                                abs((visibleItem.offset + visibleItem.size * 0.5f - center) / slotPx)
                            } else 2.5f
                            val edge = (distance / 2f).coerceIn(0f, 1f)
                            val scale = 1f - 0.32f * edge
                            scaleX = scale
                            scaleY = scale
                            translationY = with(density) { (-10f + 34f * edge.pow(1.65f)).dp.toPx() }
                            val softness = ((distance - 0.75f) / 1.25f).coerceIn(0f, 1f)
                            alpha = 1f - 0.22f * softness
                            renderEffect = edgeBlurEffects[(softness * 8f).roundToInt()]
                        },
                        shadowElevation = 0.dp
                    )
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
    // Reserve identical fixed-width slots for both labels. Proportional time
    // text would otherwise resize the weighted Slider whenever digits change
    // (e.g. 9:59 → 10:00), shifting the entire progress track.
    val timeLabelWidth = 56.dp
    LaunchedEffect(durationMs, hasSong) { drag = null }
    Row(
        Modifier.padding(horizontal = 24.dp).fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
            .background(Color(0xFF181818), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cdTime((value * duration).toLong()),
            modifier = Modifier.width(timeLabelWidth),
            color = Color(0xFFC9C9C1),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            textAlign = TextAlign.Start
        )
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
        Text(
            text = cdTime(duration),
            modifier = Modifier.width(timeLabelWidth),
            color = Color(0xFF92928D),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            textAlign = TextAlign.End
        )
    }
}

private fun cdTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
