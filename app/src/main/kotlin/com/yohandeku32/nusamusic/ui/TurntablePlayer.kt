package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import com.yohandeku32.nusamusic.data.AudioWaveform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val VinylPaper = Color(0xFFF2F1EB)
private val VinylInk = Color(0xFF171717)
private val VinylContentWidth = 512.dp
private val VinylGutter = 36.dp
private const val VinylDeckScale = 0.96f // Subtle 4% reduction; leave other content gutters untouched.

data class TurntableLabels(
    val play: String,
    val pause: String,
    val previous: String,
    val next: String,
    val shuffle: String,
    val shuffleOn: String,
    val shuffleOff: String,
    val repeat: String,
    val repeatOff: String,
    val repeatAll: String,
    val repeatOne: String,
    val seek: String,
    val settings: String,
    val more: String,
    val turntable: String = "Turntable",
    val waveformLoading: String = "Reading audio waveform",
    val waveformUnavailable: String = "Audio waveform unavailable",
    val waveformReady: String = "Audio waveform",
    val songs: String = "Songs"
)

@Composable
fun TurntablePlayer(
    title: String,
    artist: String,
    titleFontFamily: FontFamily,
    titleFontSize: Float,
    viewportHeight: Dp,
    isPlaying: Boolean,
    isVisible: Boolean,
    hasSong: Boolean,
    positionMsState: State<Long>,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    labels: TurntableLabels,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSettings: () -> Unit,
    onOpenSongs: () -> Unit,
    avatar: @Composable (Modifier) -> Unit,
    record: @Composable (Modifier) -> Unit,
    quality: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    emptyState: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    waveform: AudioWaveform? = null,
    isWaveformLoading: Boolean = false,
    artworkColor: Color? = null
) {
    val artworkTint = animateColorAsState(vinylArtworkTint(artworkColor), tween(450), label = "Vinyl artwork tint")
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = VinylPaper,
            onBackground = VinylInk,
            surface = VinylPaper,
            onSurface = VinylInk,
            surfaceVariant = Color(0xFFE3E2DD),
            onSurfaceVariant = Color(0xFF505055),
            primary = VinylInk,
            onPrimary = Color.White
        )
    ) {
        Column(modifier.fillMaxWidth().drawWithCache {
            val tint = artworkTint.value
            val gradient = Brush.verticalGradient(
                0f to VinylPaper, 0.52f to VinylPaper,
                0.82f to lerp(VinylPaper, tint, 0.6f), 1f to tint
            )
            onDrawBehind { drawRect(gradient) }
        }) {
            val titleSize = titleFontSize.coerceIn(24f, 56f)
            val density = LocalDensity.current
            val room = ((viewportHeight.value - 640f) / 240f).coerceIn(0f, 1f)
            fun space(compact: Int, tall: Int) = (compact + (tall - compact) * room).dp
            val topPadding = space(12, 36)
            val headerGap = space(8, 16)
            val recordGap = space(12, 28)
            val titleGap = space(6, 8)
            val progressGap = space(8, 20)
            val transportGap = space(4, 8)
            val transportSize = space(76, 92)
            val footerGap = space(8, 24)
            val qualityGap = 2.dp
            val bottomPadding = space(8, 12)
            val navigationHeight = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            val titleStyle = LocalTextStyle.current.copy(
                fontFamily = titleFontFamily, fontWeight = FontWeight.Bold,
                fontSize = titleSize.sp, lineHeight = (titleSize * 1.08f).sp
            )
            val titleMeasure = rememberTextMeasurer().measure("M\nM", style = titleStyle)
            val titleHeight = with(density) { titleMeasure.size.height.toDp() }
            val textAllowance = with(density) {
                (titleSize * 2.16f).sp.toDp() + 22.sp.toDp() + 20.sp.toDp()
            }
            // Reserve the same player height during song restoration: a temporary empty
            // library must not change the weighted deck size on the first frames.
            val minimumHeight = topPadding + 48.dp + headerGap + 112.dp + recordGap +
                textAllowance + titleGap + progressGap + 48.dp + transportGap + transportSize +
                footerGap + 48.dp + qualityGap + 44.dp + bottomPadding + navigationHeight
            Column(
                Modifier.fillMaxWidth().height(maxOf(viewportHeight, minimumHeight))
                    .navigationBarsPadding().padding(top = topPadding, bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    avatar(Modifier.size(44.dp))
                    Spacer(Modifier.weight(1f))
                    var moreOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { moreOpen = true }) {
                            Icon(Icons.Default.MoreHoriz, labels.more)
                        }
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (shuffleEnabled) labels.shuffleOn else labels.shuffleOff) },
                                enabled = hasSong,
                                onClick = { onShuffle(); moreOpen = false }
                            )
                            DropdownMenuItem(
                                text = { Text(when (repeatMode) {
                                    1 -> labels.repeatOne
                                    2 -> labels.repeatAll
                                    else -> labels.repeatOff
                                }) },
                                enabled = hasSong,
                                onClick = { onRepeat(); moreOpen = false }
                            )
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, labels.settings)
                    }
                }
                Spacer(Modifier.height(headerGap))
                BoxWithConstraints(
                    Modifier.widthIn(max = VinylContentWidth).fillMaxWidth()
                        .padding(horizontal = VinylGutter).weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val availableDeckHeight = minOf(maxWidth, maxHeight - 8.dp)
                    val deckWidth = maxWidth * VinylDeckScale
                    val deckHeight = availableDeckHeight * VinylDeckScale
                    if (availableDeckHeight >= 88.dp) {
                        VinylTurntableDeck(
                            size = deckWidth, height = deckHeight, isPlaying = isPlaying, isVisible = isVisible,
                            hasSong = hasSong, positionMsState = positionMsState,
                            durationMs = durationMs, repeatMode = repeatMode, labels = labels,
                            onPlayPause = onPlayPause, onRepeat = onRepeat, record = record
                        )
                    }
                }
                Spacer(Modifier.height(recordGap))
                Column(
                    Modifier.widthIn(max = VinylContentWidth).fillMaxWidth().padding(horizontal = VinylGutter)
                        .heightIn(min = titleHeight + titleGap + with(density) { 22.sp.toDp() } + qualityGap + 44.dp)
                        .offset(y = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        title,
                        style = titleStyle,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(titleGap))
                    Text(
                        artist,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(qualityGap))
                    Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) { quality() }
                }
                Spacer(Modifier.height(progressGap))
                TurntableProgress(positionMsState, durationMs, hasSong, labels, onSeek, waveform, isWaveformLoading,
                    isPlaying, isVisible)
                Spacer(Modifier.height(transportGap))
                BoxWithConstraints(
                    Modifier.widthIn(max = VinylContentWidth).fillMaxWidth()
                        .padding(horizontal = VinylGutter).height(transportSize)
                        .testTag("vinyl-transport")
                ) {
                    val compact = maxWidth < 280.dp
                    val side = if (compact) 44.dp else 48.dp
                    val primarySize = if (compact) 64.dp else 72.dp
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TurntableOption(if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            labels.repeat, when (repeatMode) {
                                1 -> labels.repeatOne
                                2 -> labels.repeatAll
                                else -> labels.repeatOff
                            }, repeatMode != 0, hasSong, onRepeat, side)
                        TurntableButton(
                            Icons.Rounded.SkipPrevious, labels.previous, onPrevious, hasSong,
                            Modifier.size(side), 44.dp
                        )
                        TurntableButton(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (isPlaying) labels.pause else labels.play,
                            onPlayPause, true, Modifier.size(primarySize), 40.dp, primary = true
                        )
                        TurntableButton(
                            Icons.Rounded.SkipNext, labels.next, onNext, hasSong,
                            Modifier.size(side), 44.dp
                        )
                        IconButton(onClick = onOpenSongs, modifier = Modifier.size(side)) {
                            Icon(Icons.Rounded.QueueMusic, labels.songs, Modifier.size(26.dp),
                                tint = VinylInk.copy(alpha = 0.7f))
                        }
                    }
                }
                Spacer(Modifier.height(footerGap))
                Box(Modifier.fillMaxWidth().height(48.dp),
                    contentAlignment = Alignment.Center) { footer() }
            }
            // Empty-library copy lives outside the weighted player column so async
            // restore/scan results cannot resize the deck or shift playback controls.
            emptyState()
        }
    }
}

