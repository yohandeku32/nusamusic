package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import org.junit.Assert.*
import org.junit.Test

class LyricsAnimationTimingTest {
    @Test fun longNoteGlowKeepsMovingAfterItsOnsetHasSettled() {
        assertEquals(0f, lyricOnset(1000, 9000, 2500, 0), 0f)
        assertNotEquals(lyricBreathing(1000, 9000, 2500), lyricBreathing(1000, 9000, 4500), 0.001f)
        assertTrue(lyricShimmer(1000, 9000, 4500) > lyricShimmer(1000, 9000, 2500))
        assertEquals(0f, syllableGlowEnvelope(1000, 9000, 9000), 0f)
    }
    @Test fun pauseIsStableAndShortNotesDoNotRunAHeldNoteShimmer() {
        val phase = lyricShimmer(1000, 9000, 4500)
        val breath = lyricBreathing(1000, 9000, 4500)
        repeat(120) {
            assertEquals(phase, lyricShimmer(1000, 9000, 4500), 0f)
            assertEquals(breath, lyricBreathing(1000, 9000, 4500), 0f)
        }
        assertEquals(0f, lyricShimmer(1000, 1300, 1150), 0f)
    }
    @Test fun bounceOccursOnceAndSeekDoesNotReplayIt() {
        assertTrue(lyricOnset(1000, 8000, 1060, 0) > 0f)
        assertEquals(0f, lyricOnset(1000, 8000, 1500, 0), 0f)
        assertEquals(0f, lyricOnset(1000, 8000, 1060, 1050), 0f)
        assertEquals(1f, lyricPresence(1000, 8000, 1060, 1050), 0f)
        assertEquals(0f, lyricPresence(1000, 8000, 8050, 8050), 0f)
    }
    @Test fun countdownPrepositionsTheNextLineAndEndsWithoutAResidualGap() {
        val gap = LyricsGap(2000, 8000, 1)
        assertEquals(1f, countdownFollowOffset(gap, 7000), 0f)
        assertEquals(0.5f, countdownFollowOffset(gap, 7780), 0.001f)
        assertEquals(0f, countdownFollowOffset(gap, 8000), 0f)
        assertTrue(countdownOpacity(gap, 7500) > 0f)
        assertEquals(0f, countdownOpacity(gap, 8000), 0f)
        assertEquals(0f, countdownOpacity(gap, 9000), 0f)
    }
    @Test fun miniVoiceFadesOutInAuthoredSilenceAndReturnsForTheNextUnit() {
        val line = KaraokeLine(listOf(KaraokeSyllable("One", 1000, 2000), KaraokeSyllable(" ", 2000, 4000),
            KaraokeSyllable("Two", 4000, 5000)), "Translation", true, KaraokeAlignment.Start, 1000, 5000)
        val voices = LyricsVocalWindows(line)
        assertEquals(0f, voices.presence(900, 0), 0f)
        assertEquals(1f, voices.presence(1500, 0), 0f)
        assertTrue(voices.presence(2080, 0) in 0.4f..0.6f)
        assertEquals(0f, voices.presence(3000, 0), 0f)
        assertTrue(voices.presence(4100, 0) > 0f)
        assertEquals(0f, voices.presence(6000, 0), 0f)
    }
    @Test fun voiceWindowsRepairOrderingAndMergeOverlapsWithoutInventingUnits() {
        val line = KaraokeLine(listOf(KaraokeSyllable("later", 3000, 5000), KaraokeSyllable("early", 1000, 4000)),
            null, true, KaraokeAlignment.Start, 1000, 5000)
        val voices = LyricsVocalWindows(line)
        assertEquals(1f, voices.presence(2500, 0), 0f)
        assertEquals(0f, voices.presence(6000, 0), 0f)
    }
    @Test fun explicitSeekAndTrackClockResetInvalidateOldTransitions() {
        val motion = LyricsMotionState(0)
        motion.update(16)
        assertEquals(0, motion.revision)
        motion.reset(2000)
        assertEquals(2000, motion.resetAt)
        assertEquals(0f, lyricOnset(1900, 3000, 2000, motion.resetAt), 0f)
        motion.update(100)
        assertEquals(100, motion.resetAt)
        assertEquals(2, motion.revision)
        assertEquals(0, LyricsMotionState(0).revision)
    }
}
