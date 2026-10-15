package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CdCarouselIndexTest {
    @Test
    fun fiveSongsHaveTwoDistinctNeighboursOnEachSideEvenAtTheFirstSong() {
        val center = initialCdCarouselIndex(0, 5)
        assertEquals(listOf(3, 4, 0, 1, 2), (-2..2).map { (center + it) % 5 })
    }

    @Test
    fun lastSongKeepsItsFollowingNeighbours() {
        val center = initialCdCarouselIndex(6, 7)
        assertEquals(listOf(4, 5, 6, 0, 1), (-2..2).map { (center + it) % 7 })
    }

    @Test
    fun aSmallLibraryDoesNotRepeatItsSongsToFillFiveSlots() {
        for (count in 1..4) {
            for (song in 0 until count) {
                assertEquals(song, initialCdCarouselIndex(song, count))
                assertEquals(song, nearestCdCarouselIndex(song, count, 50_000))
            }
        }
    }

    @Test
    fun selectingASongUsesTheNearestCycleInsteadOfJumpingAcrossTheList() {
        val around = initialCdCarouselIndex(9, 10)
        val next = nearestCdCarouselIndex(0, 10, around)
        assertEquals(around + 1, next)
        assertEquals(0, next % 10)
    }

    @Test
    fun candidatesStayInsideTheVirtualListAtBothBoundaries() {
        for (around in listOf(0, 1, Int.MAX_VALUE - 2, Int.MAX_VALUE - 1)) {
            for (song in 0 until 7) {
                val index = nearestCdCarouselIndex(song, 7, around)
                assertTrue(index in 0 until Int.MAX_VALUE)
                assertEquals(song, index % 7)
                assertTrue(abs(index.toLong() - around) < 7)
            }
        }
    }
}
