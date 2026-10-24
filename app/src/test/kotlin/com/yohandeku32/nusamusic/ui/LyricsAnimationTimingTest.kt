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
        assertTrue(voices.presence(2080, 0) > 0.8f)
        assertTrue(voices.presence(2190, 0) in 0.4f..0.6f)
        assertEquals(0f, voices.presence(2380, 0), 0f)
        assertEquals(0f, voices.presence(3000, 0), 0f)
        assertTrue(voices.presence(4100, 0) > 0f)
        assertEquals(0f, voices.presence(6000, 0), 0f)
    }
    @Test fun backingVocalEntersAndLeavesWithoutFastPops() {
        val voice = KaraokeLine(
            listOf(KaraokeSyllable("Ooh", 1000, 3000)),
            null, true, KaraokeAlignment.Start, 1000, 3000
        )
        val windows = LyricsVocalWindows(voice)
        assertEquals(0f, windows.presence(1000, 0), 0f)
        assertTrue(windows.presence(1050, 0) < 0.08f)
        assertTrue(windows.presence(1250, 0) in 0.45f..0.65f)
        assertEquals(1f, windows.presence(1420, 0), 0f)
        assertTrue(windows.presence(3190, 0) in 0.45f..0.55f)
        assertEquals(0f, windows.presence(3380, 0), 0f)
    }
    @Test fun ordinaryWordWaveIsBoundedAndSmooth() {
        assertEquals(0f, lyricPassingLiftDp(0f), 0f)
        assertEquals(-0.9f, lyricPassingLiftDp(0.5f), 0.001f)
        assertEquals(-1.8f, lyricPassingLiftDp(1f), 0.001f)
        var previous = 0f
        for (i in 0..100) {
            val value = lyricPassingLiftDp(i / 100f)
            assertTrue(value in -1.8f..0f)
            assertTrue(kotlin.math.abs(value - previous) < 0.05f)
            previous = value
        }
    }
    @Test fun backingPreviewBeginsBeforeVocalButKeepsAuthoredHighlightTime() {
        val vocal = KaraokeLine(
            listOf(KaraokeSyllable("Hello", 5000, 6100)),
            null, true, KaraokeAlignment.Start, 5000, 6100
        )
        val windows = LyricsVocalWindows(vocal)
        assertEquals(0f, windows.previewPresence(4299, 0), 0f)
        assertEquals(0f, windows.previewPresence(4300, 0), 0f)
        assertTrue(windows.previewPresence(4650, 0) in 0.49f..0.51f)
        assertEquals(1f, windows.previewPresence(4999, 0), 0.0001f)
        assertEquals(1f, windows.previewPresence(5000, 0), 0f)
        assertEquals(1f, windows.previewPresence(5800, 0), 0f)
        assertTrue(windows.previewPresence(6290, 0) in 0.49f..0.51f)
        assertEquals(0f, windows.previewPresence(6480, 0), 0f)
        assertEquals(0f, vocal.syllables.single().progress(4650), 0f)
        assertEquals(0f, vocal.syllables.single().progress(4999), 0f)
    }

    @Test fun backingPreviewCrossesSmallGapsWithoutVisiblePops() {
        val vocal = KaraokeLine(listOf(
            KaraokeSyllable("One", 1000, 1200),
            KaraokeSyllable("Two", 1750, 2050)
        ), null, true, KaraokeAlignment.Start, 1000, 2050)
        val windows = LyricsVocalWindows(vocal)
        var last = 0f
        for (ms in 300..2500 step 10) {
            val current = windows.previewPresence(ms, 0)
            assertTrue(current in 0f..1f)
            assertTrue("Backing preview jumped at ${ms}ms",
                kotlin.math.abs(current - last) < 0.09f)
            last = current
        }
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
    @Test fun eachWordMovesOnlyWithinItsOwnTimestampWindow() {
        val motion = LyricsTimedWordPulse(listOf(
            LyricsHeldWordUnit("Aku ", 1000, 1400, 0f, 46f),
            LyricsHeldWordUnit("suka ", 1400, 1950, 48f, 105f),
            LyricsHeldWordUnit("musik", 1950, 2500, 107f, 170f)
        ))
        assertEquals(0f, motion.influence(0, 999), 0f)
        assertTrue(motion.influence(0, 1250) > 0.95f)
        assertEquals(0f, motion.influence(1, 1250), 0f)
        assertEquals(0f, motion.influence(2, 1250), 0f)
        assertTrue(motion.influence(1, 1700) > 0.95f)
        assertEquals(0f, motion.influence(0, 1700), 0f)
        assertTrue(motion.influence(2, 2150) > 0.95f)
        assertEquals(0f, motion.influence(1, 2150), 0f)
        assertEquals(0f, motion.influence(2, 2700), 0f)
    }

    @Test fun syllablesWithinOneWordShareTheirGentleEnvelope() {
        val pulse = LyricsTimedWordPulse(listOf(
            LyricsHeldWordUnit("ber", 1000, 1300, 0f, 25f),
            LyricsHeldWordUnit("sa", 1300, 1600, 25f, 49f),
            LyricsHeldWordUnit("ma ", 1600, 1850, 49f, 80f),
            LyricsHeldWordUnit("kita", 1850, 2200, 90f, 130f)
        ))
        for (time in 1000..2050 step 25) {
            assertEquals(pulse.influence(0, time), pulse.influence(1, time), 0f)
            assertEquals(pulse.influence(1, time), pulse.influence(2, time), 0f)
            assertTrue(pulse.influence(0, time) in 0f..1f)
        }
        assertEquals(0f, pulse.influence(3, 1200), 0f)
    }

    @Test fun shortWordHasContinuousArrivalAndReleaseWithoutBounce() {
        val pulse = LyricsTimedWordPulse(
            listOf(LyricsHeldWordUnit("Ya", 1000, 1130, 0f, 35f))
        )
        assertEquals(0f, pulse.influence(0, 1000), 0f)
        assertTrue(pulse.influence(0, 1070) > 0f)
        val atEnd = pulse.influence(0, 1130)
        val easingOut = pulse.influence(0, 1230)
        assertTrue(atEnd > easingOut)
        assertTrue(easingOut > 0f)
        assertEquals(0f, pulse.influence(0, 1330), 0f)
        for (t in 1000..1330 step 4) {
            assertTrue(kotlin.math.abs(pulse.influence(0, t + 4) -
                pulse.influence(0, t)) < 0.09f)
        }
    }

}
