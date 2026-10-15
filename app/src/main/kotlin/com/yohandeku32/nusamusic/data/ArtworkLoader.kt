package com.yohandeku32.nusamusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object ArtworkLoader {
    // Limit simultaneous MediaMetadataRetriever and bitmap decodes while
    // flinging large libraries or rapidly shuffling tracks.
    private val decodePermit = Semaphore(2)
    // Many local files have no embedded artwork. Remember that result so
    // scrolling past them again never reopens a metadata retriever.
    private val noArtworkCache = LruCache<String, Boolean>(512)

    // Keep a bounded memory cache instead of retaining dozens of large album covers.
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    // The CD player, its carousel and the library request different image
    // sizes. Reuse an already decoded album cover immediately instead of
    // showing the placeholder and reopening the same audio file.
    private val commonSizes = intArrayOf(128, 160, 256, 512, 768)

    fun cached(
        uriString: String,
        maxSize: Int = 512,
        cacheKey: String? = null
    ): Bitmap? {
        val identity = cacheKey ?: uriString
        cache.get("$identity@$maxSize")?.let { return it }
        // Prefer images at least as sharp as requested, then a smaller
        // cached preview while the correct size is decoded asynchronously.
        for (size in commonSizes) {
            if (size >= maxSize && size != maxSize) {
                cache.get("$identity@$size")?.let { return it }
            }
        }
        for (index in commonSizes.indices.reversed()) {
            val size = commonSizes[index]
            if (size < maxSize) {
                cache.get("$identity@$size")?.let { return it }
            }
        }
        return null
    }

    private fun cachedLarger(
        uriString: String,
        maxSize: Int,
        cacheKey: String?
    ): Bitmap? {
        val identity = cacheKey ?: uriString
        for (size in commonSizes) {
            if (size > maxSize) {
                cache.get("$identity@$size")?.let { return it }
            }
        }
        return null
    }

    suspend fun load(
        context: Context,
        uriString: String,
        maxSize: Int = 512,
        cacheKey: String? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        val memoryKey = resolvedCacheKey(uriString, maxSize, cacheKey)
        cache.get(memoryKey)?.let { return@withContext it }

        // Downsample an existing larger cover on a background thread instead
        // of decoding embedded art and creating a MediaMetadataRetriever again.
        cachedLarger(uriString, maxSize, cacheKey)?.let { larger ->
            val thumbnail = scaleDown(larger, maxSize)
            cache.put(memoryKey, thumbnail)
            return@withContext thumbnail
        }
        if (noArtworkCache.get(memoryKey) == true) return@withContext null

        decodePermit.withPermit {
            cache.get(memoryKey)?.let { return@withPermit it }
            cachedLarger(uriString, maxSize, cacheKey)?.let { larger ->
                val thumbnail = scaleDown(larger, maxSize)
                cache.put(memoryKey, thumbnail)
                return@withPermit thumbnail
            }
            if (noArtworkCache.get(memoryKey) == true) return@withPermit null
            val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uriString))
            val bytes = retriever.embeddedPicture ?: run {
                noArtworkCache.put(memoryKey, true)
                return@withPermit null
            }

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
            ) ?: run {
                noArtworkCache.put(memoryKey, true)
                return@withPermit null
            }

            val bitmap = scaleDown(decoded, maxSize)
            if (bitmap !== decoded) {
                decoded.recycle()
            }

            cache.put(memoryKey, bitmap)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
        }
    }

    private fun resolvedCacheKey(uriString: String, maxSize: Int, cacheKey: String?): String {
        return (cacheKey ?: uriString) + "@" + maxSize
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
