package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppleArtistPortraitLoaderTest {
    @Test fun artistNamesResolveToExplicitAppleMusicPortraitQueries() {
        val url = AppleArtistPortraitLoader.artistUrl("Daft Punk")
        assertEquals(
            "https://unavatar.io/apple-music/artist%3ADaft%20Punk?fallback=false",
            url
        )
    }

    @Test fun collaborationSeparatorUsesPrimaryArtistAndEscapesCharacters() {
        val uri = AppleArtistPortraitLoader.artistUrl("AC/DC feat. Guest")!!
        assertTrue(uri.startsWith("https://unavatar.io/apple-music/artist%3AAC%2FDC"))
        assertTrue(uri.endsWith("?fallback=false"))
    }

    @Test fun neverRequestPlaceholderForUnknownArtist() {
        assertNull(AppleArtistPortraitLoader.artistUrl(""))
        assertNull(AppleArtistPortraitLoader.artistUrl("Unknown Artist"))
        assertNull(AppleArtistPortraitLoader.artistUrl("x"))
    }
}
