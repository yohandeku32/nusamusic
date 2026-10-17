package com.yohandeku32.nusamusic.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableState
import kotlin.math.abs

/** Carry velocity into a new target instead of stopping at every line boundary. */
internal class LyricsFollowMotion {
    private var velocity = 0f
    fun reset() { velocity = 0f }

    suspend fun move(state: ScrollableState, distance: Float) {
        state.scroll {
            var previous = 0f
            AnimationState(initialValue = 0f, initialVelocity = velocity).animateTo(
                targetValue = distance,
                animationSpec = spring(dampingRatio = 1f, stiffness = 140f, visibilityThreshold = 0.5f)
            ) {
                val step = value - previous
                val consumed = scrollBy(step)
                previous = value
                this@LyricsFollowMotion.velocity = velocity
                if (abs(step - consumed) > 0.5f) {
                    reset()
                    cancelAnimation()
                }
            }
            reset()
        }
    }
}
