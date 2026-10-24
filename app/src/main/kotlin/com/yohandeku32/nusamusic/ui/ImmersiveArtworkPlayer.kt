package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.util.Locale

private fun immersiveText(id: String, en: String) =
    if (Locale.getDefault().language.equals("id", true)) id else en

/** Artwork-first presentation of the existing callbacks; owns no playback or library state. */
@Composable
internal fun ImmersiveArtworkPlayer(
    title: String,
    artist: String,
    composerNames: String,
    titleFontFamily: FontFamily,
    titleFontSize: Float,
    hasSong: Boolean,
    isPlaying: Boolean,
    lightMode: Boolean = false,
    isFavorite: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    lyricsOpen: Boolean,
    lyricsEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onLyrics: () -> Unit,
    onArtist: () -> Unit,
    artistOpen: Boolean = false,
    canCloseArtistBySwipe: () -> Boolean = { true },
    onArtistClose: () -> Unit = {},
    artistProfile: @Composable () -> Unit = {},
    libraryOpen: Boolean = false,
    libraryLightMode: Boolean = false,
    liquidGlassEnabled: Boolean = true,
    glassArtworkColor: Color = Color(0xFF29303B),
    onLibraryClose: () -> Unit = {},
    onHome: () -> Unit = {},
    libraryContent: @Composable (Boolean) -> Unit = {},
    onLibrary: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    artwork: @Composable (Modifier, Dp, Float) -> Unit,
    progress: @Composable (Modifier) -> Unit,
    quality: @Composable () -> Unit,
    lyrics: @Composable (Modifier) -> Unit = {},
    modifier: Modifier = Modifier,
    contentInsets: WindowInsets = WindowInsets.safeDrawing
) {
    var playerBounds by remember { mutableStateOf<Rect?>(null) }
    var navigationPillBounds by remember { mutableStateOf<Rect?>(null) }
    var artistButtonBounds by remember { mutableStateOf<Rect?>(null) }
    // One predictable, non-overshooting timeline moves BOTH the circle and
    // the four-action pill. Read Animatable only in draw/placement modifiers;
    // do not recompose the expensive artwork/player on every animation frame.
    val artistLift = remember { Animatable(0f) }
    // Animate the navigation bar independently and read its value only in
    // draw/placement/measure blocks: no per-frame player recomposition.
    val pillExpansion = remember { Animatable(0f) }
    // Both content sources are recorded beneath the same small glass
    // controls: current-player artwork and the currently visible portal.
    val glassBackdrop = remember { HazeState() }
    // One liquid material is shared by Home, Artist and Library. Keep live
    // Haze on the idle Now Playing screen as well, but never re-record a
    // moving full-screen source while entering or leaving a portal.
    var portalSettled by remember { mutableStateOf(false) }
    val dragScope = rememberCoroutineScope()
    var dragActive by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(1f) }
    val shownProgress: () -> Float = {
        if (dragActive) dragFraction else artistLift.value
    }
    var portalMounted by remember { mutableStateOf(false) }
    var lastOverlayIsLibrary by remember { mutableStateOf(false) }
    val overlayOpen = artistOpen || libraryOpen
    // Make the first overlay frame part of the tap recomposition, instead
    // of waiting for LaunchedEffect to mount the portal a frame later.
    val portalVisible = overlayOpen || portalMounted
    val presentingLibrary = if (overlayOpen) libraryOpen else lastOverlayIsLibrary
    // In Library the two lower navigation controls are fully black and
    // the Artist portal bubble is absent; do not capture that full-screen
    // list as a Haze source. Artist's elevated bubble keeps its glass.
    val liveGlassReady = liquidGlassEnabled &&
        (!portalVisible || (portalSettled && !presentingLibrary))
    val liveBackdrop = if (liveGlassReady) glassBackdrop else null
    // Crossfade ONLY the real blur layer. The same refractive satin surface
    // remains underneath on Home and both portals at every animation frame.
    val liveGlassAlpha by animateFloatAsState(
        targetValue = if (liveGlassReady) 1f else 0f,
        animationSpec = tween(125, easing = FastOutSlowInEasing),
        label = "continuousLiquidGlassBackdrop"
    )
    LaunchedEffect(artistOpen, libraryOpen) {
        if (overlayOpen) {
            dragActive = false
            lastOverlayIsLibrary = libraryOpen
            portalMounted = true
            portalSettled = false
            launch {
                pillExpansion.animateTo(1f, tween(190, easing = FastOutSlowInEasing))
            }
            // Start the animation immediately. Previously awaiting the next
            // frame added noticeable latency on Library taps; its expensive
            // metadata indexing is still deferred until the sheet settles.
            artistLift.animateTo(
                1f,
                animationSpec = tween(
                    if (libraryOpen) 300 else 350,
                    easing = FastOutSlowInEasing
                )
            )
            portalSettled = true
        } else {
            portalSettled = false
            launch {
                pillExpansion.animateTo(0f, tween(165, easing = FastOutSlowInEasing))
            }
            // Continue closing from the actual finger position, not 100%.
            if (dragActive) {
                artistLift.snapTo(dragFraction)
                dragActive = false
            }
            // Touch-down dismiss should not leave a 600ms tail after the
            // sheet has already followed the finger most of the way.
            val remainingMs = (225f * artistLift.value)
                .toInt().coerceIn(75, 225)
            artistLift.animateTo(
                0f, animationSpec = tween(remainingMs, easing = FastOutSlowInEasing)
            )
            portalMounted = false
        }
    }
    BoxWithConstraints(modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        if (playerBounds != bounds) playerBounds = bounds
    }) {
        val primaryInk = if (lightMode) Color(0xFF171A20) else Color.White
        // A restrained album-responsive ink: preserve Apple Music-style high
        // contrast rather than painting transport controls in dark cover colors.
        val transportInk by animateColorAsState(
            targetValue = if (lightMode) primaryInk
                else lerp(Color.White, glassArtworkColor.copy(alpha = 1f), 0.20f),
            animationSpec = tween(520, easing = FastOutSlowInEasing),
            label = "albumResponsiveTransportInk"
        )
        val mutedInk = primaryInk.copy(alpha = 0.72f)
        val density = LocalDensity.current
        val viewportHeight = maxHeight
        val layoutDirection = LocalLayoutDirection.current
        val wide = maxWidth >= 600.dp && maxWidth > maxHeight * 1.15f
        val insetWidth = with(density) {
            (contentInsets.getLeft(density, layoutDirection) + contentInsets.getRight(density, layoutDirection)).toDp()
        }
        val controlsWidth = (288.dp + insetWidth).coerceIn(maxWidth * 0.46f, maxWidth * 0.58f)
        val controlsFraction = controlsWidth / maxWidth
        val compact = maxHeight < 720.dp || maxWidth < 370.dp
        // More breathing room at both edges without changing icon scale.
        val horizontal = if (wide) 20.dp else if (compact) 24.dp else 28.dp
        // Lift the entire Now Playing stack, INCLUDING the lower action pill.
        // Padding changes only when viewport size class changes, not per frame.
        val playerBottomPadding = if (wide) 12.dp else 22.dp
        // Shift the transport row upward toward the progress bar while
        // keeping the overall vertical spacing and bottom pill position.
        val transportGap = when {
            maxHeight < 640.dp -> 14.dp
            wide -> 16.dp
            compact -> 18.dp
            else -> 20.dp
        }
        val progressToTransportGap = (transportGap - 8.dp).coerceAtLeast(8.dp)
        val transportToPillGap = transportGap * 2 - progressToTransportGap
        var controlsHeightPx by remember { mutableIntStateOf(0) }
        val controlsHeight = with(density) { controlsHeightPx.toDp() }
        var transportHeightPx by remember { mutableIntStateOf(0) }
        val transportHeight = with(density) { transportHeightPx.toDp() }
        val topInset = with(density) { contentInsets.getTop(density).toDp() }
        val bottomInset = with(density) { contentInsets.getBottom(density).toDp() }
        // Keep the entire player stack measured exactly once per song/font.
        // Switching Lyrics changes only compositor-layer opacity/translation:
        // no header height collapse, transport jump, hero relayout or feedback
        // from onSizeChanged during the entrance animation.
        val targetHeaderTop = topInset + if (compact) 16.dp else 20.dp
        val lyricsTransition by animateFloatAsState(
            targetValue = if (lyricsOpen) 1f else 0f,
            animationSpec = tween(250, easing = FastOutSlowInEasing),
            label = "immersiveLyricsCrossfade"
        )
        // Defer expensive lazy lyric measurement until after the artwork
        // fade has largely settled, rather than stacking GPU composition,
        // remeasure and the initial automatic lyric scroll on one frame.
        // Cancelling the effect handles rapid repeated taps safely.
        var showLyricsContent by remember { mutableStateOf(false) }
        LaunchedEffect(lyricsOpen) {
            if (lyricsOpen) {
                delay(220L)
                showLyricsContent = true
            } else {
                showLyricsContent = false
            }
        }
        // Composer has independent, compact top geometry. The normal title
        // remains in its original layout slot (invisible in Lyrics mode), so
        // the seek bar, five transport buttons and lower pill never shift.
        val lyricsViewportTop = if (wide) targetHeaderTop + 48.dp
            else targetHeaderTop + 48.dp
        val lyricsBottomReserve = if (wide) bottomInset + 20.dp
            else transportHeight + bottomInset + playerBottomPadding + 12.dp
        val lyricsViewportHeight = (viewportHeight - lyricsViewportTop - lyricsBottomReserve)
            .coerceAtLeast(0.dp)
        // Keep the artwork balanced as the foreground adapts to the
        // compact, evenly spaced transport controls.
        val addedTransportGap = (transportGap -
            (if (wide) 10.dp else if (compact) 14.dp else 16.dp)).coerceAtLeast(0.dp)
        val requestedHero = if (wide) maxHeight else
            (maxHeight - (if (controlsHeightPx > 0) controlsHeight else 320.dp) +
                64.dp + addedTransportGap)
                .coerceIn(maxHeight * 0.45f, maxHeight * 0.80f)
        val heroHeight by animateDpAsState(requestedHero, tween(220), label = "immersiveHeroHeight")
        artwork(
            Modifier.matchParentSize().then(
                if (liveGlassReady) Modifier.hazeSource(glassBackdrop) else Modifier
            ),
            heroHeight, if (wide) (1f - controlsFraction + 0.06f) else 1f
        )
        // Rightward drags on the artwork open Artist; leave vertical scroll,
        // seeking, and transport-button gestures to their own controls.
        if (hasSong && !overlayOpen && !lyricsOpen) {
            Box(Modifier.matchParentSize().pointerInput(onArtist) {
                var distance = 0f
                val thresholdPx = 72.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { distance = 0f },
                    onHorizontalDrag = { change, amount ->
                        distance += amount
                        change.consume()
                    },
                    onDragEnd = {
                        if (distance >= thresholdPx) onArtist()
                    },
                    onDragCancel = { distance = 0f }
                )
            })
        }
        if (portalVisible) {
            // Opaque full-screen black underlay, including BELOW the profile.
            // No blurred cover or colored artwork can leak around the pill.
            // The actual floating sheet is rendered over the playback controls.
            Box(Modifier.matchParentSize()
                .graphicsLayer { alpha = shownProgress() }
                .background(
                    if (presentingLibrary) immersiveColors(libraryLightMode).background
                    else Color.Black
                ))
        }

        // Dedicated top composer instead of reflowing the large Now Playing
        // title into smaller credits. Both fades are graphics layers only.
        Column(
            Modifier.align(Alignment.TopStart)
                .padding(start = horizontal, end = horizontal, top = targetHeaderTop)
                .graphicsLayer {
                    alpha = lyricsTransition * (1f - shownProgress())
                    translationY = (1f - lyricsTransition) * 8.dp.toPx()
                }
        ) {
            Text(
                text = "COMPOSER",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.58f)
            )
            Text(
                text = composerNames.ifBlank { "Unknown composer" },
                fontSize = if (compact) 11.sp else 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Render lyrics above the artwork-derived ambient but BELOW the
        // foreground controls. A delayed, gentle fade lets the album metadata
        // start its smooth trip upward before lines come into focus.
        if (lyricsEnabled && lyricsViewportHeight > 0.dp) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showLyricsContent && lyricsViewportHeight > 0.dp,
                modifier = Modifier.align(if (wide) Alignment.TopStart else Alignment.TopCenter)
                    .fillMaxWidth(if (wide) (1f - controlsFraction).coerceIn(0.42f, 0.75f) else 1f)
                    // Slightly more breathing room than the previous
                    // revision: 20dp outer + 16dp safe glyph inset = 36dp.
                    // The glyph padding itself remains untouched for glow.
                    .padding(start = 20.dp, end = 16.dp)
                    .padding(top = lyricsViewportTop)
                    .height(lyricsViewportHeight),
                // Only composite the existing lyric canvas: do not animate
                // its bounds or seek target while the viewport is created.
                enter = fadeIn(animationSpec = tween(240)),
                exit = fadeOut(animationSpec = tween(160))
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Reuse the existing synced lyrics, timing and follow-scroll.
                    lyrics(Modifier.fillMaxSize())
                }
            }
        }

        // The three-action navigation pill stays centered across transitions.
        val actionPillWidth = if (lyricsEnabled) 176.dp else 128.dp

        // On compact/large-text screens the foreground can scroll rather than
        // clipping transport buttons. The artwork remains a full-screen canvas.
        val foreground: @Composable () -> Unit = {
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { controlsHeightPx = it.height }
                    .padding(horizontal = horizontal)
                    .windowInsetsPadding(contentInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(bottom = playerBottomPadding)
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .graphicsLayer {
                            alpha = (1f - lyricsTransition) * (1f - shownProgress())
                            translationY = -8.dp.toPx() * lyricsTransition
                        }
                ) {
                // Stable typography and measured height in both modes.
                // Only the parent graphics layer fades this stack.
                val expandedTitleSize = (titleFontSize *
                    if (compact) 0.78f else 0.84f).coerceIn(24f, 36f)
                val expandedLineHeight = (titleFontSize *
                    if (compact) 0.90f else 0.97f).coerceIn(29f, 43f)

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = title,
                            fontFamily = titleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = expandedTitleSize.sp,
                            lineHeight = expandedLineHeight.sp,
                            textAlign = TextAlign.Start,
                            color = primaryInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = artist,
                            fontSize = 16.25f.sp,
                            color = mutedInk,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            // Artist opens with a right swipe on the artwork.
                            // Keep this text free of an extra hidden tap target.
                            modifier = Modifier.padding(top = 5.dp)
                        )
                        // Still measured, but entirely faded out with the
                        // player title when the composer header becomes active.
                        // Reserve the quality-label row from the very first
                        // frame. Native FLAC/ALAC inspection may finish later;
                        // the label fades into this slot instead of moving the
                        // progress bar and transport controls vertically.
                        Box(
                            Modifier.align(Alignment.Start)
                                .padding(top = 4.dp)
                                .height(25.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            quality()
                        }
                    }
                    // Keep its width in the layout on both modes to prevent
                    // changes to title wrapping on every Lyrics toggle.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(12.dp))
                        ImmersiveAction(lightMode, 
                            if (isFavorite) NusaPlayerIcons.HeartFilled else NusaPlayerIcons.Heart,
                            immersiveText("Favorit", "Favorite"), onFavorite, hasSong && !lyricsOpen,
                            active = isFavorite, iconSize = 25.dp
                        )
                    }
                }
                }
                Column(Modifier.fillMaxWidth()
                    .graphicsLayer { alpha = 1f - shownProgress() }
                    .onSizeChanged { transportHeightPx = it.height }) {
                progress(Modifier.fillMaxWidth().padding(top = if (wide) 4.dp else if (compact) 10.dp else 14.dp))
                Spacer(Modifier.height(progressToTransportGap))
                // Apple Music-inspired hierarchy: filled transport glyphs,
                // lighter outline modes and generous, stable touch targets.
                // Compact sizing avoids horizontal clipping on narrow devices.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    ImmersiveAction(
                        lightMode, Lucide.Shuffle,
                        if (shuffleEnabled) immersiveText("Acak aktif", "Shuffle on") else immersiveText("Acak nonaktif", "Shuffle off"),
                        onShuffle, hasSong, active = shuffleEnabled, iconSize = 25.dp,
                        iconTint = transportInk, touchSize = if (compact) 48.dp else 50.dp
                    )
                    TransportGlyphButton(
                        icon = NusaPlayerIcons.Previous,
                        label = immersiveText("Sebelumnya", "Previous"),
                        onClick = onPrevious, enabled = hasSong, tint = transportInk,
                        touchSize = if (compact) 50.dp else 54.dp, iconSize = if (compact) 34.dp else 36.dp
                    )
                    // Only the central glyph is prominent; the button has no
                    // circular plate, shadow or new backdrop blur.
                    TransportGlyphButton(
                        icon = if (isPlaying) NusaPlayerIcons.Pause else NusaPlayerIcons.Play,
                        label = if (isPlaying) immersiveText("Jeda", "Pause") else immersiveText("Putar", "Play"),
                        onClick = onPlayPause, enabled = true, tint = transportInk,
                        touchSize = if (compact) 68.dp else 74.dp,
                        iconSize = if (compact) 46.dp else 50.dp,
                        crossfadeGlyph = true
                    )
                    TransportGlyphButton(
                        icon = NusaPlayerIcons.Next,
                        label = immersiveText("Berikutnya", "Next"),
                        onClick = onNext, enabled = hasSong, tint = transportInk,
                        touchSize = if (compact) 50.dp else 54.dp, iconSize = if (compact) 34.dp else 36.dp
                    )
                    ImmersiveAction(
                        lightMode, if (repeatMode == 1) Lucide.Repeat1 else Lucide.Repeat,
                        when (repeatMode) {
                            1 -> immersiveText("Ulangi satu lagu", "Repeat one")
                            2 -> immersiveText("Ulangi semua", "Repeat all")
                            else -> immersiveText("Ulangi nonaktif", "Repeat off")
                        }, onRepeat, hasSong, active = repeatMode != 0,
                        iconSize = 25.dp, iconTint = transportInk,
                        touchSize = if (compact) 48.dp else 50.dp
                    )
                }
                // Restore the original separate Artist profile circle beside
                // the Lyrics/Library/Settings pill. Both are 56dp high and
                // share the same measured row during transitions.
                Spacer(Modifier.height(transportToPillGap + if (wide) 0.dp else 12.dp))
                Row(
                    Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(56.dp).onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            if (artistButtonBounds != bounds) artistButtonBounds = bounds
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.matchParentSize().graphicsLayer {
                                alpha = (1f - shownProgress()).coerceIn(0f, 1f)
                                // The pill expands 48dp. Shift both pieces
                                // 24dp left to keep their combined group
                                // visually centered throughout Library reveal.
                                translationX = if (presentingLibrary) {
                                    navigationProfileLeftPx(
                                        0f, 48.dp.toPx(), pillExpansion.value
                                    )
                                } else 0f
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            ContinuousLiquidGlassSurface(
                                radius = 50.dp,
                                lightMode = lightMode,
                                liquidGlassEnabled = liquidGlassEnabled,
                                hazeState = liveBackdrop,
                                hazeAlpha = liveGlassAlpha,
                                artworkTint = glassArtworkColor,
                                modifier = Modifier.matchParentSize()
                            )
                            ImmersiveAction(
                                lightMode, Lucide.UserRound,
                                immersiveText("Buka halaman artis", "Open artist page"),
                                onArtist, hasSong && !artistOpen && !libraryOpen,
                                iconSize = 23.dp,
                                iconTint = if (liquidGlassEnabled) Color.White else null,
                                liquidMotion = liquidGlassEnabled,
                                inactiveAlpha = 1f
                            )
                        }
                    }
                    Spacer(
                        Modifier.width(actionPillWidth).height(56.dp)
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInRoot()
                                if (navigationPillBounds != bounds) navigationPillBounds = bounds
                            }
                    )
                }
                }
            }
        }

        if (wide) {
            Box(Modifier.align(Alignment.CenterEnd).width(controlsWidth).fillMaxHeight()
                .padding(top = 68.dp).verticalScroll(rememberScrollState())) {
                foreground()
            }
        } else if (maxHeight < 560.dp || density.fontScale > 1.35f) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(maxOf(180.dp, viewportHeight * 0.35f)))
                foreground()
            }
        } else {
            Box(Modifier.align(Alignment.BottomCenter)) { foreground() }
        }

        if (portalVisible) ImmersiveArtistPortal(
            open = overlayOpen,
            progress = shownProgress,
            source = artistButtonBounds ?: navigationPillBounds,
            host = playerBounds,
            topInsetPx = with(density) { topInset.toPx() },
            onClose = if (presentingLibrary) onLibraryClose else onArtistClose,
            onDragFraction = { fraction ->
                // No suspend call or coroutine per touch movement.
                dragFraction = fraction
                dragActive = true
            },
            onSwipeCancelled = {
                val releasedAt = dragFraction
                dragScope.launch {
                    artistLift.snapTo(releasedAt)
                    dragActive = false
                    artistLift.animateTo(
                        1f, animationSpec = tween(170, easing = FastOutSlowInEasing)
                    )
                }
            },
            canSwipeDown = {
                // Do not interrupt the still-opening Animatable with a
                // drag snapTo; allow gestures once the sheet has settled.
                portalSettled &&
                    (presentingLibrary || canCloseArtistBySwipe())
            },
            swipeFromHeaderOnly = presentingLibrary,
            hazeState = liveBackdrop,
            profile = {
                if (presentingLibrary) libraryContent(portalSettled)
                else artistProfile()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Library keeps the separate Artist circle visible above its sheet.
        // Now Playing still supports the artwork's right-swipe gesture.
        val pill = navigationPillBounds
        val root = playerBounds
        val bubble = artistButtonBounds
        if (presentingLibrary && portalVisible && bubble != null && root != null) {
            Box(
                Modifier.offset {
                    IntOffset(
                        navigationProfileLeftPx(
                            bubble.left - root.left,
                            48.dp.toPx(),
                            pillExpansion.value
                        ).roundToInt(),
                        (bubble.top - root.top).roundToInt()
                    )
                }.size(56.dp).graphicsLayer {
                    alpha = shownProgress().coerceIn(0f, 1f)
                },
                contentAlignment = Alignment.Center
            ) {
                ContinuousLiquidGlassSurface(
                    radius = 50.dp,
                    lightMode = lightMode,
                    liquidGlassEnabled = liquidGlassEnabled,
                    hazeState = liveBackdrop,
                    hazeAlpha = liveGlassAlpha,
                    artworkTint = glassArtworkColor,
                    modifier = Modifier.matchParentSize()
                )
                SolidNavigationOverlay(
                    radius = 50.dp,
                    progress = shownProgress,
                    modifier = Modifier.matchParentSize()
                )
                ImmersiveAction(
                    lightMode, Lucide.UserRound,
                    immersiveText("Buka profil artis", "Open artist profile"),
                    onArtist, hasSong && libraryOpen,
                    iconSize = 23.dp, iconTint = Color.White,
                    liquidMotion = liquidGlassEnabled,
                    inactiveAlpha = 1f
                )
            }
        }
        if (pill != null && root != null) {
            ImmersiveOverlayActionPill(
                anchorX = pill.left - root.left,
                anchorY = pill.top - root.top,
                progress = shownProgress,
                fullWidth = actionPillWidth,
                widthExpansion = { pillExpansion.value },
                lyricsEnabled = lyricsEnabled,
                lyricsOpen = lyricsOpen,
                lightMode = lightMode,
                libraryOpen = libraryOpen,
                presentingLibrary = presentingLibrary && portalVisible,
                overlayOpen = overlayOpen,
                showSolidPill = portalVisible,
                hazeState = liveBackdrop,
                liquidGlassEnabled = liquidGlassEnabled,
                hazeAlpha = liveGlassAlpha,
                artworkTint = glassArtworkColor,
                hasSong = hasSong,
                onLyrics = onLyrics,
                onLibrary = onLibrary,
                onHome = onHome,
                onSettings = onSettings
            )
        }
    }
}

