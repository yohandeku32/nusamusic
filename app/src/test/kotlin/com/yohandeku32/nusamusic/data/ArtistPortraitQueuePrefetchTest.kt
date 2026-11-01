package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistPortraitQueuePrefetchTest {
    @Test fun includesCurrentAndSixUpcomingInQueueOrder() {
        val artists = listOf("Current Artist", "Next One", "Next Two", "Next Three",
            "Next Four", "Next Five", "Next Six")
        assertEquals(artists, portraitPrefetchArtists(artists))
    }

    @Test fun skipsDuplicateArtistsAndUnusableTags() {
        val artists = listOf("Daft Punk", "Daft Punk feat. Guest", "Unknown Artist",
            "", "AC/DC", "AC/DC", "Four Tet")
        assertEquals(listOf("Daft Punk", "AC/DC", "Four Tet"),
            portraitPrefetchArtists(artists))
    }

    @Test fun prefetchIsBoundedToSevenDistinctArtistImages() {
        val artists = (1..20).map { "Artist $it" }
        assertEquals(artists.take(7), portraitPrefetchArtists(artists))
    }
}
