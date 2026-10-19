package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyCanvasArtworkLoaderTest {
    @Test fun acceptsOnlyRealSpotifyCanvasMp4CdnUrls() {
        assertTrue(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://canvaz.scdn.co/upload/artist/abc/video/123.cnvs.mp4"
        ))
        assertTrue(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://canvaz.scdn.co/upload/licensor/abc/video/xyz.cnvs.mp4?token=123"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "http://canvaz.scdn.co/upload/artist/test.cnvs.mp4"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://canvaz.scdn.co.attacker.example/video.mp4"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://i.scdn.co/image/album.jpg"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://example.com/canvas.mp4"
        ))
    }

    @Test fun matchesSameTitleAfterRemovingVersionSuffixesButNotUnrelatedSongs() {
        assertTrue(SpotifyCanvasArtworkLoader.similarTitle(
            "Snooze", "Snooze (Original Version)"
        ))
        assertTrue(SpotifyCanvasArtworkLoader.similarTitle(
            "Hello", "Hello"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.similarTitle(
            "Someone Like You", "Set Fire to the Rain"
        ))
    }

    @Test fun matchesPrimaryArtistWithinMultiArtistMetadata() {
        assertTrue(SpotifyCanvasArtworkLoader.similarArtist(
            "SZA", "SZA, Phoebe Bridgers"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.similarArtist(
            "Taylor Swift", "The Weeknd"
        ))
    }
}
