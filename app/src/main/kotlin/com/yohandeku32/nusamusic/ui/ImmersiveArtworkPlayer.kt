package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
    BoxWithConstraints(modifier) {
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
        artwork(Modifier.matchParentSize(), heroHeight, if (wide) (1f - controlsFraction + 0.06f) else 1f)

        // Dedicated top composer instead of reflowing the large Now Playing
        // title into smaller credits. Both fades are graphics layers only.
        Column(
            Modifier.align(Alignment.TopStart)
                .padding(start = horizontal, end = horizontal, top = targetHeaderTop)
                .graphicsLayer {
                    alpha = lyricsTransition
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
                            alpha = 1f - lyricsTransition
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
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = artist,
                            fontSize = 16.25f.sp,
                            color = Color.White.copy(alpha = 0.72f),
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 5.dp)
                                .clickable(enabled = hasSong && !lyricsOpen, onClick = onArtist)
                        )
                        // Still measured, but entirely faded out with the
                        // player title when the composer header becomes active.
                        Box(Modifier.align(Alignment.Start).padding(top = 4.dp)) {
                            quality()
                        }
                    }
                    // Keep its width in the layout on both modes to prevent
                    // changes to title wrapping on every Lyrics toggle.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(12.dp))
                        ImmersiveAction(
                            if (isFavorite) NusaPlayerIcons.HeartFilled else NusaPlayerIcons.Heart,
                            immersiveText("Favorit", "Favorite"), onFavorite, hasSong && !lyricsOpen,
                            active = isFavorite, iconSize = 25.dp
                        )
                    }
                }
                }
                Column(Modifier.fillMaxWidth()
                    .onSizeChanged { transportHeightPx = it.height }) {
                progress(Modifier.fillMaxWidth().padding(top = if (wide) 4.dp else if (compact) 10.dp else 14.dp))
                Spacer(Modifier.height(progressToTransportGap))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    ImmersiveAction(NusaPlayerIcons.Shuffle,
                        if (shuffleEnabled) immersiveText("Acak aktif", "Shuffle on") else immersiveText("Acak nonaktif", "Shuffle off"),
                        onShuffle, hasSong, active = shuffleEnabled, iconSize = 26.dp)
                    IconButton(onClick = onPrevious, enabled = hasSong, modifier = Modifier.size(56.dp)) {
                        Icon(NusaPlayerIcons.Previous, immersiveText("Sebelumnya", "Previous"),
                            tint = Color.White.copy(alpha = if (hasSong) 0.94f else 0.3f),
                            modifier = Modifier.size(38.dp))
                    }
                    // Clean, background-free Play/Pause control.
                    // The larger white glyph stays centred without glass,
                    // a filled circle, border or drop shadow.
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(if (compact) 72.dp else 76.dp)
                    ) {
                        Icon(
                            if (isPlaying) NusaPlayerIcons.Pause else NusaPlayerIcons.Play,
                            if (isPlaying) immersiveText("Jeda", "Pause") else immersiveText("Putar", "Play"),
                            tint = Color.White,
                            modifier = Modifier.size(if (compact) 44.dp else 47.dp)
                        )
                    }
                    IconButton(onClick = onNext, enabled = hasSong, modifier = Modifier.size(56.dp)) {
                        Icon(NusaPlayerIcons.Next, immersiveText("Berikutnya", "Next"),
                            tint = Color.White.copy(alpha = if (hasSong) 0.94f else 0.3f),
                            modifier = Modifier.size(38.dp))
                    }
                    ImmersiveAction(if (repeatMode == 1) NusaPlayerIcons.RepeatOne else NusaPlayerIcons.Repeat,
                        when (repeatMode) {
                            1 -> immersiveText("Ulangi satu lagu", "Repeat one")
                            2 -> immersiveText("Ulangi semua", "Repeat all")
                            else -> immersiveText("Ulangi nonaktif", "Repeat off")
                        }, onRepeat, hasSong, active = repeatMode != 0, iconSize = 26.dp)
                }
                // Raise title, seek bar and transport together by 12dp while
                // keeping the bottom artist button and action pill anchored.
                // Extra spacing is inserted ABOVE the pill, not below it.
                Spacer(Modifier.height(transportToPillGap + if (wide) 0.dp else 12.dp))
                // Keep Artist in its own circular glass target. The remaining
                // actions share a compact pill, with 48dp minimum tap targets.
                val actionPillWidth = if (lyricsEnabled) 224.dp else 176.dp
                Row(
                    Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.09f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        ImmersiveAction(NusaPlayerIcons.Artist,
                            immersiveText("Buka halaman artis", "Open artist page"),
                            onArtist, hasSong, iconSize = 23.dp)
                    }
                    Row(
                        Modifier.width(actionPillWidth)
                            .height(56.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.White.copy(alpha = 0.09f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(100.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                    if (lyricsEnabled) ImmersiveAction(NusaPlayerIcons.Lyrics,
                        if (lyricsOpen) immersiveText("Tutup lirik", "Close lyrics") else immersiveText("Buka lirik", "Open lyrics"),
                        onLyrics, hasSong, active = lyricsOpen, iconSize = 23.dp)
                    ImmersiveAction(NusaPlayerIcons.Library,
                        immersiveText("Perpustakaan", "Library"),
                        onLibrary, iconSize = 23.dp)
                    ImmersiveAction(NusaPlayerIcons.Share,
                        immersiveText("Bagikan lagu", "Share song"),
                        onShare, hasSong, iconSize = 23.dp)
                    ImmersiveAction(NusaPlayerIcons.More,
                        immersiveText("Pengaturan", "Settings"),
                        onSettings, iconSize = 23.dp)
                    }
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

    }
}

@Composable
private fun ImmersiveAction(
    icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true,
    active: Boolean = false, iconSize: Dp = 22.dp
) {
    IconButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.size(50.dp).semantics { selected = active }) {
        Icon(icon, label, tint = Color.White.copy(alpha = if (!enabled) 0.26f else if (active) 1f else 0.62f),
            modifier = Modifier.size(iconSize))
    }
}
