package com.yohandeku32.nusamusic.ui

/** Three separable box passes approximate a Gaussian blur with clamped edges. */
internal fun blurArgb(
    pixels: IntArray,
    width: Int,
    height: Int,
    radius: Int = 12,
    passes: Int = 3
): IntArray {
    require(width > 0 && height > 0 && pixels.size == width * height)
    val result = pixels.copyOf()
    if (radius <= 0 || passes <= 0) return result
    val scratch = IntArray(pixels.size)
    val effectiveRadius = radius.coerceAtMost(maxOf(width, height))
    repeat(passes) {
        blurAxis(result, scratch, width, height, effectiveRadius, horizontal = true)
        blurAxis(scratch, result, width, height, effectiveRadius, horizontal = false)
    }
    return result
}

private fun blurAxis(
    source: IntArray,
    destination: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    horizontal: Boolean
) {
    val length = if (horizontal) width else height
    val lines = if (horizontal) height else width
    val divisor = radius * 2 + 1
    for (line in 0 until lines) {
        fun index(position: Int): Int =
            if (horizontal) line * width + position else position * width + line

        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        for (offset in -radius..radius) {
            val colour = source[index(offset.coerceIn(0, length - 1))]
            alpha += colour ushr 24
            red += (colour ushr 16) and 255
            green += (colour ushr 8) and 255
            blue += colour and 255
        }
        for (position in 0 until length) {
            destination[index(position)] = ((alpha / divisor) shl 24) or
                ((red / divisor) shl 16) or ((green / divisor) shl 8) or (blue / divisor)
            val outgoing = source[index((position - radius).coerceIn(0, length - 1))]
            val incoming = source[index((position + radius + 1).coerceIn(0, length - 1))]
            alpha += (incoming ushr 24) - (outgoing ushr 24)
            red += ((incoming ushr 16) and 255) - ((outgoing ushr 16) and 255)
            green += ((incoming ushr 8) and 255) - ((outgoing ushr 8) and 255)
            blue += (incoming and 255) - (outgoing and 255)
        }
    }
}
