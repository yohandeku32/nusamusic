package com.yohandeku32.nusamusic.ui

import com.yohandeku32.nusamusic.data.EmbeddedLyricsSelector
import com.yohandeku32.nusamusic.data.StoredLyrics
import com.yohandeku32.nusamusic.data.SongLyrics
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsEmbedButtonStateTest {
    @Test fun saveAppearsOnlyWhenOnlineLyricsExistAndFileHasNone() {
        val online = SongLyrics(StoredLyrics("[00:01]Aku cinta kamu", "lrc.red"), null)
        assertTrue(lyricsNeedEmbedding(online, false))
        assertFalse(lyricsNeedEmbedding(online, true))
        assertFalse(lyricsNeedEmbedding(null, false))
        assertFalse(lyricsNeedEmbedding(SongLyrics(StoredLyrics("", "lrc.red"), null), false))
    }

    @Test fun embeddedLyricsHideSaveIncludingWhenOnlineLyricsAreBetter() {
        val embedded = EmbeddedLyricsSelector.select(
            listOf("[00:01]Aku cinta kamu"), 180000L
        )
        assertFalse(lyricsNeedEmbedding(embedded, false))
        val newerOnline = SongLyrics(StoredLyrics("<tt><body></body></tt>", "lrc.red"), null)
        assertFalse(lyricsNeedEmbedding(newerOnline, true))
    }
}
