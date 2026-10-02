package com.yohandeku32.nusamusic.data

import android.net.Uri
import android.text.Html
import android.text.Spanned
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class ArtistBiography(
    val artistName: String,
    val text: String,
    val sourceLanguage: String,
    val sourceUrl: String
)

object ArtistBiographyLoader {
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 8_000

    suspend fun load(song: Song): ArtistBiography? = withContext(Dispatchers.IO) {
        val artist = song.artist.trim()
        if (artist.isBlank() || artist.equals("Unknown artist", ignoreCase = true)) {
            return@withContext null
        }

        loadFromWikipedia(artist, "id") ?: loadFromWikipedia(artist, "en")
    }

    private fun loadFromWikipedia(
        artist: String,
        language: String
    ): ArtistBiography? {
        val searchUrl =
            "https://$language.wikipedia.org/w/rest.php/v1/search/page" +
                "?q=" + Uri.encode(artist) + "&limit=5"

        val searchJson = httpGet(searchUrl) ?: return null
        val pageTitle = findBestPageTitle(searchJson, artist) ?: return null

        val encodedTitle = Uri.encode(pageTitle).replace("+", "%20")
        val pageUrl =
            "https://$language.wikipedia.org/w/rest.php/v1/page/" +
                encodedTitle + "/with_html"

        val pageJson = httpGet(pageUrl) ?: return null
        val html = extractJsonString(pageJson, "html") ?: return null
        val paragraphs = extractParagraphs(html)

        val text = paragraphs
            .asSequence()
            .map(::cleanParagraph)
            .filter(::isUsefulParagraph)
            .take(3)
            .joinToString("\n\n")
            .trim()

        if (text.length < 80) return null

        return ArtistBiography(
            artistName = pageTitle,
            text = text,
            sourceLanguage = language,
            sourceUrl =
                "https://$language.wikipedia.org/wiki/" +
                    Uri.encode(pageTitle).replace("+", "_")
        )
    }

    private fun httpGet(urlString: String): String? {
        return runCatching {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "NusaMusic/1.0 (local music player)")
            connection.setRequestProperty("Accept", "application/json")
            connection.instanceFollowRedirects = true

            try {
                if (connection.responseCode !in 200..299) return null

                BufferedReader(
                    InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)
                ).use { reader -> reader.readText() }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun findBestPageTitle(json: String, artist: String): String? {
        val pagesStart = json.indexOf("\"pages\"")
        if (pagesStart < 0) return null

        val titles = Regex("\"title\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            .findAll(json.substring(pagesStart))
            .mapNotNull { unescapeJson(it.groupValues[1]) }
            .toList()

        if (titles.isEmpty()) return null

        val normalizedArtist = normalize(artist)
        return titles.firstOrNull { normalize(it) == normalizedArtist }
            ?: titles.firstOrNull {
                normalize(it).contains(normalizedArtist) ||
                    normalizedArtist.contains(normalize(it))
            }
            ?: titles.firstOrNull()
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"".toRegex()
        return pattern.find(json)?.let { unescapeJson(it.groupValues[1]) }
    }

    private fun unescapeJson(value: String): String =
        value
            .replace("\\\\", "\u0000")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\u0000", "\\")

    private fun extractParagraphs(html: String): List<String> =
        Regex(
            "<p(?:\\s[^>]*)?>(.*?)</p>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
            .findAll(html)
            .map { it.groupValues[1] }
            .toList()

    private fun cleanParagraph(html: String): String {
        val normalized = html
            .replace(Regex(
                "<sup.*?</sup>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ), "")
            .replace(Regex(
                "<style.*?</style>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ), "")
            .replace(Regex(
                "<script.*?</script>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ), "")

        val spanned: Spanned = Html.fromHtml(normalized, Html.FROM_HTML_MODE_LEGACY)
        return spanned.toString()
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isUsefulParagraph(text: String): Boolean {
        if (text.length < 40) return false
        val lower = text.lowercase()
        return listOf("may refer to", "disambiguation", "redirect").none(lower::contains)
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "")
}