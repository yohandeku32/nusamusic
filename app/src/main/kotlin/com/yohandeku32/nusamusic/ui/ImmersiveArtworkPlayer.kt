package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private fun immersiveText(id: String, en: String) =
    if (Locale.getDefault().language.equals("id", true)) id else en

/** Artwork-first presentation of the existing callbacks; owns no playback or library state. */
@Composable
internal fun ImmersiveArtworkPlayer(
    title: String,
    artist: String,
    titleFontFamily: FontFamily,
    titleFontSize: Float,
    hasSong: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    ambientSong: com.yohandeku32.nusamusic.model.Song?,
    lyricsOpen: Boolean,
    lyricsEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onLyrics: () -> Unit,
    onDismissLyrics: () -> Unit,
    onArtist: () -> Unit,
    onLibrary: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    artwork: @Composable (Modifier, Dp, Float) -> Unit,
    progress: @Composable (Modifier) -> Unit,
    quality: @Composable () -> Unit,
    miniArtwork: @Composable (Modifier) -> Unit = {},
    lyrics: @Composable (Modifier) -> Unit = {},
    modifier: Modifier = Modifier,
    contentInsets: WindowInsets = WindowInsets.safeDrawing
) {
    var playerRootTopPx by remember { mutableIntStateOf(0) }
    BoxWithConstraints(modifier.onGloballyPositioned {
        val top = it.positionInRoot().y.roundToInt()
        if (playerRootTopPx != top) playerRootTopPx = top
    }) {
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
        val horizontal = if (wide) 16.dp else if (compact) 24.dp else 28.dp
        // Equal gaps above and below transport keep its icons centred
        // between the progress bar and the bottom action pill.
        // Shorter viewports preserve the same balance with smaller gaps.
        val transportGap = when {
            maxHeight < 640.dp -> 14.dp
            wide -> 16.dp
            compact -> 18.dp
            else -> 20.dp
        }
        var controlsHeightPx by remember { mutableIntStateOf(0) }
        val controlsHeight = with(density) { controlsHeightPx.toDp() }
        var lyricHeaderHeightPx by remember { mutableIntStateOf(0) }
        var transportHeightPx by remember { mutableIntStateOf(0) }
        val headerHeight = with(density) { lyricHeaderHeightPx.toDp() }
        val transportHeight = with(density) { transportHeightPx.toDp() }
        val topInset = with(density) { contentInsets.getTop(density).toDp() }
        val bottomInset = with(density) { contentInsets.getBottom(density).toDp() }
        val targetHeaderTop = topInset + if (compact) 56.dp else 70.dp
        // Move the existing metadata (title, artist, Hi-Res) into the former
        // artwork area. This is paint-only translation: its original height
        // still reserves room for progress and transport at the bottom.
        val headerLift = if (wide || controlsHeightPx == 0) 0.dp else
            (viewportHeight - controlsHeight - targetHeaderTop).coerceAtLeast(0.dp)
        val lyricsTransition by animateFloatAsState(
            targetValue = if (lyricsOpen) 1f else 0f,
            animationSpec = tween(460, easing = FastOutSlowInEasing),
            label = "immersiveLyricsReflow"
        )
        var settingsRowBottomPx by remember { mutableIntStateOf(0) }
        // The anchor comes from the actual on-screen Settings row, including
        // its system-bar insets. It is not a hard-coded screen percentage.
        val viewportHeightPx = with(density) { viewportHeight.roundToPx() }
        val actualSettingsBottomPx = if (settingsRowBottomPx > 0) settingsRowBottomPx
            else with(density) { (topInset + 58.dp).roundToPx() }
        val sheetTopPx = immersiveLyricsSheetTopPx(
            actualSettingsBottomPx,
            viewportHeightPx,
            with(density) { 8.dp.roundToPx() }
        )
        val sheetHeight = with(density) {
            (viewportHeightPx - sheetTopPx).coerceAtLeast(0).toDp()
        }
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

        // On compact/large-text screens the foreground can scroll rather than
        // clipping transport buttons. The artwork remains a full-screen canvas.
        val foreground: @Composable () -> Unit = {
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { controlsHeightPx = it.height }
                    .padding(horizontal = horizontal)
                    .windowInsetsPadding(contentInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .onSizeChanged { lyricHeaderHeightPx = it.height }
                        .graphicsLayer {
                            translationY = -headerLift.toPx() * lyricsTransition
                            // The rising translucent sheet covers this header.
                            // Fade the underlying text out with it to prevent
                            // duplicated song metadata showing through lyrics.
                            alpha = 1f - lyricsTransition
                        }
                ) {
                // Existing normal header stays unchanged at progress 0.
                // During Lyrics, a compact album tile joins the metadata as
                // the title, artist and Hi-Res badge shrink together.
                val expandedTitleSize = (titleFontSize *
                    if (compact) 0.72f else 0.78f).coerceIn(22f, 34f)
                val expandedLineHeight = (titleFontSize *
                    if (compact) 0.84f else 0.90f).coerceIn(27f, 40f)
                val compactTitleSize = if (compact) 16.5f else 18f
                val compactLineHeight = if (compact) 20f else 22f
                val titleSize = expandedTitleSize +
                    (compactTitleSize - expandedTitleSize) * lyricsTransition
                val lineHeight = expandedLineHeight +
                    (compactLineHeight - expandedLineHeight) * lyricsTransition

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = lyricsOpen,
                        enter = expandHorizontally(tween(460, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Start) + fadeIn(tween(340)),
                        exit = shrinkHorizontally(tween(420, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Start) + fadeOut(tween(260))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            miniArtwork(
                                Modifier.size(if (compact) 44.dp else 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, fontFamily = titleFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = titleSize.sp, lineHeight = lineHeight.sp,
                            color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().padding(
                            top = (6f - 3f * lyricsTransition).dp,
                            bottom = (4f - 2f * lyricsTransition).dp
                        ), verticalAlignment = Alignment.CenterVertically) {
                            Text(artist,
                                fontSize = (15f - 2.5f * lyricsTransition).sp,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                                    .clickable(enabled = hasSong, onClick = onArtist))
                            if (wide) {
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.graphicsLayer {
                                    scaleX = 1f - 0.19f * lyricsTransition
                                    scaleY = 1f - 0.19f * lyricsTransition
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }) { quality() }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = !lyricsOpen,
                        enter = expandHorizontally(tween(400, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.End) + fadeIn(tween(280)),
                        exit = shrinkHorizontally(tween(340, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.End) + fadeOut(tween(170))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(12.dp))
                            ImmersiveAction(
                                if (isFavorite) NusaPlayerIcons.HeartFilled else NusaPlayerIcons.Heart,
                                immersiveText("Favorit", "Favorite"), onFavorite, hasSong,
                                active = isFavorite, iconSize = 23.dp
                            )
                        }
                    }
                }
                if (!wide) Box(
                    Modifier.padding(top = (6f - 3f * lyricsTransition).dp)
                        .graphicsLayer {
                            scaleX = 1f - 0.19f * lyricsTransition
                            scaleY = 1f - 0.19f * lyricsTransition
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) { quality() }
                }
                Column(Modifier.fillMaxWidth()
                    .onSizeChanged { transportHeightPx = it.height }) {
                progress(Modifier.fillMaxWidth().padding(top = if (wide) 4.dp else if (compact) 10.dp else 14.dp))
                Spacer(Modifier.height(transportGap))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    ImmersiveAction(NusaPlayerIcons.Shuffle,
                        if (shuffleEnabled) immersiveText("Acak aktif", "Shuffle on") else immersiveText("Acak nonaktif", "Shuffle off"),
                        onShuffle, hasSong, active = shuffleEnabled, iconSize = 21.dp)
                    IconButton(onClick = onPrevious, enabled = hasSong, modifier = Modifier.size(52.dp)) {
                        Icon(NusaPlayerIcons.Previous, immersiveText("Sebelumnya", "Previous"),
                            tint = Color.White.copy(alpha = if (hasSong) 0.94f else 0.3f),
                            modifier = Modifier.size(32.dp))
                    }
                    // Clean, background-free Play/Pause control.
                    // The larger white glyph stays centred without glass,
                    // a filled circle, border or drop shadow.
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(if (compact) 68.dp else 72.dp)
                    ) {
                        Icon(
                            if (isPlaying) NusaPlayerIcons.Pause else NusaPlayerIcons.Play,
                            if (isPlaying) immersiveText("Jeda", "Pause") else immersiveText("Putar", "Play"),
                            tint = Color.White,
                            modifier = Modifier.size(if (compact) 38.dp else 41.dp)
                        )
                    }
                    IconButton(onClick = onNext, enabled = hasSong, modifier = Modifier.size(52.dp)) {
                        Icon(NusaPlayerIcons.Next, immersiveText("Berikutnya", "Next"),
                            tint = Color.White.copy(alpha = if (hasSong) 0.94f else 0.3f),
                            modifier = Modifier.size(32.dp))
                    }
                    ImmersiveAction(if (repeatMode == 1) NusaPlayerIcons.RepeatOne else NusaPlayerIcons.Repeat,
                        when (repeatMode) {
                            1 -> immersiveText("Ulangi satu lagu", "Repeat one")
                            2 -> immersiveText("Ulangi semua", "Repeat all")
                            else -> immersiveText("Ulangi nonaktif", "Repeat off")
                        }, onRepeat, hasSong, active = repeatMode != 0, iconSize = 21.dp)
                }
                Spacer(Modifier.height(transportGap))
                // A compact, centred capsule with 48dp touch targets.
                // Its width adapts when the Lyrics action is unavailable,
                // without squeezing the tap areas on small screens.
                val actionPillWidth = if (lyricsEnabled) 236.dp else 188.dp
                Row(Modifier.align(Alignment.CenterHorizontally)
                    .widthIn(max = actionPillWidth).fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(100.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    ImmersiveAction(NusaPlayerIcons.Artist,
                        immersiveText("Buka halaman artis", "Open artist page"),
                        onArtist, hasSong, iconSize = 21.dp)
                    if (lyricsEnabled) ImmersiveAction(NusaPlayerIcons.Lyrics,
                        if (lyricsOpen) immersiveText("Tutup lirik", "Close lyrics") else immersiveText("Buka lirik", "Open lyrics"),
                        onLyrics, hasSong, active = lyricsOpen, iconSize = 21.dp)
                    ImmersiveAction(NusaPlayerIcons.Library,
                        immersiveText("Perpustakaan", "Library"),
                        onLibrary, iconSize = 21.dp)
                    ImmersiveAction(NusaPlayerIcons.Share,
                        immersiveText("Bagikan lagu", "Share song"),
                        onShare, hasSong, iconSize = 21.dp)
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

        // A large rounded sheet rises from the physical bottom edge. Its
        // top is measured from the actual Settings row, with that top control
        // left OUTSIDE the sheet so it remains touchable at full expansion.
        ImmersiveExpandingLyricsSheet(
            visible = lyricsOpen,
            song = ambientSong,
            sheetHeight = sheetHeight,
            onDismiss = onDismissLyrics,
            lyrics = lyrics,
            // Explicitly ABOVE every transport/progress/pill button, even
            // while its spring is moving. Settings stays one layer higher.
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f)
        )

        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(3f)
            .windowInsetsPadding(contentInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 10.dp).height(48.dp)
            .onGloballyPositioned {
                val bottom = (it.positionInRoot().y + it.size.height -
                    playerRootTopPx).roundToInt()
                if (settingsRowBottomPx != bottom) settingsRowBottomPx = bottom
            },
            verticalAlignment = Alignment.CenterVertically) {
            // No upper-left back button in either Immersive presentation.
            // The Library pill and Android Back navigation remain available.
            Spacer(Modifier.weight(1f))
            ImmersiveAction(NusaPlayerIcons.More, immersiveText("Pengaturan", "Settings"),
                onSettings, enabled = true, iconSize = 25.dp, glass = true)
        }
    }
}

@Composable
private fun ImmersiveAction(
    icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true,
    active: Boolean = false, iconSize: Dp = 22.dp, glass: Boolean = false
) {
    IconButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.size(48.dp)
            .then(if (glass) Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)) else Modifier)
            .semantics { selected = active }) {
        Icon(icon, label, tint = Color.White.copy(alpha = if (!enabled) 0.26f else if (active) 1f else 0.62f),
            modifier = Modifier.size(iconSize))
    }
}
