package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class SyllableTimingTest {
    @Test fun authoredSyllablesAndSilencesRemainUnchanged() {
        val line = KaraokeLine(listOf(KaraokeSyllable("hel", 1_000, 2_000), KaraokeSyllable("lo", 4_000, 5_000)),
            "halo", true, KaraokeAlignment.End, 1_000, 5_000)
        assertEquals(1f, syllableProgress(line.syllables[0], 3_000), 0f)
        assertEquals(0f, syllableProgress(line.syllables[1], 3_000), 0f)
    }
    @Test fun overlappingSyllablesHaveIndependentProgress() {
        val first = KaraokeSyllable("one", 1_000, 3_000)
        val second = KaraokeSyllable("two", 2_000, 4_000)
        assertEquals(0.75f, syllableProgress(first, 2_500), 0.001f)
        assertEquals(0.25f, syllableProgress(second, 2_500), 0.001f)
    }
    @Test fun progressResetsOnBackwardSeekAndHasSafeBoundaries() {
        val syllable = KaraokeSyllable("word", 1_000, 2_000)
        assertEquals(0f, syllableProgress(syllable, 999), 0f)
        assertEquals(0f, syllableProgress(syllable, 1_000), 0f)
        assertEquals(1f, syllableProgress(syllable, 2_000), 0f)
        assertEquals(0.5f, syllableProgress(syllable, 1_500), 0f)
        assertEquals(0f, syllableProgress(syllable, 0), 0f)
    }
    @Test fun zeroDurationAndExtremeTimestampsNeverProduceNonFiniteProgress() {
        assertEquals(1f, syllableProgress(KaraokeSyllable("zero", 100, 100), 100), 0f)
        assertEquals(1f, syllableProgress(KaraokeSyllable("last", Int.MAX_VALUE - 1, Int.MAX_VALUE), Int.MAX_VALUE), 0f)
    }
}
