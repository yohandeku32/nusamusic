package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.yohandeku32.nusamusic.data.LyricsDecoder
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class LyricsClosingVocalMotionTest {
    private val phrase = listOf(
        LyricsExpressionTiming(1000, 1600),
        LyricsExpressionTiming(1600, 1900),
        LyricsExpressionTiming(1900, 2200),
        LyricsExpressionTiming(2200, 8200)
    )
    private fun motion(timings: List<LyricsExpressionTiming> = phrase) =
        LyricsClosingVocalMotion(timings, lyricsAdaptiveExpressions(timings), 150f)

    @Test fun aHeldFinalSyllableCarriesTheWholeWordAndPhraseWithOneTransform() {
        val group = motion()
        group.update(4200)
        assertEquals(1f, group.weight, 0f)
        // All syllables, including earlier completed ones, use the shared
        // group transform. None keeps an isolated per-syllable pivot/lift.
        assertEquals(0f, group.localWeight, 0f)
        assertTrue(abs(group.offsetYDp) > 0.1f)
        assertTrue(group.scaleX > 1f)
        assertTrue(group.scaleY > 1f)
    }

    @Test fun earlierHeldWordsNeverMoveTheFutureClosingText() {
        val group = motion(listOf(
            LyricsExpressionTiming(1000, 6000),
            LyricsExpressionTiming(6500, 10500)))
        for (now in listOf(0, 1000, 3000, 6000, 6500)) {
            group.update(now)
            assertEquals(0f, group.weight, 0f)
            assertEquals(1f, group.localWeight, 0f)
            assertEquals(0f, group.offsetYDp, 0f)
        }
        group.update(7200)
        assertEquals(1f, group.weight, 0f)
    }

    @Test fun fastEndingsAndUncertainMetadataTailsKeepTheirExistingTreatment() {
        for (duration in listOf(40, 180, 350, 600, 700)) {
            val group = motion(listOf(LyricsExpressionTiming(1000, 1000 + duration)))
            group.update(1000 + duration / 2)
            assertEquals(0f, group.weight, 0f)
            assertEquals(1f, group.scaleX, 0f)
        }
        val group = motion(listOf(LyricsExpressionTiming(1000, 30000, reliableEnd = false)))
        group.update(10000)
        assertEquals(0f, group.weight, 0f)
        assertEquals(0f, group.offsetYDp, 0f)
    }

    @Test fun inheritedAndExplicitEndsDifferWithoutChangingHighlightProgress() {
        val inferred = LyricsDecoder.decodeWithTiming("[00:01]<00:01>ber<00:01.3>sa<00:01.6>ma", 10000)
        val explicit = LyricsDecoder.decodeWithTiming("[00:01]<00:01>ber<00:01.3>sa<00:01.6>ma<00:10>", 10000)
        for ((decoded, expectedWeight) in listOf(inferred to 0f, explicit to 1f)) {
            val line = decoded.lyrics.lines.single() as KaraokeLine
            val profiles = lyricsAdaptiveLineExpressions(listOf(line), decoded.inferredEnds).getValue(line)
            val group = LyricsClosingVocalMotion(line.syllables.map {
                LyricsExpressionTiming(it.start, it.end)
            }, profiles, 150f)
            group.update(5800)
            assertEquals(expectedWeight, group.weight, 0f)
            assertEquals(0.5f, line.syllables.last().progress(5800), 0.0001f)
            assertEquals(listOf(1000, 1300, 1600), line.syllables.map { it.start })
        }
    }

    @Test fun overlappingUnitsFinishBeforeTheWholePhraseTakesOver() {
        val group = motion(listOf(
            LyricsExpressionTiming(1000, 5000),
            LyricsExpressionTiming(3000, 9000)))
        group.update(4000)
        assertEquals(0f, group.weight, 0f)
        group.update(5500)
        assertEquals(1f, group.weight, 0f)
        // A previous unit outlasting the final onset leaves no closing hold.
        val noClosingWindow = motion(listOf(
            LyricsExpressionTiming(1000, 11000),
            LyricsExpressionTiming(3000, 9000)))
        noClosingWindow.update(6000)
        assertEquals(0f, noClosingWindow.weight, 0f)
    }

    @Test fun emptyOrDegenerateLyricsStayAtRest() {
        for (timings in listOf(emptyList(), listOf(LyricsExpressionTiming(1000, 1000)))) {
            val group = motion(timings)
            group.update(1000)
            assertEquals(0f, group.weight, 0f)
            assertEquals(1f, group.scaleY, 0f)
        }
    }

    @Test fun entirePhraseKeepsBreathingThroughoutALongHold() {
        val group = motion()
        val poses = (3000..7400 step 100).map { now ->
            group.update(now)
            assertEquals(0f, group.localWeight, 0f)
            group.offsetYDp to group.scaleY
        }
        assertTrue(poses.maxOf { it.first } - poses.minOf { it.first } > 3f)
        assertTrue(poses.maxOf { it.second } - poses.minOf { it.second } > 0.015f)
    }

    @Test fun sharedMotionEntersAndReleasesSmoothlyAt60_90_120Hz() {
        for (hz in listOf(60, 90, 120)) {
            val group = motion()
            var x = 0f
            var y = 0f
            var sx = 1f
            var sy = 1f
            for (frame in 0..hz * 9) {
                group.update((frame * 1000.0 / hz).toInt())
                assertTrue(group.scaleX in 1f..1.045f)
                assertTrue(group.scaleY in 1f..1.045f)
                assertTrue(abs(group.offsetXDp - x) < 0.05f)
                assertTrue("Whole-phrase lift jumped at $hz Hz", abs(group.offsetYDp - y) < 0.2f)
                assertTrue(abs(group.scaleX - sx) < 0.004f)
                assertTrue(abs(group.scaleY - sy) < 0.004f)
                x = group.offsetXDp
                y = group.offsetYDp
                sx = group.scaleX
                sy = group.scaleY
            }
            assertEquals(0f, group.weight, 0f)
            assertEquals(0f, group.offsetYDp, 0f)
        }
    }

    @Test fun pauseAndBackwardSeekReproduceTheSameSharedPose() {
        val group = motion()
        fun pose() = listOf(group.weight, group.offsetXDp, group.offsetYDp, group.scaleX, group.scaleY)
        group.update(4600)
        val expected = pose()
        repeat(60) {
            group.update(4600)
            assertEquals(expected, pose())
        }
        group.update(8100)
        group.update(9000)
        group.update(4600)
        assertEquals(expected, pose())
    }
}
