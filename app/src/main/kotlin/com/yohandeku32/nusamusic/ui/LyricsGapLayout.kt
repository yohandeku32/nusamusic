package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal val lyricsGapRowHeight = 64.dp

/** Reserve space only while the authored countdown exists; all movement follows audio time. */
internal fun countdownLayoutFraction(gap: LyricsGap, now: Int): Float =
    if (now < gap.start || now >= gap.end) 0f
    else lyricEase((now.toLong() - gap.start) / 120f) * countdownFollowOffset(gap, now)

/** Center the countdown during a gap, then blend back to the main text as its space collapses. */
internal fun countdownRowAnchor(mainHeight: Float, fullGapHeight: Float, fraction: Float): Float {
    val amount = fraction.coerceIn(0f, 1f)
    val space = fullGapHeight * amount
    val mainCenter = space + mainHeight / 2f
    return mainCenter * (1f - amount) + (space / 2f) * amount
}

@Composable
internal fun LyricsGapRow(gap: LyricsGap, time: () -> Int) {
    val fraction = remember(gap, time) {
        derivedStateOf(structuralEqualityPolicy()) { countdownLayoutFraction(gap, time()) }
    }
    Box(Modifier.fillMaxWidth().layout { measurable, constraints ->
        val child = measurable.measure(constraints.copy(minHeight = 0))
        val height = (child.height * fraction.value).roundToInt().coerceIn(0, child.height)
        layout(child.width, height) {
            child.placeRelative(0, -(child.height - height) / 2)
        }
    }.height(lyricsGapRowHeight)) {
        LyricsGapDots(gap, time, Modifier.align(Alignment.CenterStart).padding(start = 16.dp))
    }
}
