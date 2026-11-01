package com.yohandeku32.nusamusic.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.yohandeku32.nusamusic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal val LocalVinylRecordTexture = staticCompositionLocalOf<ImageBitmap?> { null }

internal data class StudioBlackRasterAssets(
    val machine: ImageBitmap,
    val record: ImageBitmap,
    val foreground: ImageBitmap
)

/** One bounded, shared decomposition of the original concept render; never repeated per frame. */
internal object StudioBlackAssetLoader {
    private val mutex = Mutex()
    @Volatile private var cached: StudioBlackRasterAssets? = null
    fun peek(): StudioBlackRasterAssets? = cached

    suspend fun load(context: Context): StudioBlackRasterAssets = withContext(Dispatchers.Default) {
        cached ?: mutex.withLock {
            cached ?: decode(context.applicationContext).also { cached = it }
        }
    }

    @Suppress("DEPRECATION")
    private fun decode(context: Context): StudioBlackRasterAssets {
        val crop = context.resources.openRawResource(R.raw.studio_black_reference).use { stream ->
            val decoder = requireNotNull(BitmapRegionDecoder.newInstance(stream, false))
            try {
                requireNotNull(decoder.decodeRegion(Rect(527, 90, 1026, 472),
                    BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
            } finally { decoder.recycle() }
        }
        val width = crop.width
        val height = crop.height
        val source = IntArray(width * height)
        crop.getPixels(source, 0, width, 0, 0, width, height)
        crop.recycle()
        val cx = 218.5f
        val cy = 148f
        val rx = 171f
        val ry = 141f
        val perspective = -0.102f

        fun sample(x: Float, y: Float): Int {
            val x0 = x.toInt().coerceIn(0, width - 2)
            val y0 = y.toInt().coerceIn(0, height - 2)
            val dx = (x - x0).coerceIn(0f, 1f)
            val dy = (y - y0).coerceIn(0f, 1f)
            val a = source[y0 * width + x0]
            val b = source[y0 * width + x0 + 1]
            val c = source[(y0 + 1) * width + x0]
            val d = source[(y0 + 1) * width + x0 + 1]
            fun channel(shift: Int): Int {
                val upper = ((a ushr shift) and 255) * (1 - dx) + ((b ushr shift) and 255) * dx
                val lower = ((c ushr shift) and 255) * (1 - dx) + ((d ushr shift) and 255) * dx
                return (upper * (1 - dy) + lower * dy).roundToInt().coerceIn(0, 255)
            }
            return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }

        // Recover the original tonearm pixels over the moving record. Outside the record,
        // the unchanged reference machine already contains the complete arm assembly.
        val metal = BooleanArray(source.size)
        val pin = BooleanArray(source.size)
        for (y in 0 until height) for (x in 0 until width) {
            val v = (y - cy) / (ry - perspective * (y - cy))
            val u = (x - cx) * (1 + perspective * v) / rx
            val color = source[y * width + x]
            val r = color ushr 16 and 255
            val g = color ushr 8 and 255
            val b = color and 255
            val arm = u > 0.42f && v > -0.55f && u * u + v * v < 1.08f &&
                max(r, max(g, b)) > 60 && max(r, max(g, b)) - min(r, min(g, b)) < 35
            val spindle = abs(x - cx) < 6f && y in 136..157 &&
                max(r, max(g, b)) - min(r, min(g, b)) < 24 && max(r, max(g, b)) > 35
            metal[y * width + x] = arm
            pin[y * width + x] = spindle
        }
        val foreground = IntArray(source.size)
        val removal = BooleanArray(source.size)
        for (y in 0 until height) for (x in 0 until width) {
            var nearMetal = false
            for (dy in -2..2) for (dx in -2..2) {
                val xx = (x + dx).coerceIn(0, width - 1)
                val yy = (y + dy).coerceIn(0, height - 1)
                if (metal[yy * width + xx]) nearMetal = true
            }
            if (nearMetal || pin[y * width + x]) foreground[y * width + x] = source[y * width + x]
            removal[y * width + x] = nearMetal
        }
        val textureSize = 512
        val record = IntArray(textureSize * textureSize)
        for (y in 0 until textureSize) for (x in 0 until textureSize) {
            val u = (x + 0.5f - textureSize / 2f) / (textureSize / 2f)
            val v = (y + 0.5f - textureSize / 2f) / (textureSize / 2f)
            val distance = u * u + v * v
            if (distance > 1f) continue
            if (distance < 0.34f * 0.34f) {
                record[y * textureSize + x] = 0xFF101112.toInt()
                continue
            }
            val denominator = 1 + perspective * v
            val sx = cx + rx * u / denominator
            val sy = cy + ry * v / denominator
            val index = sy.toInt().coerceIn(0, height - 1) * width + sx.toInt().coerceIn(0, width - 1)
            record[y * textureSize + x] = if (removal[index] && u > 0.4f)
                sample(cx - rx * u / denominator, sy) else sample(sx, sy)
        }

        // Color-to-alpha removes the warm paper from the original sheet without redrawing
        // the chassis. On VinylPaper the photograph's material detail is retained.
        val machine = IntArray(source.size)
        for (index in source.indices) {
            val x = index % width
            val y = index / width
            val color = source[index]
            val r = color ushr 16 and 255
            val g = color ushr 8 and 255
            val b = color and 255
            val tx = x.toFloat() / (width - 1)
            val ty = y.toFloat() / (height - 1)
            fun paper(shift: Int): Float {
                fun channel(pixel: Int) = (pixel ushr shift and 255).toFloat()
                val horizontal = channel(source[y * width]) * (1 - tx) + channel(source[y * width + width - 1]) * tx
                val vertical = channel(source[x]) * (1 - ty) + channel(source[(height - 1) * width + x]) * ty
                val corners = (channel(source[0]) * (1 - tx) + channel(source[width - 1]) * tx) * (1 - ty) +
                    (channel(source[(height - 1) * width]) * (1 - tx) + channel(source.last()) * tx) * ty
                return (horizontal + vertical - corners).coerceIn(1f, 255f)
            }
            val leftEdge = if (y < 310) 32f - (y - 12) * 0.078f else 8f + (y - 310) * 0.25f
            val rightEdge = if (y < 310) 447f + (y - 12) * 0.12f else 483f - (y - 310) * 0.35f
            if (y in 12..363 && x > leftEdge && x < rightEdge && max(r, max(g, b)) < 225) {
                machine[index] = color
                continue
            }
            val pr = paper(16)
            val pg = paper(8)
            val pb = paper(0)
            val opacity = (1f - min(r / pr, min(g / pg, b / pb))).coerceIn(0f, 1f)
            if (opacity < 0.035f) continue
            fun matte(channel: Int, paper: Float) = ((channel - (1 - opacity) * paper) / opacity)
                .roundToInt().coerceIn(0, 255)
            machine[index] = ((opacity * 255).roundToInt() shl 24) or
                (matte(r, pr) shl 16) or (matte(g, pg) shl 8) or matte(b, pb)
        }
        return StudioBlackRasterAssets(
            Bitmap.createBitmap(machine, width, height, Bitmap.Config.ARGB_8888).asImageBitmap(),
            Bitmap.createBitmap(record, textureSize, textureSize, Bitmap.Config.ARGB_8888).asImageBitmap(),
            Bitmap.createBitmap(foreground, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
        )
    }
}
