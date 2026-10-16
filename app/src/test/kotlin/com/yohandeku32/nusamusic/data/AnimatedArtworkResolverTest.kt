package com.yohandeku32.nusamusic.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedArtworkResolverTest {
    @Test
    fun portraitArtworkIsPreferredAndSquareRemainsAFallback() {
        assertEquals(listOf("tall", "square"), preferredAnimatedArtworkUrls("tall", "square"))
        assertEquals(listOf("square"), preferredAnimatedArtworkUrls(null, "square"))
        assertEquals(listOf("square"), preferredAnimatedArtworkUrls("null", "square"))
        assertEquals(listOf("tall"), preferredAnimatedArtworkUrls("tall", "tall"))
        assertEquals(emptyList<String>(), preferredAnimatedArtworkUrls(" ", null))
    }
    private val metadata = ArtworkLookupMetadata("Song", "Singer", "Album (Deluxe Edition)")
    private val stream = "https://example.mzstatic.com/artwork.m3u8"

    @Test
    fun failedTallArtworkFallsBackToSquareWithoutSelectingTheBrokenStreamAgain() {
        val tall = "https://example.mzstatic.com/tall.m3u8"
        val original = ArtworkReply(tall, alternatives = listOf(tall, stream))
        val square = original.afterPlaybackFailure(tall)
        assertEquals(stream, square.url)
        assertTrue(square.alternatives.isEmpty())
        val exhausted = square.afterPlaybackFailure(stream)
        assertEquals(null, exhausted.url)
        assertTrue(exhausted.failed)
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
