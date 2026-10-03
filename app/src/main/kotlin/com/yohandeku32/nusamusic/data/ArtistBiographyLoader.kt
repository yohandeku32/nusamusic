package com.yohandeku32.nusamusic.data

import android.net.Uri
import android.text.Html
import android.text.Spanned
import com.yohandeku32.nusamusic.BuildConfig
import com.yohandeku32.nusamusic.model.Song
import com.yohandeku32.nusamusic.data.ArtistNameUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

data class ArtistBiography(
    val artistName: String,
    val text: String,
    val sourceLanguage: String,
    val sourceUrl: String
)

object ArtistBiographyLoader {
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 8_000
    private const val API_ROOT = "https://ws.audioscrobbler.com/2.0/"

    private val cache = ConcurrentHashMap<String, ArtistBiography>()

    fun isConfigured(): Boolean = BuildConfig.LASTFM_API_KEY.isNotBlank()

    suspend fun load(song: Song): ArtistBiography? = withContext(Dispatchers.IO) {
        val artist = ArtistNameUtils.firstArtist(song.artist)
        if (artist.isBlank() ||
            artist.equals("Unknown artist", ignoreCase = true) ||
            !isConfigured()
        ) {
            return@withContext null
        }

        val cacheKey = artist.lowercase()
        cache[cacheKey]?.let { return@withContext it }

        // Request Indonesian first. Last.fm supports localized biographies
        // when a translation exists, but not every artist has an Indonesian
        // biography. Fall back to English so the section never disappears
        // merely because an Indonesian translation is unavailable.
        val result = loadFromLastFm(artist, "id")
            ?: loadFromLastFm(artist, "en")

        result?.let { cache[cacheKey] = it }
        result
    }

    private fun loadFromLastFm(
        artist: String,
        language: String
    ): ArtistBiography? {
        val requestUrl = Uri.parse(API_ROOT).buildUpon()
            .appendQueryParameter("method", "artist.getInfo")
            .appendQueryParameter("artist", artist)
            .appendQueryParameter("api_key", BuildConfig.LASTFM_API_KEY)
            .appendQueryParameter("autocorrect", "1")
            .appendQueryParameter("lang", language)
            .appendQueryParameter("format", "json")
            .build()
            .toString()

        val jsonText = httpGet(requestUrl) ?: return null

        return runCatching {
            val root = JSONObject(jsonText)
            if (root.has("error")) return null

            val artistObject = root.optJSONObject("artist") ?: return null
            val bio = artistObject.optJSONObject("bio") ?: return null

            val summary = cleanBiography(bio.optString("summary"))
            val content = cleanBiography(bio.optString("content"))
            val biographyText = when {
                content.length >= summary.length && content.isNotBlank() -> content
                else -> summary
            }

            if (!isUsefulBiography(biographyText)) return null

            val resolvedName = artistObject.optString("name").trim()
                .ifBlank { artist }

            val sourceUrl = artistObject.optString("url").trim()
                .ifBlank {
                    "https://www.last.fm/music/" +
                        Uri.encode(resolvedName).replace("+", "%20")
                }

            ArtistBiography(
                artistName = resolvedName,
                text = biographyText,
                sourceLanguage = language,
                sourceUrl = sourceUrl
            )
        }.getOrNull()
    }

    private fun httpGet(urlString: String): String? {
        return runCatching {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty(
                "User-Agent",
                "NusaMusic/1.0 (local music player)"
            )
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

    private fun cleanBiography(value: String): String {
        if (value.isBlank()) return ""

        val normalized = value
            .replace(
                Regex(
                    "<script.*?</script>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                ),
                ""
            )
            .replace(
                Regex(
                    "<style.*?</style>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                ),
                ""
            )

        val spanned: Spanned =
            Html.fromHtml(normalized, Html.FROM_HTML_MODE_LEGACY)

        return spanned.toString()
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removeSuffix("Read more on Last.fm")
            .trim()
    }

    private fun isUsefulBiography(text: String): Boolean {
        if (text.length < 80) return false

        val lower = text.lowercase()
        val blocked = listOf(
            "no biography",
            "no bio",
            "biography is not available",
            "there is currently no biography"
        )

        return blocked.none(lower::contains)
    }
}
