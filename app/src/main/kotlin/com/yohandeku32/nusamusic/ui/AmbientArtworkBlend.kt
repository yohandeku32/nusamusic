package com.yohandeku32.nusamusic.ui

import kotlin.math.roundToInt

/**
 * Combine two blurred samples of the SAME album art (not a palette fill).
 * The broad sample carries creamy diffusion; the finer sample preserves faint
 * composition and colour transitions instead of washing everything flat.
 *
 * Pure pixel processing happens once per cover on a background dispatcher.
 * No shader, bitmap or per-frame allocation is required for the blend.
 */
internal fun blendAmbientArtwork(
    broad: IntArray,
    detailed: IntArray
): IntArray {
    require(broad.size == detailed.size)
    val out = IntArray(broad.size)
    for (i in broad.indices) {
        val a = broad[i]
        val b = detailed[i]
        fun channel(shift: Int): Float {
            val smooth = ((a ushr shift) and 255) * 0.64f
            val structure = ((b ushr shift) and 255) * 0.36f
            return smooth + structure
        }
        val red = channel(16)
        val green = channel(8)
        val blue = channel(0)
        // Preserve the original hues with a modest colourfulness lift.
        // The luma anchor keeps whites/neutral covers neutral.
        val luma = red * 0.2126f + green * 0.7152f + blue * 0.0722f
        fun vivid(value: Float): Int =
            (luma + (value - luma) * 1.075f).roundToInt().coerceIn(0, 255)
        val alpha = ((((a ushr 24) and 255) * 0.64f) +
            (((b ushr 24) and 255) * 0.36f)).roundToInt().coerceIn(0, 255)
        out[i] = (alpha shl 24) or
            (vivid(red) shl 16) or
            (vivid(green) shl 8) or
            vivid(blue)
    }
    return out
}
