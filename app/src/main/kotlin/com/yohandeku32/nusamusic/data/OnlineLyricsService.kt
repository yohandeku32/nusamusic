package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * lrc.red only: resolve title and artist via match/search, then fetch the
 * ISRC-specific song JSON containing original TTML or timed lyric lines.
 * No Apple Music account, WebView or uploaded audio.
 */
internal class OnlineLyricsService(
    private val request: suspend (String) -> String? = ::requestLyricsText
) {
    data class Result(val content: String, val source: String = "lrc.red")
    data class Lookup(val result: Result?, val conclusivelyMissing: Boolean)

    suspend fun lookup(song: Song): Lookup {
        if (song.title.isBlank() || song.artist.isBlank()) return Lookup(null, true)
        val artist = primaryLyricsArtist(song.artist)
        val shared = "track=" + encodeLyricsQuery(song.title) +
            "&artist=" + encodeLyricsQuery(artist)
        val album = if (song.album.isNotBlank())
            "&album=" + encodeLyricsQuery(song.album) else ""
        val duration = if (song.durationMs in 1_000L..3_600_000L)
            "&duration=" + ((song.durationMs + 500L) / 1_000L) else ""
        val queries = listOf(
            "https://lrc.red/match.json?" + shared + album + duration,
            "https://lrc.red/match.json?" + shared,
            "https://lrc.red/search.json?q=" + encodeLyricsQuery(song.title + " " + artist)
        ).distinct()

        var gotSearchResponse = false
        var hadError = false
        val seenIsrcs = HashSet<String>()
        search@ for (url in queries) {
            coroutineContext.ensureActive()
            val payload = try { request(url) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    // Do not repeat dead-host requests when the first failed.
                    hadError = true
                    break
                }
            if (payload == null) continue
            val hits = try { JSONObject(payload).optJSONArray("hits") }
                catch (_: Exception) { hadError = true; continue }
            if (hits == null) { hadError = true; break }
            gotSearchResponse = true
            for (index in 0 until minOf(hits.length(), 6)) {
                coroutineContext.ensureActive()
                val hit = hits.optJSONObject(index) ?: continue
                val isrc = hit.optString("isrc").trim().uppercase(Locale.ROOT)
                if (!VALID_ISRC.matches(isrc) || !seenIsrcs.add(isrc)) continue
                val title = hit.optString("title").ifBlank { hit.optString("track") }
                val artistName = hit.optString("artist")
                if (title.isNotBlank() && artistName.isNotBlank() &&
                    !onlineLyricsMatch(song, listOf(title), listOf(artistName))) continue
                try {
                    fetchSong(isrc, song)?.let { return Lookup(it, true) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Avoid exhausting the remaining timeout retrying a
                    // failing host. Try the independent LRCLIB server now.
                    hadError = true
                    break@search
                }
            }
        }
        // lrc.red remains the preferred source. When that community host has
        // no matching lyrics or is unreachable, a single documented LRCLIB
        // search prevents an otherwise permanently empty Lyrics panel.
        var fallbackError = false
        val fallback = try { findSyncedLrclibFallback(song) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { fallbackError = true; null }
        return Lookup(fallback, gotSearchResponse && !hadError && !fallbackError)
    }

    private suspend fun findSyncedLrclibFallback(song: Song): Result? {
        val url = "https://lrclib.net/api/search?track_name=" +
            encodeLyricsQuery(song.title) + "&artist_name=" +
            encodeLyricsQuery(primaryLyricsArtist(song.artist))
        val payload = request(url) ?: throw IOException("LRCLIB unavailable")
        val results = JSONArray(payload)
        for (i in 0 until minOf(results.length(), 20)) {
            coroutineContext.ensureActive()
            val candidate = results.optJSONObject(i) ?: continue
            if (candidate.optBoolean("instrumental", false)) continue
            if (!onlineLyricsMatch(song,
                    listOf(candidate.optString("trackName")),
                    listOf(candidate.optString("artistName"))
                )) continue
            val seconds = candidate.optDouble("duration", Double.NaN)
            if (seconds.isFinite() && seconds > 0 && song.durationMs > 0 &&
                abs(seconds - song.durationMs / 1_000.0) > 6.0) continue
            val lrc = candidate.optString("syncedLyrics")
            if (validTiming(lrc, song.durationMs)) return Result(lrc, "LRCLIB")
        }
        return null
    }

    private suspend fun fetchSong(isrc: String, song: Song): Result? {
        val base = "https://lrc.red/s/" + isrc

        // The directly documented TTML endpoint is checked first, avoiding
        // an unnecessary community JSON round-trip when karaoke is present.
        val directTtml = request(base + ".ttml")
        if (validTiming(directTtml, song.durationMs)) return Result(directTtml!!)

        // Some records supply timed word segments only in .song.json.
        val json = request(base + ".song.json")
        if (json != null) {
            val record = runCatching { JSONObject(json) }.getOrNull()
            if (record != null) {
                val title = record.optString("title")
                val artist = record.optString("artist")
                if (title.isNotBlank() && artist.isNotBlank() &&
                    !onlineLyricsMatch(song, listOf(title), listOf(artist))) return null
                val duration = record.optDouble("duration", Double.NaN)
                if (duration.isFinite() && duration > 0 && song.durationMs > 0 &&
                    abs(song.durationMs / 1000.0 - duration) > 6.0) return null

                // First use authored TTML, then explicit structured timing.
                val ttml = record.optString("source").takeIf { it.trimStart().startsWith("<") }
                if (validTiming(ttml, song.durationMs)) return Result(ttml!!)
                val reconstructed = reconstructTtml(record)
                if (validTiming(reconstructed, song.durationMs)) return Result(reconstructed!!)
            }
        }

        // Finally try the documented line-synced format.
        val lrc = request(base + ".lrc")
        if (validTiming(lrc, song.durationMs)) return Result(lrc!!)
        return null
    }

    private fun validTiming(raw: String?, duration: Long): Boolean =
        !raw.isNullOrBlank() &&
            raw.toByteArray(Charsets.UTF_8).size <= LyricsDecoder.MAX_BYTES &&
            runCatching { LyricsDecoder.decodeWithTiming(raw, duration).lyrics.lines.isNotEmpty() }
                .getOrDefault(false)

    private fun reconstructTtml(record: JSONObject): String? {
        val lyrics = record.optJSONObject("lyrics") ?: return null
        val rows = lyrics.optJSONArray("lines") ?: return null
        if (rows.length() !in 1..800) return null
        val wordSync = lyrics.optString("sync") == "word"
        val xml = StringBuilder("<tt><body>")
        var count = 0
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val begin = row.second("begin") ?: continue
            val end = row.second("end") ?: continue
            if (end <= begin) continue
            val words = if (wordSync) row.optJSONArray("words") else null
            val pieces = ArrayList<Triple<String, Double, Double>>()
            if (words != null) {
                for (w in 0 until minOf(words.length(), 100)) {
                    val word = words.optJSONArray(w) ?: continue
                    for (k in 0 until minOf(word.length(), 32)) {
                        val syllable = word.optJSONObject(k) ?: continue
                        val text = syllable.optString("text")
                        val a = syllable.second("begin") ?: continue
                        val b = syllable.second("end") ?: continue
                        if (text.isBlank() || b <= a || a < begin || b > end + 0.2) continue
                        val gap = if (k == word.length() - 1 && w < words.length() - 1)
                            " " else ""
                        pieces.add(Triple(text + gap, a, b))
                    }
                }
            }
            val lineText = row.optString("text")
            if (pieces.isEmpty() && lineText.isBlank()) continue
            xml.append("<p begin=\"").append(begin).append("s\" end=\"")
                .append(end).append("s\">")
            if (pieces.isEmpty()) xml.append(escapeXml(lineText))
            else for ((text, a, b) in pieces) {
                xml.append("<span begin=\"").append(a).append("s\" end=\"")
                    .append(b).append("s\">").append(escapeXml(text)).append("</span>")
            }
            xml.append("</p>")
            count++
        }
        xml.append("</body></tt>")
        return if (count > 0) xml.toString() else null
    }
}

private fun JSONObject.second(key: String): Double? =
    optDouble(key, Double.NaN).takeIf { it.isFinite() && it in 0.0..86400.0 }

private fun escapeXml(value: String): String = value
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    .replace("\"", "&quot;")

private val VALID_ISRC = Regex("[A-Z]{2}[A-Z0-9]{3}[0-9]{7}")

internal fun normalizeLyricsTag(value: String): String = value
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim().replace(Regex("\\s+"), " ")

internal fun primaryLyricsArtist(value: String): String =
    value.split(Regex("(?i)\\s+(?:feat\\.?|ft\\.?|featuring|with|&|x|and)\\s+|[,;/]"))
        .firstOrNull().orEmpty().trim()

private fun normalizeTrackTitle(title: String): String {
    val suffix = Regex(
        "(?i)\\s*[-(\\[]\\s*(?:remaster(?:ed)?(?:\\s+\\d{4})?|\\d{4}\\s+remaster(?:ed)?|official\\s+audio|official\\s+video)\\s*[)\\]]?$"
    )
    return normalizeLyricsTag(title.replace(suffix, ""))
}

internal fun onlineLyricsMatch(song: Song, titles: List<String>, artists: List<String>): Boolean {
    val wantedTitle = normalizeTrackTitle(song.title)
    val wantedArtist = normalizeLyricsTag(primaryLyricsArtist(song.artist))
    if (wantedTitle.isBlank() || wantedArtist.isBlank()) return false
    return titles.any { normalizeTrackTitle(it) == wantedTitle } &&
        artists.any {
            val actual = normalizeLyricsTag(primaryLyricsArtist(it))
            actual == wantedArtist ||
                normalizeLyricsTag(it).split(' ').containsAll(wantedArtist.split(' '))
        }
}

internal fun encodeLyricsQuery(text: String): String = URLEncoder.encode(text, "UTF-8")

private suspend fun requestLyricsText(url: String): String? = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 3_500
        connection.readTimeout = 3_500
        connection.setRequestProperty(
            "User-Agent", "NusaMusic/1.0 (https://github.com/yohandeku32/nusamusic)"
        )
        connection.setRequestProperty("Accept", "application/json")
        // lrc.red uses canonical redirects on some routes. Java handles these
        // without asking callers to resubmit a lookup or discard the result.
        connection.instanceFollowRedirects = true
        when (connection.responseCode) {
            HttpURLConnection.HTTP_NOT_FOUND -> null
            HttpURLConnection.HTTP_OK -> {
                if (connection.contentLengthLong > LyricsDecoder.MAX_BYTES.toLong())
                    throw IOException("Lyrics response too large")
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    val output = ByteArrayOutputStream()
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
            else -> throw IOException("Lyrics HTTP " + connection.responseCode)
        }
    } finally {
        connection.disconnect()
    }
}
