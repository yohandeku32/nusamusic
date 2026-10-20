package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// Critically damped settle, with no rubber-band overshoot. Native pager
// scrolling carries the full page so both the image and touch area line up.
internal val miuixPageSpring = spring<Float>(dampingRatio = 1f, stiffness = 330f, visibilityThreshold = 1f)

internal suspend fun PagerState.animateMiuixScrollToPage(page: Int) {
    animateScrollToPage(page = page, animationSpec = miuixPageSpring)
}

@Composable
internal fun MiuixHorizontalPager(
    state: PagerState,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    onHorizontalSwipeStarted: () -> Unit = {},
    // The actual page surface and the subtly contrasting outer corner
    // backing are provided separately. A light Library must never inherit
    // the player pager's black background at its rounded corners.
    pageSurface: (Int) -> Color = { backgroundColor },
    pageCornerBacking: (Int) -> Color = pageSurface,
    content: @Composable (Int) -> Unit
) {
    val swipeAction by rememberUpdatedState(onHorizontalSwipeStarted)
    // Single reusable outline. A 28dp leading curve makes the card edge
    // recognizable without rounding the right edge or distorting content.
    val cardShape = remember { RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp) }
    HorizontalPager(
        state = state,
        modifier = modifier.background(backgroundColor).clipToBounds()
            // Observe the swipe at Initial pass without consuming it. Pager
            // retains full control over the gesture; the keyboard is dismissed
            // at touch-slop crossing, not when the page settles.
            .pointerInput(userScrollEnabled) {
                if (!userScrollEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val touchSlop = viewConfiguration.touchSlop
                    var horizontal = 0f
                    var vertical = 0f
                    var pointer = down.id
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointer }
                            ?: event.changes.firstOrNull { it.pressed }
                            ?: break
                        if (!change.pressed) break
                        val delta = change.position - change.previousPosition
                        horizontal += delta.x
                        vertical += delta.y
                        if (kotlin.math.abs(horizontal) > touchSlop &&
                            kotlin.math.abs(horizontal) > kotlin.math.abs(vertical) * 1.15f
                        ) {
                            swipeAction()
                            break
                        }
                        pointer = change.id
                    }
                }
            },
        beyondViewportPageCount = 1,
        userScrollEnabled = userScrollEnabled,
        // Android's stretch overscroll reads as jelly at the first/last page.
        overscrollEffect = null,
        flingBehavior = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
            snapAnimationSpec = miuixPageSpring
        )
    ) { page ->
        val interactive by remember(state, page) {
            derivedStateOf {
                miuixPageAcceptsInput(
                    page, state.settledPage, state.currentPage,
                    state.currentPageOffsetFraction, state.isScrollInProgress
                )
            }
        }
        val surface = pageSurface(page)
        val backing = pageCornerBacking(page)
        Box(
            Modifier.fillMaxSize()
                // Paint a full-rectangle *theme-aware* backing OUTSIDE the
                // rounded graphics layer. Otherwise the light Library reveals
                // the pager's black base through the top-left cutout.
                // The quiet cool-grey backing is visible only in the curve.
                .background(backing)
                // Fixed iOS-style navigation order (Artist, Player, Library).
                // No z-index flips in the middle of a gesture.
                .zIndex(miuixPageZIndex(page))
                // No custom X translation, alpha or scale: the native pager
                // keeps every visual at its true hit-test coordinate.
                // Clip only the leading corners of the moving front card.
                // The backing is above this modifier, so it stays un-clipped.
                .graphicsLayer {
                    val position = state.currentPage + state.currentPageOffsetFraction
                    val card = miuixCardVisuals(page, position, state.isScrollInProgress)
                    shape = cardShape
                    clip = card.shadowElevationDp > 0f
                    shadowElevation = card.shadowElevationDp.dp.toPx()
                }
                // Cheap internal edge shadow makes the card separation
                // visible even if the pager clips real elevation outside
                // the page bounds. The brush is cached; only alpha changes.
                .drawWithCache {
                    val band = 36.dp.toPx()
                    val brush = Brush.horizontalGradient(
                        0f to Color.Black,
                        0.22f to Color.Black.copy(alpha = 0.54f),
                        1f to Color.Transparent,
                        startX = 0f, endX = band
                    )
                    onDrawWithContent {
                        drawContent()
                        val position = state.currentPage + state.currentPageOffsetFraction
                        val card = miuixCardVisuals(page, position, state.isScrollInProgress)
                        if (card.edgeShadeAlpha > 0f) {
                            drawRect(brush = brush, alpha = card.edgeShadeAlpha,
                                size = Size(band, size.height))
                        }
                    }
                }
                .pagerPageInput { interactive }
                .then(if (interactive) Modifier else Modifier.clearAndSetSemantics { })
                // Use the correct *per-page* surface even in its status bar
                // and empty layout areas; never fill Library gaps with black.
                .background(surface)
        ) {
            content(page)
        }
    }
}

/**
 * Cancel taps begun on a hidden/moving page, including a second finger during
 * settling. Never consume movement: the ancestor Pager must still own swipes.
 * Once blocked, a gesture stays blocked even if settling ends before finger-up.
 */
internal fun Modifier.pagerPageInput(acceptsInput: () -> Boolean): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(false, PointerEventPass.Initial)
            var blocked = !acceptsInput()
            var dragged = false
            if (blocked) down.consume()
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.firstOrNull { it.id == down.id }?.let { change ->
                    dragged = dragged ||
                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                }
                blocked = blocked || !acceptsInput()
                if (blocked) {
                    event.changes.forEach { change ->
                        val newDown = change.pressed && !change.previousPressed
                        val tapUp = !change.pressed && change.previousPressed && !dragged
                        // Leave a drag's UP intact so Pager can calculate fling
                        // velocity and settle. Its drag already cancelled clicks.
                        if (newDown || tapUp) change.consume()
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
