package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import java.net.URI
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

internal data class ArtworkLookupMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = "",
    val durationMs: Long = 0L
) {
    val artists: List<String>
        get() = listOf(artist, ArtistNameUtils.firstArtist(artist), albumArtist)
            .map(::knownArtworkTag)
            .filter { it.isNotBlank() && normalizedArtworkTag(it) != "various artists" }
            .distinctBy(::compactArtworkTag)

    val cacheKey: String
        get() = listOf(title, artist, album, albumArtist).joinToString("|") { normalizedArtworkTag(it) }

    val canSearchCatalog: Boolean
        get() = title.isNotBlank() && artists.isNotEmpty()
}

internal data class AppleCatalogSong(
    val title: String,
    val artist: String,
    val album: String,
    val albumUrl: String,
    val durationMs: Long = 0L
)

internal fun artworkLookupMetadata(song: Song, embedded: ArtworkLookupMetadata?): ArtworkLookupMetadata {
    fun tag(fileTag: String?, libraryTag: String): String =
        knownArtworkTag(fileTag.orEmpty()).ifBlank { knownArtworkTag(libraryTag) }

    return ArtworkLookupMetadata(
        title = tag(embedded?.title, song.title),
        artist = tag(embedded?.artist, song.artist),
        album = tag(embedded?.album, song.album),
        albumArtist = knownArtworkTag(embedded?.albumArtist.orEmpty()),
        durationMs = embedded?.durationMs?.takeIf { it > 0L } ?: song.durationMs
    )
}

internal fun knownArtworkTag(value: String): String {
    val trimmed = value.trim()
    return trimmed.takeUnless {
        normalizedArtworkTag(it) in setOf("", "unknown", "unknown title", "unknown artist", "unknown album", "tidak diketahui")
    }.orEmpty()
}

internal fun normalizedArtworkTag(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

private fun compactArtworkTag(value: String): String = normalizedArtworkTag(value).replace(" ", "")

internal fun artworkSearchTitle(value: String): String = value
    .replace(Regex("(?i)\\s*[\\[(](?:feat\\.?|ft\\.?|featuring)\\s+[^)\\]]*[)\\]]"), "")
    .replace(Regex("(?i)\\s+(?:feat\\.?|ft\\.?|featuring)\\s+.*$"), "")
    .replace(Regex("(?i)\\.(?:flac|mp3|m4a|aac|alac|wav|ogg|opus)$"), "")
    .trim()

internal fun artworkSearchAlbum(value: String): String = value
    .replace(Regex("(?i)\\s*[\\[(](?:deluxe|expanded|standard)(?:\\s+(?:edition|version))?[)\\]]\\s*$"), "")
    .trim()

internal fun appleMusicAlbumUrl(value: String): String? = runCatching {
    val uri = URI(value)
    if (uri.scheme != "https" || uri.host != "music.apple.com") return null
    if (!Regex("/(?:[a-zA-Z]{2}/)?album/(?:[^/]+/)?[0-9]+/?").matches(uri.rawPath.orEmpty())) return null
    "https://music.apple.com${uri.rawPath.trimEnd('/')}"
}.getOrNull()

internal fun matchingAppleAlbums(metadata: ArtworkLookupMetadata, results: List<AppleCatalogSong>): List<AppleCatalogSong> {
    if (!metadata.canSearchCatalog) return emptyList()
    val title = compactArtworkTag(artworkSearchTitle(metadata.title))
    val artists = metadata.artists.map(::compactArtworkTag).toSet()
    val album = compactArtworkTag(artworkSearchAlbum(metadata.album))
    return results.filter { candidate ->
        val candidateArtists = listOf(candidate.artist, ArtistNameUtils.firstArtist(candidate.artist))
            .map(::compactArtworkTag)
        val durationMatches = metadata.durationMs <= 0 || candidate.durationMs <= 0 ||
            abs(metadata.durationMs - candidate.durationMs) <= 30_000L
        title == compactArtworkTag(artworkSearchTitle(candidate.title)) &&
            candidateArtists.any { it in artists } && durationMatches &&
            appleMusicAlbumUrl(candidate.albumUrl) != null
    }.sortedByDescending { candidate ->
        val albumMatches = album.isNotBlank() && album == compactArtworkTag(artworkSearchAlbum(candidate.album))
        val durationMatches = metadata.durationMs > 0 && candidate.durationMs > 0 &&
            abs(metadata.durationMs - candidate.durationMs) <= 3_000L
        (if (albumMatches) 100 else 0) + (if (durationMatches) 10 else 0)
    }.distinctBy { it.albumUrl.substringBefore('?').trimEnd('/').substringAfterLast('/') }
}
