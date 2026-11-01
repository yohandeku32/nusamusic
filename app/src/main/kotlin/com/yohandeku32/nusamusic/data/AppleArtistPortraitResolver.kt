package com.yohandeku32.nusamusic.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

internal fun buildAppleMusicPortraitUrl(name: String): String? {
    val artist = ArtistNameUtils.firstArtist(name).trim()
    if (artist.length !in 2..100 || artist.equals("unknown artist", true) ||
        artist.equals("artis tidak dikenal", true)) return null
    return "https://itunes.apple.com/search?entity=musicArtist&media=music&attribute=artistTerm" +
        "&limit=10&country=US&term=" + URLEncoder.encode(artist, "UTF-8")
}

private fun portraitMatchName(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

internal data class PortraitHttpResponse(val status: Int, val bytes: ByteArray, val contentType: String = "")

internal fun appleArtistPage(catalog: String, artist: String): String? {
    val results = JSONObject(catalog).getJSONArray("results")
    val target = portraitMatchName(ArtistNameUtils.firstArtist(artist))
    if (target.isBlank()) return null
    for (i in 0 until results.length()) {
        val result = results.optJSONObject(i) ?: continue
        if (result.optString("wrapperType") != "artist" ||
            portraitMatchName(result.optString("artistName")) != target) continue
        val id = result.optLong("artistId")
        if (id <= 0) continue
        val link = runCatching { URI(result.optString("artistLinkUrl")) }.getOrNull()
        if (link?.scheme == "https" && link.host == "music.apple.com" &&
            link.rawUserInfo == null && link.port in listOf(-1, 443) &&
            link.path.matches(Regex("/[a-z]{2}/artist/[^/]+/$id"))) {
            return "https://music.apple.com${link.rawPath}"
        }
        return "https://music.apple.com/us/artist/$id"
    }
    return null
}

internal fun isApplePortraitResource(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme == "https" && uri.rawUserInfo == null && uri.port in listOf(-1, 443) &&
        (uri.host == "itunes.apple.com" || uri.host == "music.apple.com" ||
            uri.host?.endsWith(".mzstatic.com") == true)
}.getOrDefault(false)

internal fun applePortraitImage(value: String): String? {
    if (!isApplePortraitResource(value)) return null
    val uri = URI(value)
    if (uri.host?.endsWith(".mzstatic.com") != true || !uri.path.startsWith("/image/")) return null
    val path = uri.rawPath.replace(Regex("/\\d+x\\d+[^/]*$"), "/600x600bb.jpg")
    return "https://${uri.host}$path"
}

internal fun appleArtistImageFromPage(html: String, artist: String): String? {
    val target = portraitMatchName(ArtistNameUtils.firstArtist(artist))
    val scripts = Regex("""<script\b[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    for (match in scripts.findAll(html)) {
        val data = runCatching { JSONObject(match.groupValues[1]) }.getOrNull() ?: continue
        if (data.optString("@type") !in setOf("MusicGroup", "Person") ||
            portraitMatchName(data.optString("name")) != target) continue
        return applePortraitImage(data.optString("image"))
    }
    return null
}

internal suspend fun resolveAppleArtistPortrait(
    name: String,
    request: suspend (String) -> PortraitHttpResponse
): String? {
    val search = buildAppleMusicPortraitUrl(name) ?: return null
    currentCoroutineContext().ensureActive()
    val catalog = request(search)
    if (catalog.status != 200) throw IOException("Artist catalog unavailable: ${catalog.status}")
    val page = appleArtistPage(catalog.bytes.toString(Charsets.UTF_8), name) ?: return null
    currentCoroutineContext().ensureActive()
    val response = request(page)
    if (response.status == 404) return null
    if (response.status != 200) throw IOException("Artist page unavailable: ${response.status}")
    currentCoroutineContext().ensureActive()
    val html = response.bytes.toString(Charsets.UTF_8)
    if (!html.contains("application/ld+json", ignoreCase = true)) {
        throw IOException("Artist page metadata unavailable")
    }
    return appleArtistImageFromPage(html, name)
}