/**
 * One measured navigation row on every screen. The backdrop layers fade
 * smoothly between the Now Playing capsule and Haze's live frosted glass
 * while the portal settles. This avoids abrupt style swaps on Home.
 */
/**
 * Solid only at full portal reveal; transparency is draw-layer only,
 * preserving the compositional layout and Home's source-backed glass.
 */
@Composable
private fun SolidNavigationOverlay(
    radius: Dp,
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.graphicsLayer { alpha = solidNavigationReveal(progress()) }
            .clip(RoundedCornerShape(radius))
            .background(Color.Black)
    )
}

@Composable
private fun ImmersiveOverlayActionPill(
    anchorX: Float,
    anchorY: Float,
    progress: () -> Float,
    fullWidth: Dp,
    widthExpansion: () -> Float,
    lyricsEnabled: Boolean,
    lyricsOpen: Boolean,
    lightMode: Boolean,
    libraryOpen: Boolean,
    presentingLibrary: Boolean,
    overlayOpen: Boolean,
    showSolidPill: Boolean,
    hazeState: HazeState?,
    liquidGlassEnabled: Boolean,
    hazeAlpha: Float,
    artworkTint: Color,
    hasSong: Boolean,
    onLyrics: () -> Unit,
    onLibrary: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit
) {
    val density = LocalDensity.current
    val iconColor by animateColorAsState(
        targetValue = if (liquidGlassEnabled || overlayOpen) Color.White
            else if (lightMode) Color(0xFF171A20) else Color.White,
        animationSpec = tween(390, easing = FastOutSlowInEasing),
        label = "frostedPillIconColor"
    )
    Box(
        Modifier.offset {
            // Keep navigation visually centered on BOTH overlays:
            // - Library shows Artist + 10dp gap + widening pill as one group.
            // - Artist hides its profile circle, so the pill itself centers.
            // Distribute the 48dp pill growth equally left and right.
            val left = navigationPillLeftPx(
                originalLeft = anchorX,
                expansionPx = 48.dp.toPx(),
                profileWithGapPx = 66.dp.toPx(),
                reveal = widthExpansion(),
                artistOnly = showSolidPill && !presentingLibrary
            )
            IntOffset(left.roundToInt(), anchorY.roundToInt())
        }.layout { measurable, constraints ->
            // Measure only the tiny navigation bar, not the full player.
            // Reading widthExpansion here avoids recomposing the artwork,
            // transport and entire portal once per animation frame.
            val expandedPx = (48.dp.toPx() *
                widthExpansion().coerceIn(0f, 1f)).roundToInt()
            val targetWidth = (fullWidth.roundToPx() + expandedPx).coerceIn(
                constraints.minWidth, constraints.maxWidth
            )
            val targetHeight = 56.dp.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
            val placeable = measurable.measure(Constraints.fixed(targetWidth, targetHeight))
            layout(targetWidth, targetHeight) { placeable.placeRelative(0, 0) }
        }
    ) {
        // The original Home glass remains mounted as black fades out,
        // preventing the last-frame material swap on return.
        ContinuousLiquidGlassSurface(
            radius = 100.dp,
            lightMode = lightMode,
            liquidGlassEnabled = liquidGlassEnabled,
            // Never render live blur underneath an opaque portal pill.
            hazeState = if (showSolidPill) null else hazeState,
            hazeAlpha = hazeAlpha,
            artworkTint = artworkTint,
            modifier = Modifier.matchParentSize()
        )
        if (showSolidPill) {
            SolidNavigationOverlay(
                radius = 100.dp,
                progress = progress,
                modifier = Modifier.matchParentSize()
            )
        }
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (lyricsEnabled) ImmersiveAction(
                lightMode, Lucide.MessageSquareText,
                if (lyricsOpen) immersiveText("Tutup lirik", "Close lyrics")
                else immersiveText("Buka lirik", "Open lyrics"),
                onLyrics, hasSong, active = lyricsOpen, iconSize = 23.dp,
                iconTint = iconColor, liquidMotion = liquidGlassEnabled
            )
            // A single stable 50dp slot morphs Library into Home. Keeping
            // the same measured slot prevents the entire navigation row from
            // redistributing its icons in the middle of a page transition.
            Crossfade(
                targetState = libraryOpen,
                animationSpec = tween(95),
                label = "navigationLibraryHomeGlyph"
            ) { showingLibraryHome ->
                ImmersiveAction(
                    lightMode,
                    if (showingLibraryHome)
                        Lucide.House
                    else Lucide.ListMusic,
                    if (showingLibraryHome)
                        immersiveText("Kembali ke pemutar", "Back to Now Playing")
                    else immersiveText("Perpustakaan", "Library"),
                    if (showingLibraryHome) onHome else onLibrary,
                    enabled = showingLibraryHome == libraryOpen,
                    iconSize = 23.dp, iconTint = iconColor,
                    liquidMotion = liquidGlassEnabled
                )
            }
            ImmersiveAction(
                lightMode, Lucide.Settings2,
                immersiveText("Pengaturan", "Settings"),
                onSettings, iconSize = 23.dp, iconTint = iconColor,
                liquidMotion = liquidGlassEnabled
            )
            // Artist alone needs the additional Home action. Library
            // already uses the first, fixed-width slot for its Home button.
            AnimatedVisibility(
                visible = overlayOpen && !libraryOpen,
                enter = fadeIn(tween(110)),
                exit = fadeOut(tween(90))
            ) {
                ImmersiveAction(
                    lightMode, Lucide.House,
                    immersiveText("Kembali ke pemutar", "Back to Now Playing"),
                    onHome, iconSize = 23.dp, iconTint = iconColor,
                    liquidMotion = liquidGlassEnabled
                )
            }
        }
    }
}


