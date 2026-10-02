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
    private val cache = object : LruCache<String, Bitmap>(12) {}

    suspend fun load(context: Context, uriString: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(uriString)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uriString))
            val bytes = retriever.embeddedPicture ?: return@withContext null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val scaled = scaleDown(bitmap, 900)
            cache.put(uriString, scaled)
            scaled
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
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
