package com.yohandeku32.nusamusic.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/** Matches the playing file to an Apple album, then requests its animated artwork from m8tec. */
object AppleMusicAnimatedArtworkLoader {
    private const val API_URL = "https://artwork.m8tec.top/api/v1/artwork"
    private const val CATALOG_URL = "https://itunes.apple.com/search"
    private const val POSITIVE_CACHE_MS = 12 * 60 * 60 * 1000L
    private const val NEGATIVE_CACHE_MS = 60 * 1000L
    private const val ERROR_CACHE_MS = 5 * 1000L
    private const val MAX_CACHE_ENTRIES = 300

    private data class CacheEntry(val url: String?, val expiresAtMs: Long, val alternatives: List<String> = emptyList())
    private data class JsonResponse(val body: JSONObject? = null, val failed: Boolean = false)
    private val cache = LinkedHashMap<String, CacheEntry>(64, 0.75f, true)

    suspend fun loadAnimatedArtworkUrl(context: Context, song: Song, forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val metadata = artworkLookupMetadata(song, readEmbeddedMetadata(context, song))
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val cached = cache[metadata.cacheKey]
            if (!forceRefresh && cached != null && cached.expiresAtMs > now) return@withContext cached.url
            if (cached != null && cached.expiresAtMs <= now) cache.remove(metadata.cacheKey)
        }

        fun finish(url: String?, failed: Boolean = false, alternatives: List<String> = emptyList()): String? {
            val duration = if (url != null) POSITIVE_CACHE_MS else if (failed) ERROR_CACHE_MS else NEGATIVE_CACHE_MS
            var resolvedUrl = url
            synchronized(cache) {
                val previous = cache[metadata.cacheKey]
                if (url == null && previous?.url != null && previous.expiresAtMs > System.currentTimeMillis()) {
                    // A temporary lookup failure must not replace a known working stream.
                    resolvedUrl = previous.url
                } else cache[metadata.cacheKey] = CacheEntry(url, System.currentTimeMillis() + duration, alternatives)
                while (cache.size > MAX_CACHE_ENTRIES) cache.remove(cache.entries.first().key)
            }
            return resolvedUrl
        }

