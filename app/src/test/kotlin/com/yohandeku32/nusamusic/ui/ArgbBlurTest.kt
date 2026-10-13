package com.yohandeku32.nusamusic.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgbBlurTest {
    @Test
    fun uniformCoverKeepsItsColourIncludingAtEdges() {
        val cover = IntArray(8 * 6) { 0xFFA06132.toInt() }
        assertArrayEquals(cover, blurArgb(cover, 8, 6))
    }

    @Test
    fun detailSpreadsToNeighboursWithoutChangingTheSource() {
        val cover = IntArray(9 * 9) { 0xFF000000.toInt() }
        cover[4 * 9 + 4] = 0xFFFFFFFF.toInt()
        val original = cover.copyOf()
        val blurred = blurArgb(cover, 9, 9, radius = 1, passes = 1)
        assertArrayEquals(original, cover)
        assertTrue((blurred[4 * 9 + 4] and 255) in 1..254)
        assertTrue((blurred[4 * 9 + 3] and 255) > 0)
        assertTrue((blurred[3 * 9 + 4] and 255) > 0)
    }

    @Test
    fun edgeDetailDoesNotWrapToTheOppositeSide() {
        val cover = IntArray(9 * 9) { 0xFF000000.toInt() }
        cover[4 * 9] = 0xFFFFFFFF.toInt()
        val blurred = blurArgb(cover, 9, 9, radius = 1, passes = 1)
        assertEquals(0xFF000000.toInt(), blurred[4 * 9 + 8])
        assertTrue((blurred[4 * 9] and 255) > 0)
    }
}
