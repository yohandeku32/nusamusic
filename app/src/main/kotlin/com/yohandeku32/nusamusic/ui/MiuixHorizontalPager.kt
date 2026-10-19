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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// MIUIX-style critically damped settling: fluid, with no bounce past the page.
internal val miuixPageSpring = spring<Float>(dampingRatio = 0.92f, stiffness = 195f, visibilityThreshold = 1f)

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
        flingBehavior = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
            snapAnimationSpec = miuixPageSpring
        )
    ) { page ->
        Box(
            Modifier.fillMaxSize()
                // Higher-index pages slide over the quarter-speed covered page.
                .zIndex(page.toFloat())
                .graphicsLayer {
                    val depth = state.currentPage + state.currentPageOffsetFraction - page
                    val visuals = miuixPagerVisuals(depth, size.width, isRtl)
                    translationX = visuals.translationX
                    scaleX = visuals.scale
                    scaleY = visuals.scale
                    alpha = visuals.alpha
                    clip = true
                    shape = if (visuals.leadingCornerFraction > 0f) {
                        val radius = 24.dp * visuals.leadingCornerFraction
                        RoundedCornerShape(topStart = radius, bottomStart = radius)
                    } else RectangleShape
                }
                .background(backgroundColor)
                .drawWithContent {
                    drawContent()
                    val depth = state.currentPage + state.currentPageOffsetFraction - page
                    val dim = miuixPagerVisuals(depth, size.width, isRtl).scrimAlpha
                    if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
                }
        ) {
            content(page)
        }
    }
}
