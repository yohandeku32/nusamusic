package com.yohandeku32.nusamusic.data

import android.graphics.Bitmap
import android.view.TextureView
import androidx.media3.ui.PlayerView
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
            return Bitmap.createBitmap(blurArgb(pixels, width, height, radius = 16), width, height, Bitmap.Config.ARGB_8888)
        } finally {
            if (small !== source) small.recycle()
        }
    }

    /** Capture the visible zoomed viewport, rather than the full oversized video surface. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun captureViewport(playerView: PlayerView): Bitmap? {
        val surface = playerView.videoSurfaceView as? TextureView ?: return null
        if (!surface.isAvailable || surface.width <= 0 || surface.height <= 0 ||
            playerView.width <= 0 || playerView.height <= 0
        ) return null
        val scale = THUMBNAIL_SIZE.toFloat() / maxOf(surface.width, surface.height)
        val width = (surface.width * scale).roundToInt().coerceAtLeast(1)
        val height = (surface.height * scale).roundToInt().coerceAtLeast(1)
        val frame = try {
            surface.getBitmap(width, height) ?: return null
        } catch (_: RuntimeException) {
            return null
        }
        try {
            val surfacePosition = IntArray(2)
            val viewportPosition = IntArray(2)
            surface.getLocationInWindow(surfacePosition)
            playerView.getLocationInWindow(viewportPosition)
            val left = ((viewportPosition[0] - surfacePosition[0]) * width.toFloat() / surface.width)
                .roundToInt().coerceIn(0, width - 1)
            val top = ((viewportPosition[1] - surfacePosition[1]) * height.toFloat() / surface.height)
                .roundToInt().coerceIn(0, height - 1)
            val cropWidth = (playerView.width * width.toFloat() / surface.width)
                .roundToInt().coerceIn(1, width - left)
            val cropHeight = (playerView.height * height.toFloat() / surface.height)
                .roundToInt().coerceIn(1, height - top)
            val cropped = Bitmap.createBitmap(frame, left, top, cropWidth, cropHeight)
            if (cropped !== frame) frame.recycle()
            return cropped
        } catch (_: RuntimeException) {
            frame.recycle()
            return null
        }
    }
}
