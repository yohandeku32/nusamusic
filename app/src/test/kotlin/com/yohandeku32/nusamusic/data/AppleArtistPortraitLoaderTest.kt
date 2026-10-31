package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.IOException

class AppleArtistPortraitLoaderTest {
    @Test fun artistNamesResolveToAppleArtistCatalogQueries() {
        val url = buildAppleMusicPortraitUrl("Daft Punk")
        assertEquals(
            "https://itunes.apple.com/search?entity=musicArtist&media=music&attribute=artistTerm&limit=10&country=US&term=Daft+Punk",
            url
        )
    }

    @Test fun collaborationSeparatorUsesPrimaryArtistAndEscapesCharacters() {
        val uri = buildAppleMusicPortraitUrl("AC/DC feat. Guest")!!
        assertTrue(uri.endsWith("&term=AC%2FDC"))
    }

    @Test fun neverRequestPlaceholderForUnknownArtist() {
        assertNull(buildAppleMusicPortraitUrl(""))
        assertNull(buildAppleMusicPortraitUrl("Unknown Artist"))
        assertNull(buildAppleMusicPortraitUrl("x"))
    }

    @Test fun firstSingerOwnsLookupForEverySupportedSeparator() {
        for (separator in listOf(" feat. ", " ft. ", " featuring ", " & ", "; ", ", ", " / ", " x ")) {
            assertEquals(buildAppleMusicPortraitUrl("First Singer"), buildAppleMusicPortraitUrl("First Singer${separator}Second Singer"))
        }
    }

    @Test fun catalogSkipsTributesSongsAndSimilarNamesAndKeepsCanonicalArtistLink() {
        val catalog = """{"results":[
            {"wrapperType":"artist","artistName":"LINKIN PARK Tribute","artistId":1},
            {"wrapperType":"track","artistName":"LINKIN PARK","artistId":2},
            {"wrapperType":"artist","artistName":"LINKIN PARK","artistId":148662,
             "artistLinkUrl":"https://music.apple.com/us/artist/linkin-park/148662?uo=4"}]}"""
        assertEquals("https://music.apple.com/us/artist/linkin-park/148662", appleArtistPage(catalog, "Linkin Park feat. Guest"))
        assertNull(appleArtistPage(catalog, "Someone Else"))
    }

    @Test fun unicodeNamesMatchWithoutCollapsingDifferentArtists() {
        val catalog = """{"results":[{"wrapperType":"artist","artistName":"周杰倫","artistId":3}]}"""
        assertEquals("https://music.apple.com/us/artist/3", appleArtistPage(catalog, "周杰倫"))
        assertNull(appleArtistPage(catalog, "林俊傑"))
    }

    @Test fun portraitComesOnlyFromTheMatchingArtistStructuredData() {
        val html = """<meta property="og:image" content="https://is1-ssl.mzstatic.com/image/album.jpg">
            <script type="application/ld+json">{"@type":"MusicAlbum","name":"First Singer","image":"https://is1-ssl.mzstatic.com/image/album/486x486bb.png"}</script>
            <script type="application/ld+json">{"@type":"MusicGroup","name":"Other Singer","image":"https://is1-ssl.mzstatic.com/image/other/486x486bb.png"}</script>
            <script type="application/ld+json">{"@type":"MusicGroup","name":"First Singer","image":"https://is1-ssl.mzstatic.com/image/artist/486x486bb.png"}</script>"""
        assertEquals("https://is1-ssl.mzstatic.com/image/artist/600x600bb.jpg", appleArtistImageFromPage(html, "First Singer & Second Singer"))
        assertNull(appleArtistImageFromPage(html, "No Match"))
    }

    @Test fun rejectsNonAppleImagesAndUntrustedUrls() {
        for (url in listOf("http://is1-ssl.mzstatic.com/image/a.jpg", "https://is1-ssl.mzstatic.com.evil.org/image/a.jpg",
            "https://user@is1-ssl.mzstatic.com/image/a.jpg", "https://is1-ssl.mzstatic.com:8443/image/a.jpg",
            "https://music.apple.com/image/a.jpg")) assertNull(applePortraitImage(url))
    }

    @Test fun rateLimitsAndMalformedRepliesRemainRetryableInsteadOfArtistMisses() = runBlocking {
        for (response in listOf(PortraitHttpResponse(429, byteArrayOf()), PortraitHttpResponse(200, "not json".toByteArray()))) {
            var failed = false
            try { resolveAppleArtistPortrait("First Singer") { response } } catch (_: Exception) { failed = true }
            assertTrue(failed)
        }
        try {
            resolveAppleArtistPortrait("First Singer") { throw IOException("offline") }
            throw AssertionError("Expected network error")
        } catch (_: IOException) { }
    }
}
