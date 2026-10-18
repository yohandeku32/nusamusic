package com.yohandeku32.nusamusic.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
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
        val horizontal = if (wide) 16.dp else if (compact) 24.dp else 28.dp
        // Preserve current vertical hierarchy, but give transport and pill
        // independent breathing room. Short screens compress only the gap.
        val transportPillGap = when {
            maxHeight < 640.dp -> 14.dp
            wide -> 18.dp
            compact -> 20.dp
            else -> 26.dp
        }
        var controlsHeightPx by remember { mutableIntStateOf(0) }
        val controlsHeight = with(density) { controlsHeightPx.toDp() }
        // The extra air beneath transport should not steal vertical space
        // from the artwork: compensate only the newly introduced gap.
        val addedTransportGap = (transportPillGap -
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontFamily = titleFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = (titleFontSize * if (compact) 0.72f else 0.78f).coerceIn(22f, 34f).sp,
                            lineHeight = (titleFontSize * if (compact) 0.84f else 0.90f).coerceIn(27f, 40f).sp,
                            color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(artist, fontSize = 15.sp, color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).clickable(enabled = hasSong, onClick = onArtist))
                            if (wide) {
                                Spacer(Modifier.width(8.dp))
                                quality()
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    ImmersiveAction(
                        if (isFavorite) NusaPlayerIcons.HeartFilled else NusaPlayerIcons.Heart,
                        immersiveText("Favorit", "Favorite"), onFavorite, hasSong,
                        active = isFavorite, iconSize = 23.dp
                    )
                }
                if (!wide) Box(Modifier.padding(top = 6.dp)) { quality() }
                progress(Modifier.fillMaxWidth().padding(top = if (wide) 4.dp else if (compact) 10.dp else 14.dp))
                Spacer(Modifier.height(if (wide) 6.dp else if (compact) 10.dp else 16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    ImmersiveAction(NusaPlayerIcons.Shuffle,
                        if (shuffleEnabled) immersiveText("Acak aktif", "Shuffle on") else immersiveText("Acak nonaktif", "Shuffle off"),
                        onShuffle, hasSong, active = shuffleEnabled, iconSize = 21.dp)
                    IconButton(onClick = onPrevious, enabled = hasSong, modifier = Modifier.size(48.dp)) {
                        Icon(NusaPlayerIcons.Previous, immersiveText("Sebelumnya", "Previous"),
                            tint = Color.White.copy(alpha = if (hasSong) 0.94f else 0.3f), modifier = Modifier.size(29.dp))
                    }
                    IconButton(onClick = onPlayPause,
                        modifier = Modifier.size(if (compact) 64.dp else 68.dp)
                            .clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                        Icon(if (isPlaying) NusaPlayerIcons.Pause else NusaPlayerIcons.Play,
                            if (isPlaying) immersiveText("Jeda", "Pause") else immersiveText("Putar", "Play"),
                            tint = Color.White, modifier = Modifier.size(if (compact) 35.dp else 38.dp))
                    }
                    IconButton(onClick = onNext, enabled = hasSong, modifier = Modifier.size(48.dp)) {
                        Icon(NusaPlayerIcons.Next, immersiveText("Berikutnya", "Next"),
                            tint = Color.White.copy(alpha = if (hasSong) 0.94f else 0.3f), modifier = Modifier.size(29.dp))
                    }
                    ImmersiveAction(if (repeatMode == 1) NusaPlayerIcons.RepeatOne else NusaPlayerIcons.Repeat,
                        when (repeatMode) {
                            1 -> immersiveText("Ulangi satu lagu", "Repeat one")
                            2 -> immersiveText("Ulangi semua", "Repeat all")
                            else -> immersiveText("Ulangi nonaktif", "Repeat off")
                        }, onRepeat, hasSong, active = repeatMode != 0, iconSize = 21.dp)
                }
                Spacer(Modifier.height(transportPillGap))
                Row(Modifier.align(Alignment.CenterHorizontally)
                    .widthIn(max = 300.dp).fillMaxWidth(0.94f)
                    .height(if (viewportHeight < 640.dp) 56.dp else 60.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(100.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    ImmersiveAction(NusaPlayerIcons.Artist, immersiveText("Buka halaman artis", "Open artist page"), onArtist, hasSong)
                    if (lyricsEnabled) ImmersiveAction(NusaPlayerIcons.Lyrics,
                        if (lyricsOpen) immersiveText("Tutup lirik", "Close lyrics") else immersiveText("Buka lirik", "Open lyrics"),
                        onLyrics, hasSong, active = lyricsOpen)
                    ImmersiveAction(NusaPlayerIcons.Library, immersiveText("Perpustakaan", "Library"), onLibrary)
                    ImmersiveAction(NusaPlayerIcons.Share, immersiveText("Bagikan lagu", "Share song"), onShare, hasSong)
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

        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth()
            .windowInsetsPadding(contentInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 10.dp).height(48.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLibrary, modifier = Modifier.size(48.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.28f))) {
                Icon(NusaPlayerIcons.Back, immersiveText("Kembali ke perpustakaan", "Back to library"),
                    tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.weight(1f))
            ImmersiveAction(NusaPlayerIcons.More, immersiveText("Pengaturan", "Settings"),
                onSettings, enabled = !lyricsOpen, iconSize = 25.dp, glass = true)
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
