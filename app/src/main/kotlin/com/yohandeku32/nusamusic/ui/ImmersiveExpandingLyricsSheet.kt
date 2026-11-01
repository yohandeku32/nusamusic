package com.yohandeku32.nusamusic.ui

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.yohandeku32.nusamusic.data.ImmersiveArtworkCache
import com.yohandeku32.nusamusic.model.Song

/**
 * In the same Compose window as the player: no ModalBottomSheet, fixed-height
 * anchor or extra status bar. A single translated GPU layer rises from the
 * physical bottom edge to the safe top inset beneath the status bar.
 *
 * The original synchronized Lyrics view stays mounted until the exit spring
 * finishes, avoiding a flash or a reset of its wave/scroll clock mid-close.
 */
@Composable
internal fun ImmersiveExpandingLyricsSheet(
    visible: Boolean,
    song: Song?,
    sheetHeight: Dp,
    onDismiss: () -> Unit,
    lyrics: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sheetHeightPx = with(density) { sheetHeight.toPx().coerceAtLeast(1f) }
    val velocityThreshold = with(density) { 900.dp.toPx() }
    val scope = rememberCoroutineScope()
    val latestDismiss by rememberUpdatedState(onDismiss)
    val context = LocalContext.current
    var ambientBlur by remember(song?.uri) {
        mutableStateOf<Bitmap?>(song?.let { ImmersiveArtworkCache.cached(it)?.blur })
    }
    LaunchedEffect(song?.uri, visible) {
        if (!visible || song == null) return@LaunchedEffect
        // The player already prepared this exact album's blurred bitmap.
        // Reuse its in-memory preview or disk copy; never draw the controls
        // through a translucent overlay and never blur the UI itself.
        ambientBlur = ImmersiveArtworkCache.cached(song)?.blur
            ?: ImmersiveArtworkCache.restore(context, song)?.blur
    }
    val touchShield = remember { MutableInteractionSource() }
    val reveal = remember { Animatable(1f) } // 0 = fully open, 1 = offscreen
    var rendered by remember { mutableStateOf(visible) }
    var userDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(1f) }
    val sheetSpring = remember {
        spring<Float>(dampingRatio = 0.91f, stiffness = 370f)
    }
    // Rendering resources are stable even while the animation is moving.
    val sheetShape = remember { RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp) }
    val sheetTint = remember {
        // This glaze darkens an ALREADY-OPAQUE sheet made from real blurred
        // album art. Its alpha controls ambience, not transparency to buttons.
        Brush.verticalGradient(
            0f to Color(0xFF0B0D13).copy(alpha = 0.65f),
            0.42f to Color(0xFF0A0C14).copy(alpha = 0.70f),
            1f to Color(0xFF07080D).copy(alpha = 0.84f)
        )
    }

    LaunchedEffect(visible, sheetHeightPx) {
        if (visible) {
            rendered = true
            if (userDragging) {
                reveal.snapTo(dragFraction)
                userDragging = false
            }
            reveal.animateTo(0f, sheetSpring)
        } else {
            if (userDragging) {
                reveal.snapTo(dragFraction)
                userDragging = false
            }
            reveal.animateTo(1f, sheetSpring)
            rendered = false
        }
    }

    // Drag updates are simple scalar writes; the content is NOT remeasured or
    // recomposed as the sheet moves. The graphicsLayer samples this state.
    fun startDrag() {
        if (!userDragging) {
            dragFraction = reveal.value
            userDragging = true
            scope.launch { reveal.stop() }
        }
    }

    fun applyDrag(dy: Float): Float {
        if (!visible || (!userDragging && dy <= 0f)) return 0f
        startDrag()
        val before = dragFraction
        dragFraction = (before + dy / sheetHeightPx).coerceIn(0f, 1f)
        return (dragFraction - before) * sheetHeightPx
    }

    fun finishDrag(velocity: Float) {
        if (!userDragging) return
        val releasedFraction = dragFraction
        if (shouldDismissImmersiveLyricsSheet(
                releasedFraction, velocity, velocityThreshold
            )
        ) {
            // The parent flips visible to false, and the SAME spring continues
            // down from the live finger position. No immediate remove/snap.
            latestDismiss()
        } else {
            scope.launch {
                reveal.snapTo(releasedFraction)
                userDragging = false
                reveal.animateTo(0f, sheetSpring)
            }
        }
    }

    // Only UNCONSUMED downward drag from the lyric list reaches the sheet.
    // Normal lyrics browsing and auto-follow keep their own scroll priority.
    // Upward movement while the panel is partly dragged first re-expands it.
    val nestedConnection = remember(visible, sheetHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (source == NestedScrollSource.UserInput && userDragging && available.y < 0f) {
                    Offset(0f, applyDrag(available.y))
                } else Offset.Zero

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset =
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    Offset(0f, applyDrag(available.y))
                } else Offset.Zero

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (!userDragging) return Velocity.Zero
                finishDrag(available.y)
                return Velocity(0f, available.y.coerceAtLeast(0f))
            }
        }
    }

    if (!rendered) return

    val dragHandleState = rememberDraggableState { delta -> applyDrag(delta) }
    Box(
        modifier.fillMaxWidth().height(sheetHeight)
            .graphicsLayer {
                translationY = sheetHeightPx *
                    if (userDragging) dragFraction else reveal.value
                // Rounded only at the top; bottom reaches the display edge.
                shape = sheetShape
                clip = true
            }
            // Fully opaque foundation + artwork-matched bitmap ensure that
            // Play/Next/seek/pill never show through a "glass" gradient.
            .background(Color(0xFF12141A))
            // The whole sheet captures taps on blank pixels. Child gestures
            // (lyrics scroll/tap-to-seek, handle drag, close arrow) still win.
            .clickable(interactionSource = touchShield, indication = null) {}
    ) {
        Crossfade(
            targetState = ambientBlur,
            animationSpec = tween(durationMillis = 450),
            label = "immersiveLyricsAmbient",
            modifier = Modifier.matchParentSize()
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(Modifier.matchParentSize().background(sheetTint))

        Column(Modifier.fillMaxSize()) {
            // A real draggable region, not an artificial sheet-sized pointer
            // interceptor: scrolling/seek on the text remains interactive.
            Column(
                Modifier.fillMaxWidth()
                    .draggable(
                        state = dragHandleState,
                        orientation = Orientation.Vertical,
                        onDragStarted = { startDrag() },
                        onDragStopped = { velocity -> finishDrag(velocity) }
                    )
            ) {
                Box(
                    Modifier.fillMaxWidth().height(19.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(width = 37.dp, height = 4.dp)
                            .clip(RoundedCornerShape(100))
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }
                Row(
                    Modifier.fillMaxWidth().height(49.dp)
                        .padding(start = 15.dp, end = 26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { latestDismiss() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            NusaPlayerIcons.Back,
                            contentDescription = "Close lyrics",
                            tint = Color.White.copy(alpha = 0.93f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "Lyrics",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.padding(start = 3.dp)
                    )
                }
            }

            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .nestedScroll(nestedConnection)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                lyrics(Modifier.fillMaxSize())
            }
        }
    }
}
