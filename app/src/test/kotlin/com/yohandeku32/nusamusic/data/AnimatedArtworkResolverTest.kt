package com.yohandeku32.nusamusic.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedArtworkResolverTest {
    @Test
    fun tallArtworkIsTheOnlyAppleAnimationVariant() {
        assertEquals(listOf("tall"), preferredAnimatedArtworkUrls("tall"))
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
    fun failedTallArtworkNeverFallsBackToSquareVideo() {
        val tall = "https://example.mzstatic.com/tall.m3u8"
        // No square fallback is added to the result when only Tall is allowed.
        val original = ArtworkReply(tall)
        val exhausted = original.afterPlaybackFailure(tall)
        assertEquals(null, exhausted.url)
        assertTrue(exhausted.alternatives.isEmpty())
        assertTrue(exhausted.failed)
    }

    @Test
    fun resolverContinuesLookingForTallWhenFirstAlbumHasOnlySquare() = runBlocking {
        val tall = "https://example.mzstatic.com/tall.m3u8"
        val secondAlbum = AppleCatalogSong(
            "Song", "Singer", "Alternate Album", "https://music.apple.com/us/album/7654321"
        )
        val result = resolveAnimatedArtwork(
            metadata, metadata, "US",
            searchAlbum = { ArtworkReply() }, // Square-only response maps to no Tall URL.
            searchCatalog = { _, _ -> CatalogReply(listOf(secondAlbum)) },
            loadAlbumUrl = { ArtworkReply(tall) }
        )
        assertEquals(tall, result.url)
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
