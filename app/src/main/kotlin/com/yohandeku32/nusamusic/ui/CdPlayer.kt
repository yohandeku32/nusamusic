package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.gestures.stopScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

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
    roundedCarouselCorners: Boolean,
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
        val transfer = remember { CdDiscTransferState() }
        val flightProgress = remember { Animatable(0f) }
        val seatingProgress = remember { Animatable(0f) }
        val liftProgress = remember { Animatable(0f) }
        val idlePosition = remember { mutableStateOf(0L) }
        var rootOrigin by remember { mutableStateOf(Offset.Zero) }
        var sourceBounds by remember { mutableStateOf<Rect?>(null) }
        var flightOrigin by remember { mutableStateOf<Rect?>(null) }
        var playerDiscBounds by remember { mutableStateOf<Rect?>(null) }
        var activeCaseBounds by remember { mutableStateOf<Rect?>(null) }
        var activeCaseIndex by remember { mutableStateOf<Int?>(null) }
        var pendingCaseBounds by remember { mutableStateOf<Rect?>(null) }
        var exitSide by remember { mutableFloatStateOf(0f) }
        val latestSongId = rememberUpdatedState(song?.id)
        val latestSongs = rememberUpdatedState(songs)
        val latestPlaying = rememberUpdatedState(isPlaying)
        val togglePlay = rememberUpdatedState(onPlayPause)
        val selectSong = rememberUpdatedState(onSelectSong)
        val request = transfer.request
        val displaySong = request?.song ?: song
        val loadingDisc = request != null
        val storedDisc = transfer.storedSongId == displaySong?.id && displaySong != null &&
            request?.direction != CdDiscTransferDirection.INSERT
        val noDisc = storedDisc || displaySong == null
        val insertingDisc = request?.direction == CdDiscTransferDirection.INSERT &&
            transfer.phase != CdDiscTransferPhase.ARRIVED
        val ejectingDisc = request?.direction == CdDiscTransferDirection.RETURN
        val discAngle = remember(displaySong?.id) { mutableFloatStateOf(0f) }
        fun returnDisc() {
            val current = song ?: return
            val index = activeCaseIndex ?: return
            if (!isVisible || songs.none { it.id == current.id } || activeCaseBounds == null) return
            sourceBounds = activeCaseBounds
            if (transfer.beginReturn(index, current) != null && isPlaying) onPlayPause()
        }
        val returnAction = rememberUpdatedState({ returnDisc() })
        val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
        val sideThreshold = with(LocalDensity.current) { 24.dp.toPx() }
        val canReturn = song != null && !loadingDisc && !storedDisc && isVisible && activeCaseBounds != null
        LaunchedEffect(request, isVisible) {
            val item = request ?: return@LaunchedEffect
            try {
                if (item.direction == CdDiscTransferDirection.RETURN) {
                    if (!isVisible) return@LaunchedEffect
                    // Give the carousel time to recenter its destination case.
                    withFrameNanos { }
                    withFrameNanos { }
                    flightOrigin = sourceBounds ?: return@LaunchedEffect
                    flightProgress.snapTo(1f)
                    seatingProgress.snapTo(0f)
                    liftProgress.snapTo(0f)
                    val horizontal = (flightOrigin?.center?.x ?: 0f) - (playerDiscBounds?.center?.x ?: 0f)
                    exitSide = if (abs(horizontal) > sideThreshold) {
                        if (horizontal < 0f) -1f else 1f
                    } else if ((transfer.pendingInsertion?.index ?: item.index) > item.index) -1f else 1f
                    if (!transfer.returnFlight(item)) return@LaunchedEffect
                    if (item.liftBeforeReturn) liftProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                    flightProgress.animateTo(0f, tween(900, easing = CubicBezierEasing(0.22f, 0f, 0.12f, 1f)))
                    if (!transfer.close(item)) return@LaunchedEffect
                    delay(560)
                    val next = transfer.completeReturn(item)
                    if (next != null && latestSongs.value.any { it.id == next.song.id }) {
                        sourceBounds = pendingCaseBounds
                        transfer.begin(next.index, next.song, latestSongId.value)
                    }
                    return@LaunchedEffect
                }
                flightProgress.snapTo(0f)
                seatingProgress.snapTo(0f)
                liftProgress.snapTo(0f)
                if (!isVisible) return@LaunchedEffect
                // Preview selection opens the case without starting audio.
                val measured = withTimeoutOrNull(1500) {
                    snapshotFlow { sourceBounds != null && playerDiscBounds?.width?.let { it > 0f } == true }
                        .first { it }
                }
                if (measured != true) return@LaunchedEffect
                delay(560)
                if (sourceBounds == null) return@LaunchedEffect
                flightOrigin = sourceBounds
                if (!transfer.fly(item)) return@LaunchedEffect
                flightProgress.animateTo(1f, tween(900, easing = CubicBezierEasing(0.22f, 0f, 0.12f, 1f)))
                if (!transfer.seat(item)) return@LaunchedEffect
                // Both discs now share the same bounds and artwork. Hand off
                // while stationary, then let the final seated frame render.
                seatingProgress.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                withFrameNanos { }
                transfer.arrive(item)?.let { selected ->
                    if (item.resumePlayback) {
                        if (!latestPlaying.value) togglePlay.value()
                    } else selectSong.value(selected)
                    // Keep the seated artwork until the audio state catches up.
                    withTimeoutOrNull(1500) {
                        snapshotFlow { latestSongId.value }.first { it == selected.id }
                    }
                }
            } finally {
                transfer.finish(item)
            }
        }
        LaunchedEffect(song?.id, songs, isVisible, isPlaying) {
            transfer.syncPlayer(song?.id, isPlaying)
            transfer.discardRemovedPending(songs)
            val item = transfer.request ?: return@LaunchedEffect
            if (!isVisible || songs.none { it.id == item.song.id } ||
                (item.direction == CdDiscTransferDirection.RETURN && isPlaying) ||
                (song?.id != item.previousSongId &&
                    !(transfer.phase == CdDiscTransferPhase.ARRIVED && song?.id == item.song.id))) {
                transfer.finish(item)
            }
        }
        BoxWithConstraints(modifier.background(CdBackground).onGloballyPositioned { rootOrigin = it.positionInRoot() }) {
            val viewportWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
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
                        Icon(NusaPlayerIcons.More, cdText("Pengaturan", "Settings"))
                    }
                }
                key(displaySong?.id) {
                    CdTimeline(
                        if (loadingDisc || noDisc) idlePosition else positionMsState,
                        if (noDisc) 0L else request?.song?.durationMs ?: durationMs,
                        hasSong = song != null && !loadingDisc && !storedDisc,
                        onSeek = onSeek
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(18.dp))
                    CdJewelCase(
                        modifier = Modifier.size(width = discSize * 1.12f, height = discSize)
                            .then(if (canReturn) Modifier.pointerInput(swipeThreshold) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var distance = 0f
                                    val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                        if (overSlop > 0f) {
                                            distance = overSlop
                                            change.consume()
                                        }
                                    }
                                    if (drag != null && distance > 0f) {
                                        val completed = verticalDrag(drag.id) { change ->
                                            distance += change.positionChange().y
                                            change.consume()
                                        }
                                        if (completed && distance >= swipeThreshold) returnAction.value()
                                    }
                                }
                            }.semantics {
                                customActions = listOf(CustomAccessibilityAction(
                                    cdText("Kembalikan CD ke kotak", "Return CD to case")
                                ) { returnAction.value(); true })
                            } else Modifier),
                        artistName = displaySong?.artist
                    ) { discModifier ->
                        key(displaySong?.id) {
                            CdDisc(
                                song = displaySong,
                                // Page visibility gates gestures, not playback rotation.
                                spinning = isPlaying && !loadingDisc && !storedDisc,
                                visible = !loadingDisc && !storedDisc,
                                angle = discAngle,
                                artwork = artwork,
                                modifier = discModifier.onGloballyPositioned {
                                    playerDiscBounds = it.boundsInRoot().translate(-rootOrigin)
                                }.graphicsLayer {
                                    alpha = if (storedDisc) 0f else if (!loadingDisc ||
                                        transfer.phase == CdDiscTransferPhase.SEATING || transfer.phase == CdDiscTransferPhase.ARRIVED ||
                                        transfer.phase == CdDiscTransferPhase.PREPARING_RETURN) 1f else 0f
                                }
                            )
                        }
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
                                text = when {
                                    noDisc -> "No CD"
                                    insertingDisc -> cdText("Memasukkan CD…", "Inserting CD…")
                                    ejectingDisc -> cdText("Mengeluarkan CD…", "Ejecting CD…")
                                    else -> displaySong?.title.orEmpty()
                                },
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
                                text = when {
                                    noDisc -> cdText("Masukkan CD dari koleksi di bawah", "Insert a CD from your collection")
                                    insertingDisc || ejectingDisc -> ""
                                    else -> displaySong?.artist?.takeIf { it.isNotBlank() }
                                        ?: cdText("Koleksi musik Anda", "Your music collection")
                                },
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
                        Modifier.padding(
                            top = 12.dp,
                            bottom = if (viewportHeight < 680.dp) 16.dp else 20.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            if (viewportHeight < 680.dp) 8.dp else 12.dp
                        )
                    ) {
                        IconToggleButton(checked = shuffleEnabled, onCheckedChange = { onShuffle() }, enabled = song != null) {
                            Icon(NusaPlayerIcons.Shuffle, cdText("Acak", "Shuffle"), Modifier.size(20.dp), tint = if (shuffleEnabled) CdAccent else Color.Gray)
                        }
                        IconButton(onClick = onPrevious, enabled = song != null) {
                            Icon(NusaPlayerIcons.Previous, cdText("Sebelumnya", "Previous"), Modifier.size(26.dp))
                        }
                        FilledIconButton(
                            onClick = {
                                val current = song
                                val bounds = activeCaseBounds
                                val index = activeCaseIndex
                                if (storedDisc && current != null && bounds != null && index != null && isVisible) {
                                    sourceBounds = bounds
                                    transfer.begin(index, current, current.id)
                                } else onPlayPause()
                            }, enabled = !loadingDisc, modifier = Modifier.size(54.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEDEDE7), contentColor = CdBackground)
                        ) {
                            Icon(if (isPlaying && !loadingDisc) NusaPlayerIcons.Pause else NusaPlayerIcons.Play,
                                if (isPlaying && !loadingDisc) cdText("Jeda", "Pause") else cdText("Putar", "Play"), Modifier.size(30.dp))
                        }
                        IconButton(onClick = onNext, enabled = song != null) {
                            Icon(NusaPlayerIcons.Next, cdText("Berikutnya", "Next"), Modifier.size(26.dp))
                        }
                        IconButton(onClick = onRepeat, enabled = song != null) {
                            Icon(if (repeatMode == 1) NusaPlayerIcons.RepeatOne else NusaPlayerIcons.Repeat,
                                when (repeatMode) {
                                    1 -> cdText("Ulangi satu lagu", "Repeat one")
                                    2 -> cdText("Ulangi semua", "Repeat all")
                                    else -> cdText("Ulangi nonaktif", "Repeat off")
                                }, Modifier.size(19.dp), tint = if (repeatMode != 0) CdAccent else Color.Gray)
                        }
                    }
                }
                val carouselCornerRadius = if (roundedCarouselCorners) 48.dp else 0.dp
                val carouselPanelShape = RoundedCornerShape(topStart = carouselCornerRadius, topEnd = carouselCornerRadius)
                Column(
                    Modifier.fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(carouselPanelShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF1B1B1B), Color(0xFF151515))))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), carouselPanelShape)
                        .navigationBarsPadding().padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(28.dp, 3.dp).clip(CircleShape).background(Color(0xFF343434)))
                    Spacer(Modifier.height(8.dp))
                    if (songs.isNotEmpty()) {
                        CdSongCarousel(
                            songs, displaySong?.id, thumbnailArtwork, transfer, { discAngle.floatValue },
                            onDiscBounds = { index, songId, bounds ->
                                val localBounds = bounds.translate(-rootOrigin)
                                if (songId == displaySong?.id && bounds.width > 0f && bounds.height > 0f) {
                                    activeCaseBounds = localBounds
                                    activeCaseIndex = index
                                }
                                if (transfer.pendingInsertion?.song?.id == songId && bounds.width > 0f && bounds.height > 0f) {
                                    pendingCaseBounds = localBounds
                                }
                                if (transfer.request?.song?.id == songId &&
                                    bounds.width > 0f && bounds.height > 0f &&
                                    transfer.phase in listOf(CdDiscTransferPhase.OPENING, CdDiscTransferPhase.PREPARING_RETURN)) {
                                    sourceBounds = localBounds
                                }
                            },
                            onSelectSong = { index, item, bounds ->
                                val returning = transfer.request?.takeIf { it.direction == CdDiscTransferDirection.RETURN }
                                if (returning != null && bounds != null) {
                                    pendingCaseBounds = bounds.translate(-rootOrigin)
                                    transfer.queueInsertion(returning, index, item)
                                } else if ((item.id != song?.id || transfer.storedSongId == item.id) && isVisible && bounds != null &&
                                    bounds.width > 0f && bounds.height > 0f && playerDiscBounds != null) {
                                    val replacingInsertion = transfer.request != null
                                    val current = song
                                    val oldCase = activeCaseBounds
                                    val oldIndex = activeCaseIndex
                                    if (!replacingInsertion && current != null && !storedDisc && oldCase != null && oldIndex != null &&
                                        songs.any { it.id == current.id }) {
                                        sourceBounds = oldCase
                                        pendingCaseBounds = bounds.translate(-rootOrigin)
                                        transfer.beginReturn(oldIndex, current, liftBeforeReturn = true)?.let {
                                            transfer.queueInsertion(it, index, item)
                                        }
                                    } else {
                                        sourceBounds = bounds.translate(-rootOrigin)
                                        transfer.begin(index, item, song?.id)
                                    }
                                    if (!replacingInsertion && isPlaying && song != null) onPlayPause()
                                } else {
                                    transfer.request?.let(transfer::finish)
                                    onSelectSong(item)
                                }
                            }
                        )
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
            // A sibling of the lower panel can travel beyond its rounded clip
            // and fade mask all the way into the measured player spindle.
            val origin = flightOrigin
            val target = playerDiscBounds
            if (request != null && transfer.phase in listOf(CdDiscTransferPhase.FLYING, CdDiscTransferPhase.SEATING, CdDiscTransferPhase.RETURNING) &&
                origin != null && target != null && target.width > 0f) {
                CdDiscSurface(
                    request.song, artwork,
                    Modifier.size(with(LocalDensity.current) { target.width.toDp() }).graphicsLayer {
                        val p = flightProgress.value
                        val lift = if (request.liftBeforeReturn) liftProgress.value else 0f
                        if (request.liftBeforeReturn) {
                            // Swapping ejects the full-size disc horizontally at
                            // player height. Only the explicit swipe returns it
                            // down to the carousel tray.
                            val distanceToEdge = if (exitSide < 0f) target.center.x else viewportWidthPx - target.center.x
                            val exitDistance = distanceToEdge + size.width * 0.55f + 16.dp.toPx()
                            translationX = target.center.x - size.width / 2f + exitSide * exitDistance * (1f - p)
                            translationY = target.center.y - size.height / 2f - 28.dp.toPx() * lift
                            scaleX = 1f + 0.04f * lift
                            scaleY = scaleX
                            rotationX = -10f * lift
                        } else {
                            val center = origin.center + (target.center - origin.center) * p
                            translationX = center.x - size.width / 2f
                            translationY = center.y - size.height / 2f
                            scaleX = (origin.width + (target.width - origin.width) * p) / size.width
                            scaleY = (origin.height + (target.height - origin.height) * p) / size.height
                        }
                        cameraDistance = size.width * 8f
                        alpha = 1f - seatingProgress.value
                    },
                    rotationDegrees = { discAngle.floatValue },
                    shadowElevation = if (transfer.phase == CdDiscTransferPhase.SEATING) 0.dp else 12.dp
                )
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
    angle: MutableFloatState,
    artwork: @Composable (Song?, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the rotating layer independent from the parent composition and
    // reuse a single frame loop across rapid Play / Pause changes. A finite
    // ease-in/out is important: exponential drag never fully stops and can
    // look like tiny irregular movements at the end on 120 Hz screens.
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
internal fun CdDiscSurface(
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
    transfer: CdDiscTransferState,
    discRotation: () -> Float,
    onDiscBounds: (Int, Long, Rect) -> Unit,
    onSelectSong: (Int, Song, Rect?) -> Unit
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
    LaunchedEffect(currentId, songs, transfer.request?.token) {
        if (transfer.request != null) listState.stopScroll()
        // During a swap the old case keeps its real left/right shelf position.
        // Recenter only once its CD is stored and the next case starts opening.
        if (transfer.request?.liftBeforeReturn == true) return@LaunchedEffect
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
    // Reorder only when a different case becomes closest to the centre.
    // The cover in front then overlaps its neighbours in the correct order.
    val centerItemIndex by remember(listState) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) * 0.5f
            layout.visibleItemsInfo.minByOrNull {
                abs(it.offset + it.size * 0.5f - center)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }
    BoxWithConstraints(
        Modifier.widthIn(max = 360.dp).fillMaxWidth().graphicsLayer {
            clip = true
            compositingStrategy = CompositingStrategy.Offscreen
        }.drawWithCache {
            // Fade to the actual panel underneath on every supported Android
            // version. The mask affects the carousel, not the screen canvas.
            val edgeFade = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.16f to Color.Black,
                0.84f to Color.Black,
                1f to Color.Transparent
            )
            onDrawWithContent {
                drawContent()
                drawRect(edgeFade, blendMode = BlendMode.DstIn)
            }
        }
    ) {
        val slotWidth = maxWidth / CD_CAROUSEL_VISIBLE_COUNT
        // Five overlapping cases keep the stack compact, with a full cover
        // in front and progressively smaller covers tucked behind each side.
        val caseWidth = minOf(116.dp, maxWidth * 0.32f)
        val caseHeight = caseWidth * 0.93f
        LazyRow(
            state = listState,
            userScrollEnabled = transfer.request == null,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = (maxWidth - slotWidth) / 2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(caseHeight + 24.dp)
        ) {
            items(count = if (looping) Int.MAX_VALUE else songs.size, key = { it }) { index ->
                val item = songs[index % songs.size]
                val active = item.id == currentId
                var discBounds by remember(index, item.id) { mutableStateOf<Rect?>(null) }
                LaunchedEffect(active, discBounds, transfer.request?.token) {
                    if (active) discBounds?.let { onDiscBounds(index, item.id, it) }
                }
                Box(
                    Modifier.width(slotWidth).heightIn(min = 48.dp).height(caseHeight)
                        .zIndex((3 - abs(index - centerItemIndex)).coerceAtLeast(0).toFloat())
                        .clickable(interactionSource = null, indication = null, role = Role.Button) {
                            onSelectSong(index, item, discBounds)
                        }
                        .semantics {
                            selected = active
                            contentDescription = "${item.title}, ${item.artist}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CdAlbumCase(
                        song = item,
                        artwork = artwork,
                        selected = active && (transfer.storedSongId != item.id ||
                            transfer.request?.direction == CdDiscTransferDirection.INSERT),
                        showDisc = transfer.request?.song?.id == item.id &&
                            (transfer.phase == CdDiscTransferPhase.OPENING ||
                                (transfer.phase == CdDiscTransferPhase.CLOSING && transfer.request?.liftBeforeReturn != true)),
                        discRotation = discRotation,
                        onDiscBounds = { bounds ->
                            discBounds = bounds
                            onDiscBounds(index, item.id, bounds)
                        },
                        modifier = Modifier.requiredSize(width = caseWidth, height = caseHeight).graphicsLayer {
                            val layout = listState.layoutInfo
                            val center = (layout.viewportStartOffset + layout.viewportEndOffset) * 0.5f
                            val visibleItem = layout.visibleItemsInfo.firstOrNull { it.index == index }
                            val distance = if (visibleItem != null && visibleItem.size > 0) {
                                (visibleItem.offset + visibleItem.size * 0.5f - center) / visibleItem.size
                            } else 3f
                            val depth = abs(distance).coerceAtMost(2f)
                            val scale = 1f - 0.12f * depth
                            scaleX = scale
                            scaleY = scale
                            rotationY = -distance.coerceIn(-2f, 2f) * 12f
                            cameraDistance = size.width * 8f
                            alpha = 1f - 0.14f * depth
                        }
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
