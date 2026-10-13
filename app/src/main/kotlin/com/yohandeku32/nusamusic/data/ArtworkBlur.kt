package com.yohandeku32.nusamusic.data

import android.graphics.Bitmap
import com.yohandeku32.nusamusic.ui.blurArgb
import kotlin.math.roundToInt

internal object ArtworkBlur {
    private const val THUMBNAIL_SIZE = 128

    // The caller runs this on Dispatchers.Default, never on the UI thread.
    fun create(source: Bitmap): Bitmap {
        val scale = THUMBNAIL_SIZE.toFloat() / maxOf(source.width, source.height)
        val width = (source.width * scale.coerceAtMost(1f)).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale.coerceAtMost(1f)).roundToInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(source, width, height, true)
        try {
            val pixels = IntArray(width * height)
            small.getPixels(pixels, 0, width, 0, 0, width, height)
            return Bitmap.createBitmap(blurArgb(pixels, width, height), width, height, Bitmap.Config.ARGB_8888)
        } finally {
            if (small !== source) small.recycle()
        }
    }
}
