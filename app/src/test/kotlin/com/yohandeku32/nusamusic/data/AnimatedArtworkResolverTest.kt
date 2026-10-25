package com.yohandeku32.nusamusic.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class AnimatedArtworkResolverTest {
    @Test
    fun standardAndPortraitAreRetainedInPlaybackOrder() {
        assertEquals(listOf("square", "portrait"), preferredAnimatedArtworkUrls("square", "portrait"))
        assertEquals(listOf("square"), preferredAnimatedArtworkUrls("square", null))
        assertEquals(listOf("portrait"), preferredAnimatedArtworkUrls(null, "portrait"))
        assertEquals(emptyList<String>(), preferredAnimatedArtworkUrls("null", "NONE"))
        assertEquals(emptyList<String>(), preferredAnimatedArtworkUrls(" ", null))
        assertEquals(listOf("https://apple.com/square.m3u8"),
            preferredAnimatedArtworkUrls("  https://apple.com/square.m3u8  ", "https://apple.com/square.m3u8"))
    }
    private val metadata = ArtworkLookupMetadata("Song", "Singer", "Album (Deluxe Edition)")
    private val stream = "https://example.mzstatic.com/artwork.m3u8"

    @Test
    fun playbackFailureTriesTheOtherAppleVariantBeforeExhaustingCandidates() {
        val square = "https://example.mzstatic.com/square.m3u8"
        val tall = "https://example.mzstatic.com/tall.m3u8"
        val urls = AppleMusicAnimatedArtworkLoader.animatedUrls(JSONObject()
            .put("url", square).put("url_tall", tall))
        val original = ArtworkReply(urls.first(), alternatives = urls.drop(1))
        val remaining = original.afterPlaybackFailure(square)
        assertEquals(tall, remaining.url)
        val exhausted = remaining.afterPlaybackFailure(tall)
        assertEquals(null, exhausted.url)
        assertTrue(exhausted.alternatives.isEmpty())
        assertTrue(exhausted.failed)
    }

    @Test
    fun squareOnlyAlbumResolvesWithoutSearchingForAnotherPortraitAlbum() = runBlocking {
        val square = "https://example.mzstatic.com/square.m3u8"
        val result = resolveAnimatedArtwork(
            metadata, metadata, "US",
            searchAlbum = {
                val urls = AppleMusicAnimatedArtworkLoader.animatedUrls(JSONObject().put("url", square))
                ArtworkReply(urls.firstOrNull(), alternatives = urls.drop(1))
            },
            searchCatalog = { _, _ -> error("A square animation is already available") },
            loadAlbumUrl = { error("Another album lookup is unnecessary") }
        )
        assertEquals(square, result.url)
    }

    @Test
    fun responseRetainsSquareAndPortraitWhileRemovingDuplicates() {
        val square = "https://example.mzstatic.com/square.m3u8"
        val tall = "https://example.mzstatic.com/tall.m3u8"
        assertEquals(listOf(square, tall), AppleMusicAnimatedArtworkLoader.animatedUrls(
            JSONObject().put("url", square).put("url_tall", tall)))
        assertEquals(listOf(square), AppleMusicAnimatedArtworkLoader.animatedUrls(
            JSONObject().put("url", " $square ").put("url_tall", square)))
    }

    @Test
    fun invalidStandardUrlStillAllowsValidPortraitAndRejectsUntrustedHosts() {
        val tall = "https://example.mzstatic.com/tall.m3u8"
        for (invalid in listOf("null", "NONE", "", "http://a.mzstatic.com/a.m3u8",
            "https://mzstatic.com.evil.example/a.m3u8", "file:///a.mp4")) {
            assertEquals(listOf(tall), AppleMusicAnimatedArtworkLoader.animatedUrls(
                JSONObject().put("url", invalid).put("url_tall", tall)))
        }
        assertTrue(AppleMusicAnimatedArtworkLoader.animatedUrls(null).isEmpty())
        assertTrue(AppleMusicAnimatedArtworkLoader.animatedUrls(JSONObject()).isEmpty())
    }

    @Test
    fun responseSupportsStandardMp4AsWellAsHls() {
        val mp4 = "https://example.cdn-apple.com/square.mp4"
        assertEquals(listOf(mp4), AppleMusicAnimatedArtworkLoader.animatedUrls(
            JSONObject().put("url", mp4).put("url_tall", "none")))
    }

    @Test
    fun knownAlbumSuccessDoesNotDependOnCatalogAvailability() = runBlocking {
        val result = resolveAnimatedArtwork(
            metadata, metadata, "ID",
            searchAlbum = { ArtworkReply(stream) },
            searchCatalog = { _, _ -> error("Catalog must not delay a working album result") },
            loadAlbumUrl = { error("Direct lookup is unnecessary") }
        )
        assertEquals(stream, result.url)
    }

    @Test
    fun failedCatalogLinkStillTriesTheAlbumFallback() = runBlocking {
        val requests = mutableListOf<ArtworkAlbumQuery>()
        val album = AppleCatalogSong("Song", "Singer", "Album", "https://music.apple.com/id/album/123")
        val result = resolveAnimatedArtwork(
            metadata, metadata, "ID",
            searchAlbum = { query ->
                requests += query
                if (query.album == "Album") ArtworkReply(stream) else ArtworkReply()
            },
            searchCatalog = { _, _ -> CatalogReply(listOf(album)) },
            loadAlbumUrl = { ArtworkReply(failed = true) }
        )
        assertEquals(stream, result.url)
        assertTrue(requests.any { it.album == "Album" })
    }

    @Test
    fun changedEmbeddedTagsDoNotDiscardThePreviouslyWorkingLibraryMetadata() = runBlocking {
        val library = metadata.copy(album = "Original album")
        val result = resolveAnimatedArtwork(
            metadata, library, "US",
            searchAlbum = { query ->
                if (query.album == library.album) ArtworkReply(stream) else ArtworkReply(failed = true)
            },
            searchCatalog = { _, _ -> error("Working library tags should resolve first") },
            loadAlbumUrl = { error("Direct lookup is unnecessary") }
        )
        assertEquals(stream, result.url)
    }

    @Test
    fun temporaryErrorsRemainRetryableWhenNoRouteSucceeds() = runBlocking {
        val result = resolveAnimatedArtwork(
            metadata, metadata, "ID",
            searchAlbum = { ArtworkReply() },
            searchCatalog = { _, _ -> CatalogReply(failed = true) },
            loadAlbumUrl = { error("No catalog match exists") }
        )
        assertEquals(null, result.url)
        assertTrue(result.failed)
    }
}
