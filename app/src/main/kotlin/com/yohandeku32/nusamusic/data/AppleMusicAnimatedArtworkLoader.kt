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
    private const val NEGATIVE_CACHE_MS = 30 * 60 * 1000L
    private const val ERROR_CACHE_MS = 2 * 60 * 1000L
    private const val MAX_CACHE_ENTRIES = 300

    private data class CacheEntry(val url: String?, val expiresAtMs: Long)
    private data class JsonResponse(val body: JSONObject? = null, val failed: Boolean = false)
    private val cache = LinkedHashMap<String, CacheEntry>(64, 0.75f, true)

    suspend fun loadAnimatedArtworkUrl(context: Context, song: Song): String? = withContext(Dispatchers.IO) {
        val metadata = artworkLookupMetadata(song, readEmbeddedMetadata(context, song))
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val cached = cache[metadata.cacheKey]
            if (cached != null && cached.expiresAtMs > now) return@withContext cached.url
            if (cached != null) cache.remove(metadata.cacheKey)
        }

        fun finish(url: String?, failed: Boolean = false): String? {
            val duration = if (url != null) POSITIVE_CACHE_MS else if (failed) ERROR_CACHE_MS else NEGATIVE_CACHE_MS
            synchronized(cache) {
                cache[metadata.cacheKey] = CacheEntry(url, System.currentTimeMillis() + duration)
                while (cache.size > MAX_CACHE_ENTRIES) cache.remove(cache.entries.first().key)
            }
            return url
        }

        var catalogFailed = false
        val attemptedAlbums = HashSet<String>()
        if (metadata.canSearchCatalog) {
            val localCountry = Locale.getDefault().country.uppercase(Locale.ROOT)
                .takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "US"
            val countries = listOf(localCountry, "US", "ID").distinct()
            val artists = metadata.artists.take(2)
            val searches = artists.flatMap { artist -> countries.map { it to artist } }.take(4)
            catalog@ for ((country, artist) in searches) {
                currentCoroutineContext().ensureActive()
                val term = artworkSearchTitle(metadata.title) + " " + artist
                val response = readJson(
                    CATALOG_URL + "?term=${Uri.encode(term)}&entity=song&media=music&limit=20&explicit=Yes&country=$country"
                )
                currentCoroutineContext().ensureActive()
                if (response.failed) {
                    catalogFailed = true
                    break
                }
                val candidates = matchingAppleAlbums(metadata, catalogSongs(response.body, country))
                for (candidate in candidates.take(2)) {
                    val albumUrl = appleMusicAlbumUrl(candidate.albumUrl) ?: continue
                    if (!attemptedAlbums.add(albumUrl.substringAfterLast('/'))) continue
                    val artwork = readJson(API_URL + "/url?url=${Uri.encode(albumUrl)}")
                    currentCoroutineContext().ensureActive()
                    if (artwork.failed) return@withContext finish(null, failed = true)
                    val found = animatedUrl(artwork.body)
                    if (found != null) return@withContext finish(found)
                    if (attemptedAlbums.size >= 3) break@catalog
                }
            }
        }

        // Retain album lookup for tracks absent from iTunes, using both track and album artists.
        if (metadata.album.isNotBlank()) {
            val queries = listOf(metadata.album, artworkSearchAlbum(metadata.album)).distinct().flatMap { album ->
                metadata.artists.map { artist -> artist to album }
            }.distinct().take(3)
            for ((artist, album) in queries) {
                currentCoroutineContext().ensureActive()
                val response = readJson(
                    API_URL + "/search?artist=${Uri.encode(artist)}&album=${Uri.encode(album)}" +
                        (if (metadata.title.isNotBlank()) "&title=${Uri.encode(artworkSearchTitle(metadata.title))}" else "")
                )
                currentCoroutineContext().ensureActive()
                if (response.failed) return@withContext finish(null, failed = true)
                val found = animatedUrl(response.body)
                if (found != null) return@withContext finish(found)
            }
        }
        finish(null, failed = catalogFailed)
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

    private fun readJson(endpoint: String): JsonResponse {
        var connection: HttpsURLConnection? = null
        return try {
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
    }

    private fun animatedUrl(json: JSONObject?): String? = sequenceOf("url_tall", "url")
        .mapNotNull { key -> json?.optString(key)?.takeIf { it.isNotBlank() && it != "null" } }
        .firstOrNull(::isAppleArtworkUrl)

    private fun isAppleArtworkUrl(value: String): Boolean = runCatching {
        val parsed = URL(value)
        val host = parsed.host.lowercase(Locale.ROOT)
        parsed.protocol.equals("https", ignoreCase = true) &&
            (host == "apple.com" || host.endsWith(".apple.com") ||
                host == "mzstatic.com" || host.endsWith(".mzstatic.com") ||
                host == "cdn-apple.com" || host.endsWith(".cdn-apple.com"))
    }.getOrDefault(false)
}
