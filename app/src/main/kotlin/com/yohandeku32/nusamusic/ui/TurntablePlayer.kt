package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val turntable: String = "Turntable"
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
    avatar: @Composable (Modifier) -> Unit,
    record: @Composable (Modifier) -> Unit,
    quality: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    emptyState: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
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
        Box(modifier.fillMaxWidth().background(VinylPaper)) {
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
            val minimumHeight = topPadding + 48.dp + headerGap + 112.dp + recordGap +
                textAllowance + titleGap + progressGap + 48.dp + transportGap + transportSize +
                footerGap + 48.dp + qualityGap + 44.dp + bottomPadding + navigationHeight +
                if (hasSong) 0.dp else 96.dp
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
                    val deckHeight = minOf(maxWidth, maxHeight - 8.dp)
                    if (deckHeight >= 88.dp) {
                        VinylTurntableDeck(
                            size = maxWidth, height = deckHeight, isPlaying = isPlaying, isVisible = isVisible,
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
                TurntableProgress(positionMsState, durationMs, hasSong, labels.seek, onSeek)
                Spacer(Modifier.height(transportGap))
                Row(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = VinylGutter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TurntableButton(
                        Icons.Rounded.FastRewind, labels.previous, onPrevious, hasSong,
                        Modifier.weight(1f).height(space(52, 64)), 32.dp
                    )
                    TurntableButton(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (isPlaying) labels.pause else labels.play,
                        onPlayPause, true, Modifier.size(transportSize), 42.dp, primary = true
                    )
                    TurntableButton(
                        Icons.Rounded.FastForward, labels.next, onNext, hasSong,
                        Modifier.weight(1f).height(space(52, 64)), 32.dp
                    )
                }
                Spacer(Modifier.height(footerGap))
                Box(Modifier.fillMaxWidth().height(48.dp),
                    contentAlignment = Alignment.Center) { footer() }
                emptyState()
            }
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
        shape = if (primary) CircleShape else RoundedCornerShape(50),
        color = if (primary) Color(0xFF111111) else Color(0xFFE3E2DD),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(size), tint = if (primary) Color.White
                else Color(0xFF505055).copy(alpha = if (enabled) 1f else 0.38f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TurntableProgress(
    positionMsState: State<Long>,
    durationMs: Long,
    hasSong: Boolean,
    label: String,
    onSeek: (Long) -> Unit
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
            modifier = Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = label },
            thumb = {},
            track = { state ->
                Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                    val y = size.height / 2f
                    // Material reserves half the track height at each end, even without a thumb.
                    val start = -size.height / 2f
                    val end = size.width + size.height / 2f
                    drawLine(Color(0xFFCECDC8), Offset(start, y), Offset(end, y), 3.dp.toPx(), StrokeCap.Butt)
                    drawLine(VinylInk, Offset(start, y), Offset(start + (end - start) * state.value, y),
                        3.dp.toPx(), StrokeCap.Butt)
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(time(positionMs), fontSize = 14.sp, lineHeight = 20.sp)
            Text(time(durationMs), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
