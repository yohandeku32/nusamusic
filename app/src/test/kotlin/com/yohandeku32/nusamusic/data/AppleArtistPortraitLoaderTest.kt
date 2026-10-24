package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleArtistPortraitLoaderTest {
    @Test fun artistNamesResolveToExplicitAppleMusicPortraitQueries() {
        val url = buildAppleMusicPortraitUrl("Daft Punk")
        assertEquals(
            "https://unavatar.io/apple-music/artist%3ADaft%20Punk?fallback=false",
            url
        )
    }

    @Test fun collaborationSeparatorUsesPrimaryArtistAndEscapesCharacters() {
        val uri = buildAppleMusicPortraitUrl("AC/DC feat. Guest")!!
        assertTrue(uri.startsWith("https://unavatar.io/apple-music/artist%3AAC%2FDC"))
        assertTrue(uri.endsWith("?fallback=false"))
    }

    @Test fun neverRequestPlaceholderForUnknownArtist() {
        assertNull(buildAppleMusicPortraitUrl(""))
        assertNull(buildAppleMusicPortraitUrl("Unknown Artist"))
        assertNull(buildAppleMusicPortraitUrl("x"))
    }
}
