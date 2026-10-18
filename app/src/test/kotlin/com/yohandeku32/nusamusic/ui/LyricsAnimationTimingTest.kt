package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import org.junit.Assert.*
import org.junit.Test

class LyricsAnimationTimingTest {
    @Test fun longNoteGlowKeepsMovingAfterItsOnsetHasSettled() {
        assertEquals(1f, syllableExpression(1000, 9000, 2500, 0), 0f)
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
    @Test fun motionSettlesOnceAndSeekDoesNotReplayItsAttack() {
        assertTrue(syllableExpression(1000, 8000, 1060, 0) in 0.01f..0.99f)
        assertEquals(1f, syllableExpression(1000, 8000, 1500, 0), 0f)
        assertEquals(1f, syllableExpression(1000, 8000, 1060, 1050), 0f)
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
        val playback = LyricsPlaybackState(0, 0)
        playback.publish(16, 0)
        assertEquals(0, playback.motion.revision)
        playback.publish(2000, 1)
        assertEquals(2000, playback.motion.resetAt)
        assertEquals(1f, syllableExpression(1900, 3000, 2000, playback.motion.resetAt), 0f)
        playback.publish(100, 2)
        assertEquals(100, playback.motion.resetAt)
        assertEquals(2, playback.motion.revision)
        assertEquals(0, LyricsMotionState(0).revision)
    }
    @Test fun criticallyDampedSyllableMotionSettlesWithoutRepeatedJumps() {
        var last = 0f
        for (now in 1000..1180) {
            val value = syllableExpression(1000, 9000, now, 0)
            assertTrue(value >= last && value <= 1f)
            last = value
        }
        assertEquals(1f, syllableExpression(1000, 9000, 4000, 0), 0f)
        assertEquals(1f, syllableExpression(1000, 9000, 8000, 0), 0f)
        assertEquals(0f, syllableExpression(1000, 9000, 9160, 0), 0f)
    }
    @Test fun longGlowBroadensAndSeekSetsExpressionImmediately() {
        assertTrue(syllableBloom(1000, 9000, 7000) > syllableBloom(1000, 9000, 2000))
        assertEquals(1f, syllableExpression(1000, 9000, 1060, 1060), 0f)
        assertEquals(0f, syllableExpression(1000, 9000, 9050, 9050), 0f)
        repeat(120) { assertEquals(syllableExpression(1000, 9000, 1060, 0),
            syllableExpression(1000, 9000, 1060, 0), 0f) }
    }
}
