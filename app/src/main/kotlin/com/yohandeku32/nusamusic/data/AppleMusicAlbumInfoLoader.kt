package com.yohandeku32.nusamusic.data

import android.net.Uri
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale

/** Public Apple/iTunes catalog metadata; local playback is never replaced. */
internal data class AppleMusicAlbumInfo(
    val name: String,
    val artist: String,
    val releaseDate: String?,
    val genre: String?,
    val trackCount: Int?,
    val copyright: String?,
    val appleMusicUrl: String?
)

/**
 * Based on the public iTunes Search API and the editorial-notes extraction
 * approach in github.com/navidrome/apple-music-plugin (album.go).
 *
 * This is intentionally not Apple's authenticated MusicKit catalog endpoint.
 * Notes are optional; never invent a review when Apple has no editorial copy.
 */
internal object AppleMusicAlbumInfoLoader {
    private const val TTL_MS = 12L * 60 * 60 * 1000
    private const val MISS_MS = 20L * 60 * 1000
    private const val MAX_JSON_BYTES = 400_000
    private const val MAX_PAGE_BYTES = 1_800_000

    private data class Cached<T>(val value: T?, val expiry: Long)
    private val catalog = LinkedHashMap<String, Cached<AppleMusicAlbumInfo>>(24, 0.75f, true)
    private val notes = LinkedHashMap<String, Cached<String>>(24, 0.75f, true)

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
            .replace(Regex("\\s+"), " ")

    private fun queryKey(artist: String, album: String): String =
        normalize(ArtistNameUtils.firstArtist(artist)) + "|" + normalize(album)

    suspend fun load(artist: String, album: String): AppleMusicAlbumInfo? =
        withContext(Dispatchers.IO) {
            val key = queryKey(artist, album)
            if (key.startsWith("|") || key.endsWith("|")) return@withContext null
            val now = System.currentTimeMillis()
            synchronized(catalog) {
                catalog[key]?.takeIf { it.expiry > now }?.let { return@withContext it.value }
            }
            val term = ArtistNameUtils.firstArtist(artist) + " " + album
            val country = Locale.getDefault().country.uppercase(Locale.ROOT)
                .takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "US"
            // Search an actual ALBUM, not a song from an unrelated compilation.
            val endpoint = "https://itunes.apple.com/search?entity=album&media=music" +
                "&limit=25&country=$country&term=${Uri.encode(term)}"
            val payload = fetch(endpoint, MAX_JSON_BYTES) ?: return@withContext null
            val json = runCatching { JSONObject(String(payload, Charsets.UTF_8)) }.getOrNull()
            val results = json?.optJSONArray("results")
            val info = results?.let { matchAlbum(it, artist, album) }
            synchronized(catalog) {
                catalog[key] = Cached(info, now + if (info != null) TTL_MS else MISS_MS)
                while (catalog.size > 128) catalog.remove(catalog.keys.first())
            }
            info
        }

    private fun matchAlbum(
        albums: JSONArray, expectedArtist: String, expectedAlbum: String
    ): AppleMusicAlbumInfo? {
        val normalizedAlbum = normalize(expectedAlbum)
        val normalizedArtist = normalize(ArtistNameUtils.firstArtist(expectedArtist))
        if (normalizedAlbum.isBlank() || normalizedArtist.isBlank()) return null
        for (index in 0 until albums.length()) {
            val result = albums.optJSONObject(index) ?: continue
            if (result.optString("collectionType") != "Album") continue
            if (normalize(result.optString("collectionName")) != normalizedAlbum) continue
            val catalogArtist = normalize(ArtistNameUtils.firstArtist(result.optString("artistName")))
            if (catalogArtist != normalizedArtist) continue
            val rawUrl = result.optString("collectionViewUrl").substringBefore("?")
            val safeUrl = rawUrl.takeIf(::isAppleMusicAlbumUrl)
            return AppleMusicAlbumInfo(
                name = result.optString("collectionName"),
                artist = result.optString("artistName"),
                releaseDate = result.optString("releaseDate").takeIf { it.isNotBlank() },
                genre = result.optString("primaryGenreName").takeIf { it.isNotBlank() },
                trackCount = result.optInt("trackCount").takeIf { it > 0 },
                copyright = result.optString("copyright").takeIf { it.isNotBlank() },
                appleMusicUrl = safeUrl
            )
        }
        return null
    }

    suspend fun editorialNotes(albumUrl: String): String? = withContext(Dispatchers.IO) {
        if (!isAppleMusicAlbumUrl(albumUrl)) return@withContext null
        val now = System.currentTimeMillis()
        synchronized(notes) {
            notes[albumUrl]?.takeIf { it.expiry > now }?.let {
                return@withContext it.value
            }
        }
        val html = fetch(albumUrl, MAX_PAGE_BYTES) ?: return@withContext null
        val note = extractNotes(String(html, Charsets.UTF_8))
        synchronized(notes) {
            notes[albumUrl] = Cached(note, now + if (note != null) TTL_MS else MISS_MS)
            while (notes.size > 128) notes.remove(notes.keys.first())
        }
        note
    }

    private fun extractNotes(html: String): String? {
        // Apple's public web page embeds the text under:
        // data[].data.sections[].items[].modalPresentationDescriptor.paragraphText
        val raw = Regex(
            """<script[^>]*id=["']serialized-server-data["'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)?.groupValues?.getOrNull(1) ?: return null
        val array = runCatching { JSONArray(raw) }.getOrNull()
            ?: runCatching { JSONArray().put(JSONObject(raw)) }.getOrNull() ?: return null
        for (pageIndex in 0 until array.length()) {
            val data = array.optJSONObject(pageIndex)?.optJSONArray("data") ?: continue
            for (d in 0 until data.length()) {
                val sections = data.optJSONObject(d)?.optJSONObject("data")
                    ?.optJSONArray("sections") ?: continue
                for (s in 0 until sections.length()) {
                    val items = sections.optJSONObject(s)?.optJSONArray("items") ?: continue
                    for (i in 0 until items.length()) {
                        val para = items.optJSONObject(i)
                            ?.optJSONObject("modalPresentationDescriptor")
                            ?.optString("paragraphText").orEmpty()
                        if (para.isBlank()) continue
                        val plain = Html.fromHtml(para, Html.FROM_HTML_MODE_COMPACT)
                            .toString().replace(Regex("\\s+"), " ").trim()
                        if (plain.isNotBlank()) return plain.take(1_200)
                    }
                }
            }
        }
        return null
    }

    private fun isAppleMusicAlbumUrl(value: String): Boolean = runCatching {
        val url = URL(value)
        url.protocol.equals("https", true) &&
            url.host.equals("music.apple.com", true) && url.path.contains("/album/")
    }.getOrDefault(false)

    private fun fetch(endpoint: String, maximum: Int): ByteArray? {
        val url = URL(endpoint)
        if (url.protocol != "https" ||
            url.host !in setOf("itunes.apple.com", "music.apple.com")) return null
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 3_000
            connection.readTimeout = 5_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json,text/html")
            connection.setRequestProperty("User-Agent", "Aruna/AlbumInfo")
            if (connection.responseCode != HttpURLConnection.HTTP_OK ||
                connection.contentLengthLong > maximum) return null
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream(16 * 1024)
                val chunk = ByteArray(8 * 1024)
                while (true) {
                    val count = input.read(chunk)
                    if (count < 0) break
                    if (output.size() + count > maximum) return null
                    output.write(chunk, 0, count)
                }
                return output.toByteArray()
            }
        } catch (_: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }
}