        val result = resolveAnimatedArtwork(
            metadata = metadata,
            libraryMetadata = artworkLookupMetadata(song, null),
            country = Locale.getDefault().country.uppercase(Locale.ROOT)
                .takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "US",
            searchAlbum = { query ->
                requestArtwork(
                    API_URL + "/search?artist=${Uri.encode(query.artist)}&album=${Uri.encode(query.album)}" +
                        (if (query.title.isNotBlank()) "&title=${Uri.encode(query.title)}" else "")
                )
            },
            searchCatalog = { term, country ->
                val response = readJson(
                    CATALOG_URL + "?term=${Uri.encode(term)}&entity=song&media=music&limit=20&explicit=Yes&country=$country"
                )
                CatalogReply(catalogSongs(response.body, country), response.failed)
            },
            loadAlbumUrl = { url -> requestArtwork(API_URL + "/url?url=${Uri.encode(url)}") }
        )
        finish(result.url, result.failed, result.alternatives)
    }

    // Album-detail hero uses Apple's SQUARE animated cover, unlike the tall
    // player artwork. Reuse m8tec's existing open community endpoint without
    // opening a local audio file or searching per track.
    private val albumSquareCache = LinkedHashMap<String, CacheEntry>(32, 0.75f, true)

    suspend fun loadAlbumSquareArtworkUrl(artist: String, album: String): String? =
        withContext(Dispatchers.IO) {
            val artistName = ArtistNameUtils.firstArtist(artist).trim()
            val albumName = album.trim()
            if (artistName.length < 2 || albumName.length < 2 ||
                albumName.equals("unknown album", true)) return@withContext null
            val key = (artistName + "\u0001" + albumName).lowercase(Locale.ROOT)
            val now = System.currentTimeMillis()
            synchronized(albumSquareCache) {
                albumSquareCache[key]?.takeIf { it.expiresAtMs > now }?.let {
                    return@withContext it.url
                }
            }
            val response = readJson(
                API_URL + "/search?artist=${Uri.encode(artistName)}&album=${Uri.encode(albumName)}"
            )
            val url = response.body?.optString("url")?.takeIf { isAppleArtworkUrl(it) }
            val ttl = if (url != null) POSITIVE_CACHE_MS
                else if (response.failed) ERROR_CACHE_MS else NEGATIVE_CACHE_MS
            synchronized(albumSquareCache) {
                albumSquareCache[key] = CacheEntry(url, now + ttl)
                while (albumSquareCache.size > 128) albumSquareCache.remove(albumSquareCache.keys.first())
            }
            url
        }

    fun nextPlaybackUrl(failedUrl: String): String? = synchronized(cache) {
        var nextUrl: String? = null
        val entries = cache.entries.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            if (entry.value.url != failedUrl) continue
            val remaining = ArtworkReply(entry.value.url, alternatives = entry.value.alternatives)
                .afterPlaybackFailure(failedUrl)
            if (remaining.url != null) {
                entry.setValue(CacheEntry(remaining.url, entry.value.expiresAtMs, remaining.alternatives))
                if (nextUrl == null) nextUrl = remaining.url
            } else entries.remove()
        }
        nextUrl
    }

    private suspend fun requestArtwork(endpoint: String): ArtworkReply {
        val response = readJson(endpoint)
        val urls = animatedUrls(response.body)
        return ArtworkReply(urls.firstOrNull(), response.failed, urls.drop(1))
    }

    private fun readEmbeddedMetadata(context: Context, song: Song): ArtworkLookupMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(song.uri))
            ArtworkLookupMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty(),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty(),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            )
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun catalogSongs(json: JSONObject?, country: String): List<AppleCatalogSong> {
        val results = json?.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { index ->
            val result = results.optJSONObject(index) ?: return@mapNotNull null
            if (result.optString("kind") != "song") return@mapNotNull null
            val albumId = result.optLong("collectionId")
            val albumUrl = appleMusicAlbumUrl(result.optString("collectionViewUrl"))
                ?: albumId.takeIf { it > 0 }?.let { "https://music.apple.com/$country/album/$it" }
                ?: return@mapNotNull null
            AppleCatalogSong(
                title = result.optString("trackName"),
                artist = result.optString("artistName"),
                album = result.optString("collectionName"),
                albumUrl = albumUrl,
                durationMs = result.optLong("trackTimeMillis")
            )
        }
    }

    private suspend fun readJson(endpoint: String): JsonResponse {
        currentCoroutineContext().ensureActive()
        var connection: HttpsURLConnection? = null
        val response = try {
            connection = URL(endpoint).openConnection() as HttpsURLConnection
            connection.connectTimeout = 4_500
            connection.readTimeout = 7_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "NusaMusic/AppleAnimatedArtwork")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> JsonResponse(
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                )
                HttpURLConnection.HTTP_NOT_FOUND -> JsonResponse()
                else -> JsonResponse(failed = true)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            JsonResponse(failed = true)
        } finally {
            connection?.disconnect()
        }
        currentCoroutineContext().ensureActive()
        return response
    }

    // The service returns url_tall (portrait) and url (square) independently.
    // Never fall back to its square URL: the full-height Immersive player must
    // display genuine Tall artwork, not a zoomed/cropped square video.
    private fun animatedUrls(json: JSONObject?): List<String> =
        preferredAnimatedArtworkUrls(json?.optString("url_tall"))
            .filter(::isAppleArtworkUrl)
    private fun isAppleArtworkUrl(value: String): Boolean = runCatching {
        val parsed = URL(value)
        val host = parsed.host.lowercase(Locale.ROOT)
        parsed.protocol.equals("https", ignoreCase = true) &&
            (host == "apple.com" || host.endsWith(".apple.com") ||
                host == "mzstatic.com" || host.endsWith(".mzstatic.com") ||
                host == "cdn-apple.com" || host.endsWith(".cdn-apple.com"))
    }.getOrDefault(false)
}
