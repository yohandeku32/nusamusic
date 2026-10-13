package com.yohandeku32.nusamusic.data

import android.net.Uri
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * Looks up Apple Music animated album artwork through the community-maintained
 * m8tec Apple Music Animated Artworks API.
 *
 * This is not an official Apple API. Missing artwork, network failures, or rate
 * limiting resolve to null, leaving the local album cover as the visual fallback.
 */
object AppleMusicAnimatedArtworkLoader {
    private const val API_URL = "https://artwork.m8tec.top/api/v1/artwork/search"
    private const val POSITIVE_CACHE_MS = 12 * 60 * 60 * 1000L
    private const val NEGATIVE_CACHE_MS = 30 * 60 * 1000L
    private const val ERROR_CACHE_MS = 2 * 60 * 1000L
    private const val MAX_CACHE_ENTRIES = 300

    private data class CacheEntry(
        val url: String?,
        val expiresAtMs: Long
    )

    private val cache = LinkedHashMap<String, CacheEntry>(64, 0.75f, true)

    suspend fun loadAnimatedArtworkUrl(song: Song): String? = withContext(Dispatchers.IO) {
        val artist = song.artist.trim()
        val album = song.album.trim()
        val title = song.title.trim()
        if (artist.isBlank() || album.isBlank()) return@withContext null

        val cacheKey = normalize(artist) + "|" + normalize(album)
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val cached = cache[cacheKey]
            if (cached != null && cached.expiresAtMs > now) return@withContext cached.url
            if (cached != null) cache.remove(cacheKey)
        }

        var connection: HttpURLConnection? = null
        var foundUrl: String? = null
        var cacheDuration = ERROR_CACHE_MS

        try {
            val endpoint = URL(
                API_URL +
                    "?artist=${Uri.encode(artist)}" +
                    "&album=${Uri.encode(album)}" +
                    (if (title.isNotBlank()) "&title=${Uri.encode(title)}" else "")
            )
            val httpsConnection = endpoint.openConnection() as? HttpsURLConnection
                ?: return@withContext null
            connection = httpsConnection
            httpsConnection.connectTimeout = 4_500
            httpsConnection.readTimeout = 7_000
            httpsConnection.requestMethod = "GET"
            httpsConnection.setRequestProperty("Accept", "application/json")
            httpsConnection.setRequestProperty("User-Agent", "NusaMusic/AppleAnimatedArtwork")

            when (httpsConnection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val payload = httpsConnection.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    }
                    val json = JSONObject(payload)
                    // The taller stream is better suited to Immersive Artwork; use the square
                    // variant when Apple Music only has that version for the album.
                    val tall = json.optString("url_tall").takeIf { it.isNotBlank() && it != "null" }
                    val square = json.optString("url").takeIf { it.isNotBlank() && it != "null" }
                    foundUrl = sequenceOf(tall, square)
                        .filterNotNull()
                        .firstOrNull(::isAppleArtworkUrl)
                    cacheDuration = if (foundUrl != null) POSITIVE_CACHE_MS else NEGATIVE_CACHE_MS
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    cacheDuration = NEGATIVE_CACHE_MS
                }
                else -> {
                    cacheDuration = ERROR_CACHE_MS
                }
            }
        } catch (_: Exception) {
            // Unavailable service, parsing error and Apple rate-limit errors use static artwork.
            foundUrl = null
            cacheDuration = ERROR_CACHE_MS
        } finally {
            connection?.disconnect()
        }

        synchronized(cache) {
            cache[cacheKey] = CacheEntry(foundUrl, System.currentTimeMillis() + cacheDuration)
            while (cache.size > MAX_CACHE_ENTRIES) {
                val oldestKey = cache.entries.firstOrNull()?.key ?: break
                cache.remove(oldestKey)
            }
        }
        foundUrl
    }

    private fun isAppleArtworkUrl(value: String): Boolean {
        return try {
            val parsed = URL(value)
            val host = parsed.host.lowercase(Locale.ROOT)
            parsed.protocol.equals("https", ignoreCase = true) &&
                (host == "apple.com" || host.endsWith(".apple.com") ||
                    host == "mzstatic.com" || host.endsWith(".mzstatic.com") ||
                    host == "cdn-apple.com" || host.endsWith(".cdn-apple.com"))
        } catch (_: Exception) {
            false
        }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