/**
 * Filled SF-inspired transport silhouettes built into NusaMusic. This keeps
 * the design familiar on Android without bundling Apple's licensed SF assets.
 * Press animations transform only the icon layer, never the row or touch area.
 */
@Composable
private fun TransportGlyphButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    tint: Color,
    touchSize: Dp,
    iconSize: Dp,
    crossfadeGlyph: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.91f else 1f,
        animationSpec = spring(dampingRatio = 0.87f, stiffness = 760f),
        label = "transportGlyphPress"
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.size(touchSize)
    ) {
        val glyph: @Composable (ImageVector) -> Unit = { vector ->
            Icon(
                imageVector = vector,
                contentDescription = label,
                tint = tint.copy(alpha = if (enabled) 1f else 0.30f),
                modifier = Modifier.size(iconSize).graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
            )
        }
        if (crossfadeGlyph) {
            Crossfade(targetState = icon, animationSpec = tween(135),
                label = "immersivePlayPauseGlyph") { vector -> glyph(vector) }
        } else {
            glyph(icon)
        }
    }
}


@Composable
private fun ImmersiveAction(
    lightMode: Boolean,
    icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true,
    active: Boolean = false, iconSize: Dp = 22.dp,
    iconTint: Color? = null,
    liquidMotion: Boolean = false,
    touchSize: Dp = 50.dp,
    inactiveAlpha: Float = 0.68f
) {
    // iOS-inspired liquid press response is a restrained native Compose
    // spring, not a screen-wide transform or heavy shader animation.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val touchScale by animateFloatAsState(
        targetValue = if (liquidMotion && pressed) 0.945f else 1f,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = 680f
        ),
        label = "liquidNavigationPress"
    )
    val pressLight by animateFloatAsState(
        targetValue = if (liquidMotion && pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.87f, stiffness = 750f),
        label = "liquidPressReflection"
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.size(touchSize)
            .graphicsLayer {
                scaleX = touchScale
                scaleY = touchScale
            }
            // A tiny caustic glint blooms under the finger on touch, then
            // recedes. Unlike a ripple it stays within the round tap target.
            .drawWithCache {
                val highlight = Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f * pressLight),
                        Color.White.copy(alpha = 0.045f * pressLight),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.34f, size.height * 0.26f),
                    radius = size.width * 0.63f
                )
                onDrawWithContent {
                    drawContent()
                    if (liquidMotion && pressLight > 0.001f) {
                        drawCircle(
                            brush = highlight,
                            radius = size.width * 0.49f,
                            center = center
                        )
                    }
                }
            }
            .semantics { selected = active }
    ) {
        Icon(icon, label, tint = (iconTint ?: if (lightMode) Color(0xFF171A20) else Color.White)
            .copy(alpha = if (!enabled) 0.26f else if (active) 1f else inactiveAlpha),
            modifier = Modifier.size(iconSize))
    }
}
