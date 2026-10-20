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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex

// Critically damped: natural response to a release, no rubber-band overshoot.
internal val miuixPageSpring = spring<Float>(dampingRatio = 1f, stiffness = 345f, visibilityThreshold = 1f)

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
    content: @Composable (Int) -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val swipeAction by rememberUpdatedState(onHorizontalSwipeStarted)
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
        Box(
            Modifier.fillMaxSize()
                // Place the incoming MIUIX layer above the outgoing page while
                // swiping, then return ownership to the settled page at rest.
                // Hidden neighbours still cannot intercept player button taps.
                .zIndex(miuixPageZIndex(page, state.settledPage, state.isScrollInProgress))
                // Genuine 58% finger-driven glide. Keep both surfaces opaque
                // through the mid-swipe, avoiding a full-screen dissolve and
                // offscreen pixel blending across expensive artwork and Media3.
                // Updates remain in the draw phase, never on the layout path.
                .graphicsLayer {
                    val depth = state.currentPage +
                        state.currentPageOffsetFraction - page
                    val visuals = miuixPagerVisuals(depth, size.width, isRtl)
                    translationX = visuals.translationX
                    alpha = visuals.alpha
                    scaleX = visuals.scale
                    scaleY = visuals.scale
                    clip = true
                }
                .pagerPageInput { interactive }
                .then(if (interactive) Modifier else Modifier.clearAndSetSemantics { })
                .background(backgroundColor)
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