@Composable
private fun TurntableButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    size: Dp,
    primary: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        border = if (primary) BorderStroke(2.dp, VinylInk) else null,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(size), tint = VinylInk.copy(alpha = if (enabled) 1f else 0.38f))
        }
    }
}

@Composable
private fun TurntableOption(icon: ImageVector, label: String, state: String, active: Boolean,
    enabled: Boolean, onClick: () -> Unit, side: Dp) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(side).semantics {
        contentDescription = label
        stateDescription = state
        selected = active
    }) {
        Box(Modifier.size(24.dp)) {
            Icon(icon, null, Modifier.fillMaxSize(), tint = VinylInk.copy(
                alpha = if (!enabled) 0.38f else if (active) 1f else 0.7f))
            if (active && enabled) Box(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-2).dp)
                .size(4.dp).background(Color(0xFFD91C16), CircleShape))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TurntableProgress(
    positionMsState: State<Long>,
    durationMs: Long,
    hasSong: Boolean,
    labels: TurntableLabels,
    onSeek: (Long) -> Unit,
    waveform: AudioWaveform?,
    isWaveformLoading: Boolean,
    isPlaying: Boolean,
    isVisible: Boolean
) {
    val positionMs by positionMsState
    val enabled = hasSong && durationMs > 0L
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    fun time(ms: Long): String {
        val seconds = ms.coerceAtLeast(0L) / 1000L
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
    Column(Modifier.widthIn(max = VinylContentWidth).fillMaxWidth().padding(horizontal = VinylGutter)
        .testTag("vinyl-progress")) {
        Slider(
            value = fraction,
            onValueChange = { onSeek((it * durationMs).toLong()) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(48.dp).semantics {
                contentDescription = labels.seek
                if (hasSong) stateDescription = when {
                    waveform != null -> labels.waveformReady
                    isWaveformLoading -> labels.waveformLoading
                    else -> labels.waveformUnavailable
                }
            },
            thumb = { Spacer(Modifier.size(width = 4.dp, height = 0.dp)) },
            track = { state ->
                VinylWaveformTrack(waveform, hasSong, isPlaying, isVisible) { state.value }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(time(positionMs), fontSize = 14.sp, lineHeight = 20.sp)
            Text(time(durationMs), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
