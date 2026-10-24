package com.yohandeku32.nusamusic.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Full-screen compositor-only Artist overlay. The upper-left corner stays
 * clear of duplicate profile icons; the bottom pill handles navigation.
 *
 * No blur, layout-frame animation, whole-page scaling, bounce or overlaid
 * artwork leaks. The profile always fills the viewport; the real four-action
 * pill is composed after this portal so it floats above the Artist content.
 */
@Composable
internal fun ImmersiveArtistPortal(
    open: Boolean,
    progress: () -> Float,
    source: Rect?,
    host: Rect?,
    topInsetPx: Float,
    onClose: () -> Unit,
    // Keep external callbacks non-suspending inside awaitEachGesture's
    // restricted gesture coroutine.
    onDragFraction: (Float) -> Unit = {},
    onSwipeCancelled: () -> Unit = {},
    canSwipeDown: () -> Boolean = { true },
    swipeFromHeaderOnly: Boolean = false,
    hazeState: HazeState?,
    profile: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the portal anchored to the measured player without inserting
    // a redundant Artist icon into the top-left of the Hero section.
    if (source == null || host == null) return
    val density = LocalDensity.current
    // Do not wait for finger-up after a long drag. A deliberate 64dp
    // downward movement starts closing instantly, while the touch slop
    // and directional check prevent accidental dismissal.
    val minSwipe = with(density) { 64.dp.toPx() }
    val maxHeaderY = topInsetPx + with(density) { 118.dp.toPx() }
    // Audio progress can recompose the player frequently. Keep the pointer
    // detector alive across callback changes so a slow gesture is never lost.
    val closeCallback by rememberUpdatedState(onClose)
    val swipeAllowed by rememberUpdatedState(canSwipeDown)
    val updateDrag by rememberUpdatedState(onDragFraction)
    val restoreAfterDrag by rememberUpdatedState(onSwipeCancelled)
    Box(modifier.pointerInput(open, swipeFromHeaderOnly) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false, pass = PointerEventPass.Initial
            )
            val acceptsStart = open &&
                (!swipeFromHeaderOnly || down.position.y <= maxHeaderY) &&
                swipeAllowed()
            val touchSlop = viewConfiguration.touchSlop
            var deltaX = 0f
            var deltaY = 0f
            var dragCaptured = false
            var dismissRequested = false
            var pointer = down.id
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointer }
                    ?: event.changes.firstOrNull { it.pressed }
                if (change == null) {
                    if (dragCaptured) restoreAfterDrag()
                    break
                }
                if (!change.pressed) {
                    if (dragCaptured) {
                        if (shouldDismissOverlaySwipe(deltaX, deltaY, minSwipe)) {
                            // For very short sampling (touch-up before
                            // another move) the close still happens instantly.
                            closeCallback()
                        } else {
                            // An incomplete gesture springs gently back.
                            restoreAfterDrag()
                        }
                    }
                    break
                }
                val diff = change.position - change.previousPosition
                deltaX += diff.x
                deltaY += diff.y
                if (acceptsStart && !dragCaptured &&
                    deltaY > touchSlop &&
                    deltaY > kotlin.math.abs(deltaX) * 1.25f) {
                    dragCaptured = true
                }
                if (dragCaptured) {
                    change.consume()
                    val fraction = overlayDragRevealFraction(
                        deltaY - touchSlop, size.height.toFloat()
                    )
                    updateDrag(fraction)
                    // Trigger the exit WHILE the finger is dragging, not
                    // after ACTION_UP. Prevent double-close on pointer release.
                    if (!dismissRequested &&
                        shouldDismissOverlaySwipe(deltaX, deltaY, minSwipe)) {
                        dismissRequested = true
                        closeCallback()
                        break
                    }
                }
                pointer = change.id
            }
        }
    }) {
        // Full-screen profile from the very TOP to the very BOTTOM.
        // Previously it was restricted to pillTop and padded down by
        // toolbarHeight, exposing black cut-off bands above and below.
        // Only the presentation transform moves; final measured size is
        // always the full viewport and neither artist rows nor album artwork
        // are cropped to make space for the four-button pill.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val p = progress().coerceIn(0f, 1f)
                    // Keep the hero artwork fully measured. Only a restrained
                    // 14dp lift + gradual reveal; large clipped translations
                    // made the cover look severed as it entered the viewport.
                    translationY = (1f - p) * 14.dp.toPx()
                    alpha = ((p - 0.04f) / 0.94f).coerceIn(0f, 1f)
                    clip = false
                }
                .then(
                    if (hazeState != null) Modifier.hazeSource(hazeState)
                    else Modifier
                )
        ) {
            profile()
        }

        // No duplicate profile bubble in the Artist header. Navigation
        // remains in the bottom pill and swipe-to-close stays available.
    }
}

// Use actual placement coordinates rather than graphics translation:
// tap targets track the pill precisely when it shifts to center.
internal fun artistPillLeftPx(
    artistLeftPx: Float, circlePx: Float, gapPx: Float, progress: Float
): Float {
    val shift = (circlePx + gapPx) * 0.5f * progress.coerceIn(0f, 1f)
    return artistLeftPx + circlePx + gapPx - shift
}

/** Fraction of the open panel remaining while the finger drags down.
 * A 72% screen travel reveals the underlying player completely, while the
 * first frames remain smooth and reversible without a sudden jump. */
internal fun overlayDragRevealFraction(
    distancePx: Float, heightPx: Float
): Float {
    if (!distancePx.isFinite() || !heightPx.isFinite() || heightPx <= 0f) return 1f
    return (1f - distancePx.coerceAtLeast(0f) / (heightPx * 0.72f))
        .coerceIn(0f, 1f)
}

/** Directional, thresholded release avoids interpreting a small list scroll
 * or horizontal finger wobble as a request to close the profile. */
internal fun shouldDismissOverlaySwipe(
    deltaX: Float, deltaY: Float, thresholdPx: Float
): Boolean = deltaY.isFinite() && deltaX.isFinite() &&
    deltaY >= thresholdPx && deltaY > kotlin.math.abs(deltaX) * 1.35f

/** Keep the separate circular Artist button and Library action pill one
 * constant 10dp apart while Home increases the width symmetrically. */
internal fun libraryArtistCircleLeftPx(
    artistStartPx: Float, pillExtraWidthPx: Float
): Float = artistStartPx - pillExtraWidthPx.coerceAtLeast(0f) * 0.5f

internal fun libraryActionPillLeftPx(
    artistStartPx: Float,
    circleWidthPx: Float,
    gapPx: Float,
    pillExtraWidthPx: Float
): Float = libraryArtistCircleLeftPx(artistStartPx, pillExtraWidthPx) +
    circleWidthPx + gapPx

/** Library's independent Artist circle and the pill must share identical
 * fade progress, including finger-driven partial dismissal. */
internal fun libraryArtistCircleOpacity(openProgress: Float): Float =
    if (openProgress.isFinite()) openProgress.coerceIn(0f, 1f) else 0f

/**
 * The exact opacity of the black navigation material. Driven by the same
 * reversible Animatable as the Artist/Library portal, not by a page boolean.
 * This guarantees the last closing frame has zero black overlay before the
 * portal unmounts and restores full-strength Now Playing Liquid Glass.
 */
internal fun solidNavigationReveal(openProgress: Float): Float =
    if (openProgress.isFinite()) openProgress.coerceIn(0f, 1f) else 0f
