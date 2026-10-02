package com.yohandeku32.nusamusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkLoader {
    // Keep a bounded memory cache instead of retaining dozens of large album covers.
    private val cache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    suspend fun load(
        context: Context,
        uriString: String,
        maxSize: Int = 512
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$uriString@$maxSize"
        cache.get(cacheKey)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uriString))
            val bytes = retriever.embeddedPicture ?: return@withContext null

            // Read image bounds first so a large 2000px+ embedded cover is never
            // decoded at full resolution when the UI only needs a small thumbnail.
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            val sample = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxSize = maxSize
            )

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val decoded = BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                options
            ) ?: return@withContext null

            val bitmap = scaleDown(decoded, maxSize)
            if (bitmap !== decoded) {
                decoded.recycle()
            }

            cache.put(cacheKey, bitmap)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxSize: Int
    ): Int {
        if (width <= 0 || height <= 0) return 1

        var sample = 1
        val largest = maxOf(width, height)
        while (largest / (sample * 2) >= maxSize) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(source: Bitmap, maxSize: Int): Bitmap {
        val largest = maxOf(source.width, source.height)
        if (largest <= maxSize) return source

        val ratio = maxSize.toFloat() / largest.toFloat()
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
