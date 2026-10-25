package com.yohandeku32.nusamusic.data

internal data class ArtworkReply(
    val url: String? = null,
    val failed: Boolean = false,
    val alternatives: List<String> = emptyList()
) {
    fun afterPlaybackFailure(failedUrl: String): ArtworkReply {
        val remaining = (listOfNotNull(url) + alternatives).distinct().filter { it != failedUrl }
        return ArtworkReply(remaining.firstOrNull(), remaining.isEmpty(), remaining.drop(1))
    }
}
internal data class CatalogReply(val songs: List<AppleCatalogSong> = emptyList(), val failed: Boolean = false)
internal data class ArtworkAlbumQuery(val artist: String, val album: String, val title: String)

/** Keep the original album route available when catalog or direct-link requests fail. */
internal suspend fun resolveAnimatedArtwork(
    metadata: ArtworkLookupMetadata,
    libraryMetadata: ArtworkLookupMetadata,
    country: String,
    searchAlbum: suspend (ArtworkAlbumQuery) -> ArtworkReply,
    searchCatalog: suspend (String, String) -> CatalogReply,
    loadAlbumUrl: suspend (String) -> ArtworkReply
): ArtworkReply {
    val sources = listOf(metadata, libraryMetadata).distinct()
    fun queries(source: ArtworkLookupMetadata): List<ArtworkAlbumQuery> {
        if (source.album.isBlank()) return emptyList()
        return listOf(source.album, artworkSearchAlbum(source.album)).distinct().flatMap { album ->
            source.artists.map { artist -> ArtworkAlbumQuery(artist, album, artworkSearchTitle(source.title)) }
        }
    }
    val preferred = sources.mapNotNull { queries(it).firstOrNull() }.distinct()
    val remaining = sources.flatMap(::queries).distinct().filter { it !in preferred }.take(2)
    var failed = false
    for (query in preferred) {
        val reply = searchAlbum(query)
        if (reply.url != null) return reply
        failed = failed || reply.failed
    }

    if (metadata.canSearchCatalog) {
        val countries = listOf(country, "US", "ID").distinct()
        val searches = metadata.artists.take(2).flatMap { artist -> countries.map { it to artist } }.take(4)
        val attemptedAlbums = HashSet<String>()
        catalog@ for ((storefront, artist) in searches) {
            val reply = searchCatalog(artworkSearchTitle(metadata.title) + " " + artist, storefront)
            if (reply.failed) {
                failed = true
                break
            }
            for (candidate in matchingAppleAlbums(metadata, reply.songs).take(2)) {
                val url = appleMusicAlbumUrl(candidate.albumUrl) ?: continue
                if (!attemptedAlbums.add(url.substringAfterLast('/'))) continue
                val artwork = loadAlbumUrl(url)
                if (artwork.url != null) return artwork
                failed = failed || artwork.failed
                if (attemptedAlbums.size >= 3) break@catalog
            }
        }
    }

    for (query in remaining) {
        val reply = searchAlbum(query)
        if (reply.url != null) return reply
        failed = failed || reply.failed
    }
    return ArtworkReply(failed = failed)
}

/** Try the standard animation first and retain portrait as a playback alternative. */
internal fun preferredAnimatedArtworkUrls(standard: String?, tall: String? = null): List<String> =
    listOfNotNull(standard, tall)
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.equals("null", true) && !it.equals("none", true) }
        .distinct()
