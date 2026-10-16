package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// MIUIX-style critically damped settling: fluid, with no bounce past the page.
private val pageSpring = spring<Float>(dampingRatio = 1f, stiffness = 146f, visibilityThreshold = 1f)

internal suspend fun PagerState.animateMiuixScrollToPage(page: Int) {
    animateScrollToPage(page = page, animationSpec = pageSpring)
}

@Composable
internal fun MiuixHorizontalPager(
    state: PagerState,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    HorizontalPager(
        state = state,
        modifier = modifier.background(backgroundColor).clipToBounds(),
        beyondViewportPageCount = 1,
        flingBehavior = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
            snapAnimationSpec = pageSpring
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
