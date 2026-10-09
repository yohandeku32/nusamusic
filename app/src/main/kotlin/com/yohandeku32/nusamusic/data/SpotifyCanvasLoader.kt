package com.yohandeku32.nusamusic.data

import android.net.Uri
import com.yohandeku32.nusamusic.BuildConfig
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
 * Optional Spotify Canvas lookup.
 *
 * The app never sends Spotify session credentials. It only calls the user's separately
 * deployed Canvas backend with the local song title/artist. A missing or unavailable
 * backend deliberately resolves to null so Immersive Artwork keeps using album art.
 */
object SpotifyCanvasLoader {
    private const val POSITIVE_CACHE_MS = 6 * 60 * 60 * 1000L
    private const val NEGATIVE_CACHE_MS = 30 * 60 * 1000L
    private const val ERROR_CACHE_MS = 2 * 60 * 1000L
    private const val MAX_CACHE_ENTRIES = 250

    private data class CacheEntry(
        val url: String?,
        val expiresAtMs: Long
    )

    private val cache = LinkedHashMap<String, CacheEntry>(64, 0.75f, true)

    suspend fun loadCanvasUrl(song: Song): String? = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.CANVAS_API_BASE_URL.trim().trimEnd('/')
        val apiKey = BuildConfig.CANVAS_API_KEY.trim()

        // Do not make a network request until a backend URL and key are configured.
        if (!baseUrl.startsWith("https://", ignoreCase = true) || apiKey.isBlank()) {
            return@withContext null
        }

        val title = song.title.trim()
        val artist = song.artist.trim()
        if (title.isBlank() || artist.isBlank()) return@withContext null

        val cacheKey = normalize(title) + "|" + normalize(artist)
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val cached = cache[cacheKey]
            if (cached != null && cached.expiresAtMs > now) {
                return@withContext cached.url
            }
            if (cached != null) cache.remove(cacheKey)
        }

        var connection: HttpURLConnection? = null
        var foundUrl: String? = null
        var cacheDuration = ERROR_CACHE_MS

        try {
            val endpoint = URL(
                "$baseUrl/api/canvas?title=${Uri.encode(title)}&artist=${Uri.encode(artist)}"
            )
            connection = endpoint.openConnection() as? HttpsURLConnection
                ?: return@withContext null
            connection.connectTimeout = 4_500
            connection.readTimeout = 6_500
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-API-Key", apiKey)

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
                val candidate = JSONObject(payload).optString("canvasUrl")
                foundUrl = candidate.takeIf(::isTrustedCanvasUrl)
                cacheDuration = if (foundUrl != null) POSITIVE_CACHE_MS else NEGATIVE_CACHE_MS
            }
        } catch (_: Exception) {
            // Network, API, parsing and unavailable-Canvas errors all gracefully fall back to art.
            foundUrl = null
        } finally {
            connection?.disconnect()
        }

        synchronized(cache) {
            cache[cacheKey] = CacheEntry(
                url = foundUrl,
                expiresAtMs = System.currentTimeMillis() + cacheDuration
            )
            while (cache.size > MAX_CACHE_ENTRIES) {
                val oldestKey = cache.entries.firstOrNull()?.key ?: break
                cache.remove(oldestKey)
            }
        }

        foundUrl
    }

    private fun isTrustedCanvasUrl(value: String): Boolean {
        return try {
            val parsed = URL(value)
            parsed.protocol.equals("https", ignoreCase = true) &&
                (parsed.host.equals("canvaz.scdn.co", ignoreCase = true) ||
                    parsed.host.endsWith(".scdn.co", ignoreCase = true))
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
