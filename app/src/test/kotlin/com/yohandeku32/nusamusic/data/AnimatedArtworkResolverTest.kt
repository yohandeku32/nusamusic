package com.yohandeku32.nusamusic.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class AnimatedArtworkResolverTest {
    @Test
    fun portraitSelectionTrimsAndRejectsPlaceholders() {
        assertEquals(listOf("portrait"), preferredAnimatedArtworkUrls("portrait"))
        assertEquals(emptyList<String>(), preferredAnimatedArtworkUrls(null))
        assertEquals(emptyList<String>(), preferredAnimatedArtworkUrls("null"))
        assertEquals(emptyList<String>(), preferredAnimatedArtworkUrls("NONE"))
        assertEquals(emptyList<String>(), preferredAnimatedArtworkUrls(" "))
        assertEquals(listOf("https://apple.com/tall.m3u8"),
            preferredAnimatedArtworkUrls("  https://apple.com/tall.m3u8  "))
    }
    private val metadata = ArtworkLookupMetadata("Song", "Singer", "Album (Deluxe Edition)")
    private val stream = "https://example.mzstatic.com/artwork.m3u8"

    @Test
    fun failedPortraitDoesNotFallBackToTheSquareVideo() {
        val square = "https://example.mzstatic.com/square.m3u8"
        val tall = "https://example.mzstatic.com/tall.m3u8"
        val urls = AppleMusicAnimatedArtworkLoader.animatedUrls(JSONObject()
            .put("url", square).put("url_tall", tall))
        val original = ArtworkReply(urls.first(), alternatives = urls.drop(1))
        assertEquals(tall, original.url)
        val exhausted = original.afterPlaybackFailure(tall)
        assertEquals(null, exhausted.url)
        assertTrue(exhausted.alternatives.isEmpty())
        assertTrue(exhausted.failed)
    }

    @Test
    fun squareOnlyAlbumContinuesSearchingForPortraitArtwork() = runBlocking {
        val square = "https://example.mzstatic.com/square.m3u8"
        val tall = "https://example.mzstatic.com/tall.m3u8"
        val otherAlbum = AppleCatalogSong("Song", "Singer", "Alternate Album",
            "https://music.apple.com/us/album/7654321")
        val result = resolveAnimatedArtwork(
            metadata, metadata, "US",
            searchAlbum = {
                val urls = AppleMusicAnimatedArtworkLoader.animatedUrls(JSONObject().put("url", square))
                ArtworkReply(urls.firstOrNull(), alternatives = urls.drop(1))
            },
            searchCatalog = { _, _ -> CatalogReply(listOf(otherAlbum)) },
            loadAlbumUrl = { ArtworkReply(tall) }
        )
        assertEquals(tall, result.url)
    }

    @Test
    fun responseAcceptsOnlyThePortraitField() {
        val square = "https://example.mzstatic.com/square.m3u8"
        val tall = "https://example.mzstatic.com/tall.m3u8"
        assertEquals(listOf(tall), AppleMusicAnimatedArtworkLoader.animatedUrls(
            JSONObject().put("url", square).put("url_tall", tall)))
        assertEquals(listOf(square), AppleMusicAnimatedArtworkLoader.animatedUrls(
            JSONObject().put("url", " $square ").put("url_tall", square)))
    }

    @Test
    fun invalidPortraitDoesNotUseAnAvailableSquareVideo() {
        val tall = "https://example.mzstatic.com/tall.m3u8"
        for (invalid in listOf("null", "NONE", "", "http://a.mzstatic.com/a.m3u8",
            "https://mzstatic.com.evil.example/a.m3u8", "file:///a.mp4")) {
            assertTrue(AppleMusicAnimatedArtworkLoader.animatedUrls(
                JSONObject().put("url", tall).put("url_tall", invalid)).isEmpty())
        }
        assertTrue(AppleMusicAnimatedArtworkLoader.animatedUrls(null).isEmpty())
        assertTrue(AppleMusicAnimatedArtworkLoader.animatedUrls(JSONObject()).isEmpty())
    }

    @Test
    fun portraitMp4IsAcceptedWhileSquareOnlyMp4IsExcluded() {
        val mp4 = "https://example.cdn-apple.com/square.mp4"
        assertTrue(AppleMusicAnimatedArtworkLoader.animatedUrls(
            JSONObject().put("url", mp4).put("url_tall", "none")).isEmpty())
        assertEquals(listOf(mp4), AppleMusicAnimatedArtworkLoader.animatedUrls(
            JSONObject().put("url_tall", mp4)))
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
