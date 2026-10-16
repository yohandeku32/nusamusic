package com.yohandeku32.nusamusic.ui

/** Population decides the tone; small bright labels must not lift a dark cover. */
internal fun dominantArtworkBackdrop(pixels: IntArray): Int {
    val populations = IntArray(8)
    fun band(pixel: Int): Int {
        val r = (pixel ushr 16) and 255
        val g = (pixel ushr 8) and 255
        val b = pixel and 255
        return ((r * 0.2126 + g * 0.7152 + b * 0.0722) / 32).toInt().coerceIn(0, 7)
    }
    for (pixel in pixels) {
        if (pixel ushr 24 >= 128) populations[band(pixel)]++
    }
    // In a tie, retain the darker group to keep white player controls legible.
    val dominantBand = populations.indices.maxByOrNull { populations[it] } ?: 0
    if (populations[dominantBand] == 0) return 0xFF141414.toInt()
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    var total = 0.0
    for (pixel in pixels) {
        if (pixel ushr 24 < 128 || band(pixel) != dominantBand) continue
        val r = (pixel ushr 16) and 255
        val g = (pixel ushr 8) and 255
        val b = pixel and 255
        // A gentle preference retains colour without letting a tiny accent win.
        val weight = 1.0 + (maxOf(r, g, b) - minOf(r, g, b)) / 255.0 * 0.35
        red += r * weight
        green += g * weight
        blue += b * weight
        total += weight
    }
    fun channel(value: Double) = (value / total * 0.78).toInt().coerceIn(0, 255)
    return (255 shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)
}
