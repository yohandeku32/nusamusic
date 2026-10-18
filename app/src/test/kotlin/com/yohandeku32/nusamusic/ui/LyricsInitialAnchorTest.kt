package com.yohandeku32.nusamusic.ui

import org.junit.Assert.*
import org.junit.Test

class LyricsInitialAnchorTest {
    @Test fun aTallWrappedFirstLineKeepsItsTopPadding() {
        val anchor = lyricsFollowAnchor(-100, 100, true, mainHeight = 180f, minimumPadding = 24f)
        assertTrue(anchor + 100f - 180f / 2f >= 24f)
        assertEquals(0f, lyricsFollowAnchor(-100, 100, false, 180f, 24f), 0f)
    }

    @Test fun firstVocalIsLiftedByTwelvePointFivePercentRegardlessOfContentPadding() {
        for (height in listOf(200, 400, 800)) {
            for (padding in listOf(0, height / 2)) {
                val first = lyricsFollowAnchor(-padding, height - padding, true)
                val normal = lyricsFollowAnchor(-padding, height - padding, false)
                assertEquals(height * 0.125f, normal - first, 0.0001f)
                assertTrue(first + padding > height * 0.3f)
            }
        }
    }

    @Test fun returningToTheFirstRowUsesTheSameAnchorAndLaterRowsKeepTheirCentre() {
        val initial = lyricsFollowAnchor(-200, 200, true)
        assertEquals(0f, lyricsFollowAnchor(-200, 200, false), 0f)
        assertEquals(initial, lyricsFollowAnchor(-200, 200, true), 0f)
        assertEquals(-50f, initial, 0f)
    }
}
