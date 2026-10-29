package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// One physics engine for gestures, button navigation and cancellation. The
// moving page and its touch targets stay under native HorizontalPager layout.
internal val miuixPageSpring = spring<Float>(
    dampingRatio = 1f, stiffness = 330f, visibilityThreshold = 1f
)

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
    pageSurface: (Int) -> Color = { backgroundColor },
    content: @Composable (Int) -> Unit
) {
    val swipeAction by rememberUpdatedState(onHorizontalSwipeStarted)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // The page is 36dp wider than the viewport, with a matching negative
    // spacing. The scroll stride therefore stays EXACTLY one screen wide.
    // Every page owns a real 36dp underlay behind the next card's cut-out.
    // No screenshot, fake translation, transparent filler, or page-coordinate
    // mismatch is needed, even on a very slow or reversed swipe.
    val underlap = 36.dp
    val overlappingPageSize = remember {
        object : PageSize {
            override fun Density.calculateMainAxisPageSize(
                availableSpace: Int, pageSpacing: Int
            ): Int = (availableSpace - pageSpacing).coerceAtLeast(1)
        }
    }
    val cardShape = remember(isRtl) {
        if (isRtl) RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp)
        else RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp)
    }

    BoxWithConstraints(modifier.background(backgroundColor).clipToBounds()) {
        // Content is constrained to the *actual screen*, not the slightly
        // wider pager cell. Artist, controls, and Library retain their original
        // measured positions and touch-target coordinates.
        val viewportWidth = maxWidth

        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxSize()
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
            pageSize = overlappingPageSize,
            pageSpacing = -underlap,
            beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled,
            overscrollEffect = null,
            flingBehavior = PagerDefaults.flingBehavior(
                state = state,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
                snapAnimationSpec = miuixPageSpring
            )
        ) { page ->
            val surface = pageSurface(page)
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
                    // The higher page rests IN FRONT of the previous page
                    // even when reversing a gesture. Never flip z-index mid-swipe.
                    .zIndex(miuixPageZIndex(page))
                    // Only the entering/front card is masked; because the
                    // previous page actually overlaps for 36dp underneath,
                    // its real pixels fill the curved corner with no black/gray
                    // fake patch. Clipping is disabled completely at rest.
                    .graphicsLayer {
                        val position = state.currentPage +
                            state.currentPageOffsetFraction
                        val card = miuixCardVisuals(
                            page, position, state.isScrollInProgress
                        )
                        val active = card.shadowElevationDp > 0f
                        shape = if (active) cardShape else RectangleShape
                        clip = active
                        shadowElevation = card.shadowElevationDp.dp.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = 0.28f)
                        spotShadowColor = Color.Black.copy(alpha = 0.32f)
                    }
                    // Drawing stays inside the same card clip. Use a cached
                    // soft directional seam rather than a second blur layer.
                    .drawWithCache {
                        val band = 30.dp.toPx()
                        val brush = Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.7f),
                            1f to Color.Transparent,
                            startX = if (isRtl) size.width - band else 0f,
                            endX = if (isRtl) size.width else band
                        )
                        onDrawWithContent {
                            drawContent()
                            val position = state.currentPage +
                                state.currentPageOffsetFraction
                            val card = miuixCardVisuals(
                                page, position, state.isScrollInProgress
                            )
                            if (card.edgeShadeAlpha > 0f) {
                                drawRect(
                                    brush = brush, alpha = card.edgeShadeAlpha,
                                    topLeft = androidx.compose.ui.geometry.Offset(
                                        if (isRtl) size.width - band else 0f, 0f
                                    ),
                                    size = Size(band, size.height)
                                )
                            }
                        }
                    }
                    .pagerPageInput { interactive }
                    .then(if (interactive) Modifier else Modifier.clearAndSetSemantics { })
                    // Paint the entire 36dp extension using the real theme.
                    .background(surface)
            ) {
                Box(
                    Modifier.width(viewportWidth).fillMaxHeight()
                        .background(surface)
                ) {
                    content(page)
                }
            }
        }
    }
}

/**
 * Reject a tap that began on a hidden or moving page. Leave drag movement
 * untouched so HorizontalPager handles velocity, settling and cancellations.
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
                        (change.position - down.position).getDistance() >
                            viewConfiguration.touchSlop
                }
                blocked = blocked || !acceptsInput()
                if (blocked) {
                    event.changes.forEach { change ->
                        val newDown = change.pressed && !change.previousPressed
                        val tapUp = !change.pressed &&
                            change.previousPressed && !dragged
                        if (newDown || tapUp) change.consume()
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
