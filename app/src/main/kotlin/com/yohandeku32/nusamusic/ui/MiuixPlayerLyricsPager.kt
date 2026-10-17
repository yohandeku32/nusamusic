package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/** Page 0 is the player; page 1 slides up from below to show lyrics. */
@Composable
internal fun MiuixPlayerLyricsPager(
    enabled: Boolean,
    state: PagerState,
    backgroundColor: Color,
    content: @Composable (Int) -> Unit
) {
    if (!enabled) {
        content(0)
        return
    }
    VerticalPager(
        state = state,
        modifier = Modifier.fillMaxSize().background(backgroundColor).clipToBounds(),
        beyondViewportPageCount = 1,
        flingBehavior = PagerDefaults.flingBehavior(
            state = state,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
            snapAnimationSpec = miuixPageSpring
        )
    ) { page ->
        Box(
            Modifier.fillMaxSize().zIndex(page.toFloat()).graphicsLayer {
                val depth = state.currentPage + state.currentPageOffsetFraction - page
                val visuals = miuixPagerVisuals(depth, size.height, isRtl = false)
                translationY = visuals.translationX
                alpha = visuals.alpha
                clip = true
                val radius = 24.dp * visuals.leadingCornerFraction
                shape = if (radius > 0.dp) RoundedCornerShape(topStart = radius, topEnd = radius) else RectangleShape
            }.background(backgroundColor).drawWithContent {
                drawContent()
                val depth = state.currentPage + state.currentPageOffsetFraction - page
                val dim = miuixPagerVisuals(depth, size.height, isRtl = false).scrimAlpha
                if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
            }
        ) { content(page) }
    }
}
