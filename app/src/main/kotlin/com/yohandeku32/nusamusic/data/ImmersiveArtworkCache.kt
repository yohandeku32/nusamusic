package com.yohandeku32.nusamusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

internal data class ImmersiveArtworkSnapshot(val cover: Bitmap, val blur: Bitmap, val backdrop: Int)

internal object ImmersiveArtworkCache {
    private val diskMutex = Mutex()
    private val memory = object : androidx.collection.LruCache<String, ImmersiveArtworkSnapshot>(16 * 1024) {
        override fun sizeOf(key: String, value: ImmersiveArtworkSnapshot) =
            (value.cover.byteCount + value.blur.byteCount) / 1024
    }
    private fun key(song: Song) = if (song.albumId > 0L) "album:${song.albumId}" else "uri:${song.uri}"
    private fun disk(context: Context) = ImmersiveArtworkDiskCache(File(context.filesDir, "immersive_launch_preview.zip"))

    fun cached(song: Song): ImmersiveArtworkSnapshot? = memory.get(key(song))

    suspend fun restore(context: Context, song: Song): ImmersiveArtworkSnapshot? = withContext(Dispatchers.IO) {
        cached(song)?.let { return@withContext it }
        diskMutex.withLock {
            cached(song)?.let { return@withLock it }
            val bytes = disk(context).read(key(song)) ?: return@withLock null
            val cover = decode(bytes.cover, 768)
            val blur = decode(bytes.blur, 320)
            if (cover == null || blur == null) {
                disk(context).clear()
                return@withLock null
            }
            ImmersiveArtworkSnapshot(cover, blur, bytes.backdrop).also { memory.put(key(song), it) }
        }
    }

    suspend fun save(context: Context, song: Song, value: ImmersiveArtworkSnapshot) {
        memory.put(key(song), value)
        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                // Avoid recompressing the same album on every reopening or swipe.
                if (disk(context).read(key(song)) != null) return@withLock
                val scale = (768f / maxOf(value.cover.width, value.cover.height)).coerceAtMost(1f)
                val preview = Bitmap.createScaledBitmap(value.cover,
                    (value.cover.width * scale).toInt().coerceAtLeast(1),
                    (value.cover.height * scale).toInt().coerceAtLeast(1), true)
                try {
                    val cover = ByteArrayOutputStream().also { preview.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
                    val blur = ByteArrayOutputStream().also { value.blur.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
                    disk(context).write(ImmersiveArtworkBytes(key(song), cover, blur, value.backdrop))
                } finally {
                    if (preview !== value.cover) preview.recycle()
                }
            }
        }
    }

    private fun decode(bytes: ByteArray, maxSize: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..maxSize || bounds.outHeight !in 1..maxSize) return null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
