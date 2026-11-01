package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertEquals
import org.json.JSONObject

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

    @Test fun acceptsOnlyCanonicalSpotycovsCanvasStreams() {
        val trackId = "4iZ4pt7kvcaH6Yo8UoZ4s2"
        assertTrue(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://www.spotycovs.lol/api/canvas?id=$trackId&stream=1"
        ))
        assertTrue(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://spotycovs.lol/api/canvas?stream=1&id=$trackId"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://www.spotycovs.lol/api/canvas?id=$trackId"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://www.spotycovs.lol/api/canvas?id=$trackId&stream=1&url=https://evil.example"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://www.spotycovs.lol.evil.example/api/canvas?id=$trackId&stream=1"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "https://www.spotycovs.lol/api/canvas?id=invalid&stream=1"
        ))
        assertFalse(SpotifyCanvasArtworkLoader.isSpotifyCanvasVideo(
            "http://www.spotycovs.lol/api/canvas?id=$trackId&stream=1"
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

    @Test fun preservesChineseJapaneseAndKoreanTitlesAndArtistNames() {
        for (title in listOf("平行线", "夜に駆ける", "사랑해")) {
            assertTrue(title, SpotifyCanvasArtworkLoader.similarTitle(title, title))
        }
        assertTrue(SpotifyCanvasArtworkLoader.similarArtist("黃霄雲", "黃霄雲, Willim"))
        assertFalse(SpotifyCanvasArtworkLoader.similarTitle("平行线", "夜に駆ける"))
    }

    @Test fun directSpotifyVideoWinsOverAnEquivalentProxyWhenBothAreAvailable() {
        val proxy = "https://www.spotycovs.lol/api/canvas?id=4iZ4pt7kvcaH6Yo8UoZ4s2&stream=1"
        val direct = "https://canvaz.scdn.co/upload/artist/clip.cnvs.mp4"
        val reply = JSONObject().put("canvasUrl", proxy).put("videoUrl", direct)
        assertEquals(direct, SpotifyCanvasArtworkLoader.canvasVideoUrl(reply))
        assertEquals(proxy, SpotifyCanvasArtworkLoader.canvasVideoUrl(JSONObject().put("canvasUrl", proxy)))
        assertEquals("A direct URL in another candidate must not change the selected recording", proxy,
            SpotifyCanvasArtworkLoader.canvasVideoUrl(JSONObject().put("canvasUrl", proxy)
                .put("data", JSONObject().put("videoUrl", direct))))
    }

    @Test fun meaningfulNonLatinMarksRemainDistinctWhileLatinAccentsStillMatch() {
        assertFalse(SpotifyCanvasArtworkLoader.similarTitle("かみ", "がみ"))
        assertFalse(SpotifyCanvasArtworkLoader.similarTitle("पल", "पाल"))
        assertTrue(SpotifyCanvasArtworkLoader.similarArtist("Beyoncé", "Beyonce"))
    }
}
