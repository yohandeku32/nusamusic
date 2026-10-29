package com.yohandeku32.nusamusic.ui

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.*
import org.junit.Test

class MiniLyricsTest {
    private fun line(text: String, start: Int, end: Int, backing: Boolean = false) = KaraokeLine(
        listOf(KaraokeSyllable(text, start, end)), null, backing, KaraokeAlignment.Start, start, end)

    @Test fun onlyTheCurrentMainLineAppearsBetweenItsAuthoredBoundaries() {
        val first = line("First", 1000, 2000)
        val second = line("Second", 2500, 3500)
        val mini = MiniLyricsTimeline(listOf(first, second))
        assertNull(mini.lineAt(999))
        assertSame(first, mini.lineAt(1000))
        assertSame(first, mini.lineAt(1999))
        assertNull(mini.lineAt(2000))
        assertNull(mini.lineAt(2499))
        assertSame(second, mini.lineAt(2500))
        assertNull(mini.lineAt(3500))
    }

    @Test fun overlappingDuetsUseOneStableAuthoredIdentityAndIgnoreBackingOnlyPassages() {
        val first = line("First", 1000, 4000)
        val second = line("Second", 2000, 3500)
        val simultaneous = line("Third", 2000, 3500)
        val background = line("Background", 4000, 6000, true)
        val mini = MiniLyricsTimeline(listOf(first, second, simultaneous, background))
        assertSame(first, mini.lineAt(1500))
        assertSame(second, mini.lineAt(2500))
        assertSame(first, mini.lineAt(3750))
        assertNull(mini.lineAt(4500))
        assertNull(MiniLyricsTimeline(listOf(background)).lineAt(4500))
    }

    @Test fun longInstrumentalGapsInsideOneLineRemainEmpty() {
        val line = line("Before", 1000, 2000).copy(end = 7000, syllables = listOf(
            KaraokeSyllable("Before", 1000, 2000), KaraokeSyllable("After", 6000, 7000)))
        val mini = MiniLyricsTimeline(listOf(line))
        assertSame(line, mini.lineAt(1500))
        assertNull(mini.lineAt(4000))
        assertSame(line, mini.lineAt(6500))
    }

    @Test fun pauseSeekAndEmptyOrLineTimedSourcesHaveNoInventedWordTiming() {
        val line = SyncedLine("Line synced", null, 1000, 2000)
        val mini = MiniLyricsTimeline(listOf(line))
        repeat(20) { assertSame(line, mini.lineAt(1500)) }
        assertNull(mini.lineAt(3000))
        assertSame(line, mini.lineAt(1500))
        assertNull(MiniLyricsTimeline(emptyList<ISyncedLine>()).lineAt(1500))
    }

    @Test fun transitionWindowsFollowDuetTakeoversReturnsAndInstrumentalResumption() {
        val lead = line("Lead", 1000, 5000)
        val guest = line("Guest", 2000, 3500)
        val timeline = MiniLyricsTimeline(listOf(lead, guest))
        assertEquals(MiniLyricsCue(lead, 1000, 2000), timeline.cueAt(1999))
        assertEquals(MiniLyricsCue(guest, 2000, 3500), timeline.cueAt(2000))
        assertEquals(MiniLyricsCue(lead, 3500, 5000), timeline.cueAt(3500))
        val rest = lead.copy(end = 8000, syllables = listOf(
            KaraokeSyllable("Before", 1000, 2000), KaraokeSyllable("After", 6000, 8000)))
        val resumed = MiniLyricsTimeline(listOf(rest))
        assertEquals(MiniLyricsCue(rest, 1000, 2000), resumed.cueAt(1500))
        assertNull(resumed.cueAt(4000))
        assertEquals(MiniLyricsCue(rest, 6000, 8000), resumed.cueAt(6000))
    }

    @Test fun oneLineTransitionsAreBoundedContinuousAndDeterministicAtMultipleFrameRates() {
        val cue = MiniLyricsCue(line("Line", 1000, 3000), 1000, 3000)
        assertEquals(0f, miniLyricsLinePose(cue, 1000).alpha, 0f)
        assertEquals(1f, miniLyricsLinePose(cue, 1180).alpha, 0f)
        assertEquals(0f, miniLyricsLinePose(cue, 3000).alpha, 0f)
        assertEquals(4f, miniLyricsLinePose(cue, 1000).offsetDp, 0f)
        assertEquals(-3f, miniLyricsLinePose(cue, 3000).offsetDp, 0f)
        for (fps in listOf(60, 90, 120)) {
            var previous = 0f
            for (step in 0..(fps * 180 / 1000)) {
                val now = 1000 + step * 1000 / fps
                val pose = miniLyricsLinePose(cue, now)
                assertTrue(pose.alpha in previous..1f)
                assertTrue(pose.offsetDp in 0f..4f)
                assertEquals(pose, miniLyricsLinePose(cue, now))
                previous = pose.alpha
            }
        }
        val short = cue.copy(start = 1000, end = 1090)
        assertEquals(1f, miniLyricsLinePose(short, 1045).alpha, 0f)
        assertEquals(1f, miniLyricsLinePose(cue, 2200).alpha, 0f)
    }
}
