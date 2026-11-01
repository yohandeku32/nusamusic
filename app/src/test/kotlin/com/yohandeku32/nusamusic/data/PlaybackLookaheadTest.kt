package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackLookaheadTest {
    @Test fun linearOrderFetchesFiveAfterCurrentWithoutCurrentTrack() {
        assertEquals(listOf(1, 2, 3, 4, 5),
            upcomingPlaybackIndices(0, 9) { index -> if (index < 8) index + 1 else -1 })
    }

    @Test fun shuffleOrderFollowsTimelineNotLibraryIndices() {
        val order = listOf(3, 0, 5, 1, 6, 4, 2)
        val next = order.zipWithNext().toMap()
        assertEquals(listOf(5, 1, 6, 4, 2),
            upcomingPlaybackIndices(0, 7) { current -> next[current] ?: -1 })
    }

    @Test fun repeatAllWrapsWithoutDuplicatingCurrentSong() {
        val next: (Int) -> Int = { (it + 1) % 3 }
        assertEquals(listOf(2, 0), upcomingPlaybackIndices(1, 3, nextIndex = next))
    }

    @Test fun tinyQueuesHaveNoUnnecessaryPrefetch() {
        assertEquals(emptyList<Int>(), upcomingPlaybackIndices(0, 1) { -1 })
        assertEquals(emptyList<Int>(), upcomingPlaybackIndices(-1, 5) { it + 1 })
        assertEquals(emptyList<Int>(), upcomingPlaybackIndices(0, 5, limit = 0) { it + 1 })
    }

    @Test fun appleTallWinsEvenWhenSpotifyAlreadyResolves() {
        val preferred = AnimatedArtworkQueuePreloader.choose(
            "https://a.mzstatic.com/tall.m3u8", "https://canvas.example/canvas.mp4"
        )
        assertEquals(AnimatedArtworkQueuePreloader.Provider.APPLE_TALL, preferred?.provider)
        assertEquals(AnimatedArtworkQueuePreloader.Provider.SPOTIFY_CANVAS,
            AnimatedArtworkQueuePreloader.choose(null, "spotify")?.provider)
        assertNull(AnimatedArtworkQueuePreloader.choose(null, null))
    }

    @Test fun sourceOrderFollowsUserSelectionWithoutLosingFallback() {
        val apple = "https://a.mzstatic.com/tall.m3u8"
        val spotify = "https://canvaz.scdn.co/video.mp4"
        assertEquals(AnimatedArtworkQueuePreloader.Provider.SPOTIFY_CANVAS,
            AnimatedArtworkQueuePreloader.choose(apple, spotify, ArtworkSourceOrder.SPOTIFY_FIRST)?.provider)
        assertEquals(AnimatedArtworkQueuePreloader.Provider.APPLE_TALL,
            AnimatedArtworkQueuePreloader.choose(apple, spotify, ArtworkSourceOrder.APPLE_FIRST)?.provider)
        assertEquals(AnimatedArtworkQueuePreloader.Provider.APPLE_TALL,
            AnimatedArtworkQueuePreloader.choose(apple, spotify, ArtworkSourceOrder.APPLE_ONLY)?.provider)
        assertEquals(AnimatedArtworkQueuePreloader.Provider.SPOTIFY_CANVAS,
            AnimatedArtworkQueuePreloader.choose(apple, spotify, ArtworkSourceOrder.SPOTIFY_ONLY)?.provider)
        assertNull(AnimatedArtworkQueuePreloader.choose(null, spotify, ArtworkSourceOrder.APPLE_ONLY))
        assertNull(AnimatedArtworkQueuePreloader.choose(apple, null, ArtworkSourceOrder.SPOTIFY_ONLY))
        assertEquals(ArtworkSourceOrder.APPLE_FIRST, ArtworkSourceOrder.fromPreference("NOT_A_SETTING"))
        assertEquals(ArtworkSourceOrder.SPOTIFY_FIRST, ArtworkSourceOrder.fromPreference("SPOTIFY_FIRST"))
    }
    @Test fun exhaustedSpotifyFirstNeverReturnsToTheSameFailedCanvas() {
        assertEquals(true, AnimatedArtworkQueuePreloader.spotifyAfterAppleFailure(
            ArtworkSourceOrder.SPOTIFY_FIRST, spotifyAlreadyFailed = false))
        assertEquals(false, AnimatedArtworkQueuePreloader.spotifyAfterAppleFailure(
            ArtworkSourceOrder.SPOTIFY_FIRST, spotifyAlreadyFailed = true))
        assertEquals(true, AnimatedArtworkQueuePreloader.spotifyAfterAppleFailure(
            ArtworkSourceOrder.APPLE_FIRST, spotifyAlreadyFailed = false))
        assertEquals(false, AnimatedArtworkQueuePreloader.spotifyAfterAppleFailure(
            ArtworkSourceOrder.APPLE_ONLY, spotifyAlreadyFailed = false))
        assertEquals(false, AnimatedArtworkQueuePreloader.spotifyAfterAppleFailure(
            ArtworkSourceOrder.SPOTIFY_ONLY, spotifyAlreadyFailed = false))
    }

}
