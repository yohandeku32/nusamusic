package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class LyricsDisplayRowsTest {
    private fun main(start: Int) = SyncedLine("Main", null, start, start + 5000)
    private fun backing(start: Int = 1000, end: Int = 3000) = KaraokeLine(
        listOf(KaraokeSyllable("Back", start, end)), null, true, KaraokeAlignment.Start, start, end)

    @Test fun backingBelongsBelowTheRelatedMainWithoutPermanentLazySlots() {
        val back = backing()
        val rows = LyricsDisplayRows(listOf(main(0), back, main(2000), backing(2500, 4000)))
        assertEquals(2, rows.rows.size)
        assertEquals(listOf(back), rows.rows[0].backing.map { it.value })
        assertArrayEquals(intArrayOf(0, 0, 1, 1), rows.lineToRow)
    }
    @Test fun simultaneousAndLeadingBackingDoNotLoseTheirMapping() {
        val rows = LyricsDisplayRows(listOf(backing(0, 500), main(1000), backing(1000, 2000)))
        assertEquals(1, rows.rows.size)
        assertEquals(2, rows.rows.single().backing.size)
        assertArrayEquals(intArrayOf(0, 0, 0), rows.lineToRow)
        assertEquals(1, LyricsDisplayRows(listOf(backing())).rows.size)
        assertTrue(LyricsDisplayRows(emptyList()).rows.isEmpty())
    }
    @Test fun backingPushesNextRowDownAndReclaimsAllPaddingAfterExit() {
        val voice = LyricsVocalWindows(backing())
        val nextTop: (Int) -> Int = { now -> 60 + vocalLayoutHeight(48, voice.presence(now, 0)) }
        assertEquals(60, nextTop(900))
        assertTrue(nextTop(1060) in 61..107)
        assertEquals(108, nextTop(1500))
        assertTrue(nextTop(3080) in 61..107)
        assertEquals(60, nextTop(3160))
        assertEquals(60, nextTop(9000))
    }
    @Test fun layoutFreezeAndSeekDoNotReplayOldExpansion() {
        val voices = LyricsVocalWindows(backing())
        val frozen = vocalLayoutHeight(48, voices.presence(1060, 0))
        repeat(120) { assertEquals(frozen, vocalLayoutHeight(48, voices.presence(1060, 0))) }
        assertEquals(48, vocalLayoutHeight(48, voices.presence(1060, 1060)))
        assertEquals(0, vocalLayoutHeight(48, voices.presence(3080, 3080)))
        assertEquals(0, vocalLayoutHeight(48, voices.presence(900, 900)))
    }
    @Test fun backingHeightChangesContinuouslyAt60_90_120Hz() {
        val voices = LyricsVocalWindows(backing())
        for (hz in listOf(60, 90, 120)) {
            var previous = 0
            for (frame in 0..(hz * 4)) {
                val now = (frame * 1000.0 / hz).toInt()
                val height = vocalLayoutHeight(48, voices.presence(now, 0))
                assertTrue("Frame jump at $hz Hz", kotlin.math.abs(height - previous) <= 11)
                previous = height
            }
            assertEquals(0, previous)
        }
    }

    @Test fun identicalBackingMetadataStillHasDistinctStableSourceKeys() {
        val back = backing()
        val rows = LyricsDisplayRows(listOf(main(0), back, back))
        assertEquals(listOf(1, 2), rows.rows.single().backing.map { it.index })
        assertEquals(listOf(back, back), rows.rows.single().backing.map { it.value })
    }
}
