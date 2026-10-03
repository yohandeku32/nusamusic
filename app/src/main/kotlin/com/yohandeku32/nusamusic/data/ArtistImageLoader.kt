package com.yohandeku32.nusamusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Resolves an artist name to a portrait using the public Deezer artist search
 * endpoint, then keeps a local copy so repeated playback does not require a
 * network request.
 *
 * Resolution strategy:
 * 1. Memory cache
 * 2. Persistent file cache
 * 3. Deezer artist search with an exact normalized-name match
 * 4. Download the artist portrait and persist it
 */
object ArtistImageLoader {
    private const val MAX_DOWNLOAD_BYTES = 4 * 1024 * 1024

    private val memoryCache = object :
        androidx.collection.LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    suspend fun load(
        context: Context,
        artistName: String?,
        maxSize: Int = 256
    ): Bitmap? = withContext(Dispatchers.IO) {
        val artist = artistName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return@withContext null

        val queryArtist = ArtistNameUtils.firstArtist(artist)
        if (queryArtist.isBlank()) return@withContext null

        val cacheKey = normalize(queryArtist)
        val memoryKey = "${cacheKey}@${maxSize}"

        memoryCache.get(memoryKey)?.let { return@withContext it }

        val cacheFile = cacheFile(context, cacheKey)
        decodeSampled(cacheFile, maxSize)?.let {
            memoryCache.put(memoryKey, it)
            return@withContext it
        }

        val imageUrl = searchDeezerArtistImage(queryArtist) ?: return@withContext null
        val saved = downloadImage(imageUrl, cacheFile) ?: return@withContext null

        decodeSampled(saved, maxSize)?.let {
            memoryCache.put(memoryKey, it)
            return@withContext it
        }

        null
    }

    private fun normalize(value: String): String {
        return java.text.Normalizer
            .normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    private fun cacheFile(context: Context, artistKey: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(artistKey.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val directory = File(context.filesDir, "artist_art").apply {
            if (!exists()) mkdirs()
        }

        return File(directory, "${digest}.jpg")
    }

    private fun searchDeezerArtistImage(artist: String): String? {
        val encoded = URLEncoder.encode(artist, StandardCharsets.UTF_8.toString())
        val endpoint =
            "https://api.deezer.com/search/artist?q=${encoded}&limit=5"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NusaMusic/1.0")
        }

        return try {
            if (connection.responseCode !in 200..299) return null

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(response).optJSONArray("data") ?: return null
            val target = normalize(artist)

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val name = item.optString("name").trim()
                if (normalize(name) == target) {
                    val image = item.optString("picture_big").trim()
                    if (image.isNotBlank()) return image
                }
            }

            null
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadImage(urlString: String, destination: File): File? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NusaMusic/1.0")
        }

        return try {
            if (connection.responseCode !in 200..299) return null

            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_DOWNLOAD_BYTES) return null

            val temp = File(destination.parentFile, "${destination.name}.tmp")
            var total = 0L

            connection.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break

                        total += count
                        if (total > MAX_DOWNLOAD_BYTES) {
                            temp.delete()
                            return null
                        }

                        output.write(buffer, 0, count)
                    }
                }
            }

            if (!temp.renameTo(destination)) {
                temp.delete()
                return null
            }

            destination
        } catch (_: Exception) {
            File(destination.parentFile, "${destination.name}.tmp").delete()
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun decodeSampled(file: File, maxSize: Int): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, bounds)

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                file.delete()
                return null
            }

            val sample = calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                maxSize
            )

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxSize: Int
    ): Int {
        var sample = 1
        var currentWidth = width
        var currentHeight = height

        while (
            currentWidth / 2 >= maxSize &&
            currentHeight / 2 >= maxSize
        ) {
            currentWidth /= 2
            currentHeight /= 2
            sample *= 2
        }

        return sample.coerceAtLeast(1)
    }
}
