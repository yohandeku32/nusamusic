package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Optional online source for LOCAL songs. AMLL supplies authored TTML
 * word/syllable timing; LRCLIB supplies synced LRC as the fallback.
 * No WebView, tokens, user account, lyrics HTML scraping or media upload.
 */
internal class OnlineLyricsService(
    private val request: suspend (String) -> String? = ::requestLyricsJson
) {
    data class Result(val content: String, val source: String)
    data class Lookup(val result: Result?, val conclusivelyMissing: Boolean)

    suspend fun lookup(song: Song): Lookup {
        if (song.title.isBlank() || song.artist.isBlank()) return Lookup(null, true)
        var allAvailable = true
        val amll = try { findAmll(song) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { allAvailable = false; null }
        if (amll != null) return Lookup(amll, true)

        val lrclib = try { findLrclib(song) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { allAvailable = false; null }
        return Lookup(lrclib, allAvailable)
    }

    private suspend fun findAmll(song: Song): Result? {
        val url = "https://api.amll.dev/v1/lyrics/search" +
            "?musicName=${encodeLyricsQuery(song.title)}" +
            "&artistName=${encodeLyricsQuery(primaryLyricsArtist(song.artist))}&pageSize=8"
        val root = JSONObject(request(url) ?: throw IOException("AMLL unavailable"))
        val items = root.optJSONObject("data")?.optJSONArray("items") ?: return null
        val candidates = buildList {
            for (i in 0 until minOf(items.length(), 8)) {
                val entry = items.optJSONObject(i) ?: continue
                val titleNames = entry.optJSONArray("musicNames").lyricNames()
                val artistNames = entry.optJSONArray("artistNames").lyricNames()
                if (!onlineLyricsMatch(song, titleNames, artistNames)) continue
                val filename = entry.optString("filename")
                if (filename.isBlank() || !filename.endsWith(".ttml")) continue
                val albums = entry.optJSONArray("albumNames").lyricNames()
                val albumRank = if (song.album.isNotBlank() &&
                    albums.any { normalizeLyricsTag(it) == normalizeLyricsTag(song.album) }
                ) 1 else 0
                add(albumRank to filename)
            }
        }.sortedByDescending { it.first }
        // Bounded fetches. Invalid community TTML must never replace a local
        // lyric or trigger a provider-wide crawl of candidate versions.
        for ((_, filename) in candidates.take(2)) {
            coroutineContext.ensureActive()
            val uri = "https://api.amll.dev/v1/lyrics/get?filename=${encodeLyricsQuery(filename)}"
            val response = JSONObject(request(uri) ?: throw IOException("AMLL unavailable"))
            val ttml = response.optJSONObject("data")?.optString("lyrics").orEmpty()
            if (ttml.isNotBlank() && ttml.toByteArray(Charsets.UTF_8).size <= LyricsDecoder.MAX_BYTES &&
                runCatching {
                    LyricsDecoder.decodeWithTiming(ttml, song.durationMs)
                        .lyrics.lines.any {
                            it is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
                        }
                }.getOrDefault(false)
            ) return Result(ttml, "AMLL")
        }
        return null
    }

    private suspend fun findLrclib(song: Song): Result? {
        val parameters = buildString {
            append("?track_name=${encodeLyricsQuery(song.title)}")
            append("&artist_name=${encodeLyricsQuery(primaryLyricsArtist(song.artist))}")
            if (song.album.isNotBlank()) append("&album_name=${encodeLyricsQuery(song.album)}")
            if (song.durationMs in 1000L..3_600_000L)
                append("&duration=${(song.durationMs + 500L) / 1000L}")
        }
        // A 404 means missing, not a network failure.
        val json = request("https://lrclib.net/api/get$parameters") ?: return null
        val item = JSONObject(json)
        if (item.optBoolean("instrumental") ||
            !onlineLyricsMatch(song,
                listOf(item.optString("trackName")), listOf(item.optString("artistName")))
        ) return null
        val remoteDuration = item.optDouble("duration", 0.0)
        if (song.durationMs > 0 && remoteDuration > 0 &&
            kotlin.math.abs(song.durationMs / 1000.0 - remoteDuration) > 3.5
        ) return null
        val synced = item.optString("syncedLyrics").takeIf {
            it.isNotBlank() && it != "null" &&
                it.toByteArray(Charsets.UTF_8).size <= LyricsDecoder.MAX_BYTES &&
                runCatching { LyricsDecoder.decode(it, song.durationMs).lines.isNotEmpty() }
                    .getOrDefault(false)
        }
        return synced?.let { Result(it, "LRCLIB") }
    }
}

private fun org.json.JSONArray?.lyricNames(): List<String> = buildList {
    val array = this@lyricNames ?: return@buildList
    for (i in 0 until array.length()) {
        val value = array.optString(i)
        if (value.isNotBlank()) add(value)
    }
}

internal fun normalizeLyricsTag(value: String): String = value
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim().replace(Regex("\\s+"), " ")

internal fun primaryLyricsArtist(value: String): String =
    value.split(Regex("(?i)\\s+(?:feat\\.?|ft\\.?|featuring|with|&|x|and)\\s+|[,;/]"))
        .firstOrNull().orEmpty().trim()

internal fun onlineLyricsMatch(
    song: Song, titles: List<String>, artists: List<String>
): Boolean {
    val targetTitle = normalizeLyricsTag(song.title)
    val targetArtist = normalizeLyricsTag(primaryLyricsArtist(song.artist))
    if (targetTitle.isBlank() || targetArtist.isBlank()) return false
    return titles.any { normalizeLyricsTag(it) == targetTitle } &&
        artists.any { normalizeLyricsTag(primaryLyricsArtist(it)) == targetArtist }
}

internal fun encodeLyricsQuery(text: String): String =
    URLEncoder.encode(text, "UTF-8")

/** Bounded HTTPS, streamed response limited to 512 KiB. 404 is a normal miss. */
private suspend fun requestLyricsJson(url: String): String? = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 3_500
        connection.readTimeout = 3_500
        connection.setRequestProperty("User-Agent", "NusaMusic/1.0 (Android; lyrics lookup)")
        connection.setRequestProperty("Accept", "application/json")
        connection.instanceFollowRedirects = false
        when (connection.responseCode) {
            HttpURLConnection.HTTP_NOT_FOUND -> null
            HttpURLConnection.HTTP_OK -> {
                val length = connection.contentLengthLong
                if (length > LyricsDecoder.MAX_BYTES.toLong()) throw IOException("Lyrics response too large")
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    val output = java.io.ByteArrayOutputStream()
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (output.size() + n > LyricsDecoder.MAX_BYTES)
                            throw IOException("Lyrics response too large")
                        output.write(buffer, 0, n)
                    }
                    output.toString(Charsets.UTF_8.name())
                }
            }
            else -> throw IOException("Lyrics HTTP ${connection.responseCode}")
        }
    } finally {
        connection.disconnect()
    }
}
