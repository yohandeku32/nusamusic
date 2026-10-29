package com.yohandeku32.nusamusic.ui

import com.yohandeku32.nusamusic.data.EmbeddedLyricsSelector
import com.yohandeku32.nusamusic.data.StoredLyrics
import com.yohandeku32.nusamusic.data.SongLyrics
import com.yohandeku32.nusamusic.data.lyricQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsEmbedButtonStateTest {
    private val duration = 180_000L

    private val plain by lazy {
        EmbeddedLyricsSelector.select(listOf("Aku cinta kamu"), duration)
    }
    private val lineSync by lazy {
        EmbeddedLyricsSelector.select(listOf("[00:01.00]Aku cinta kamu"), duration)
    }
    private val syllableSync by lazy {
        EmbeddedLyricsSelector.select(listOf(
            "[00:01]<00:01>Aku <00:02>cinta <00:03>kamu<00:05>"
        ), duration)
    }

    private fun online(local: SongLyrics): SongLyrics =
        local.copy(stored = local.stored.copy(source = "lrc.red"))

    @Test fun unsavedOnlineLyricsShowSave() {
        assertEquals(1, lyricQuality(plain))
        assertEquals(2, lyricQuality(lineSync))
        assertEquals(3, lyricQuality(syllableSync))
        assertTrue(lyricsNeedEmbedding(online(lineSync), 0))
        assertTrue(lyricsNeedEmbedding(online(syllableSync), 0))
        assertFalse(lyricsNeedEmbedding(null, 0))
        assertFalse(lyricsNeedEmbedding(SongLyrics(StoredLyrics("", "lrc.red"), null), 0))
    }

    @Test fun embeddedPlainOrLineLyricsCanUpgradeToOnlineSyllables() {
        // The key regression: having ANY embedded lyric is not enough to
        // suppress Save when word/syllable timing is available online.
        assertTrue(lyricsNeedEmbedding(online(syllableSync), lyricQuality(plain)))
        assertTrue(lyricsNeedEmbedding(online(syllableSync), lyricQuality(lineSync)))
        assertTrue(lyricsNeedEmbedding(online(lineSync), lyricQuality(plain)))
    }

    @Test fun verifiedUpgradeHidesSaveUntilAnActuallyBetterVersionAppears() {
        val onlineSyllables = online(syllableSync)
        assertTrue(lyricsNeedEmbedding(onlineSyllables, lyricQuality(lineSync)))
        // On verified successful write, the state records quality=3.
        assertFalse(lyricsNeedEmbedding(onlineSyllables, lyricQuality(onlineSyllables)))
        assertFalse(lyricsNeedEmbedding(online(lineSync), lyricQuality(syllableSync)))
    }

    @Test fun neverOfferSaveForUnchangedOrWorseTimingOrLocalOnlyLyrics() {
        assertFalse(lyricsNeedEmbedding(plain, 0))
        assertFalse(lyricsNeedEmbedding(lineSync, 0))
        assertFalse(lyricsNeedEmbedding(syllableSync, 0))
        assertFalse(lyricsNeedEmbedding(online(lineSync), lyricQuality(lineSync)))
        assertFalse(lyricsNeedEmbedding(online(plain), lyricQuality(lineSync)))
        assertFalse(lyricsNeedEmbedding(online(syllableSync), lyricQuality(syllableSync)))
    }
    @Test fun onlineBackingVocalsOfferSaveEvenAtEqualKaraokeQuality() {
        val richer = EmbeddedLyricsSelector.select(listOf(
            """<tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body>
            <p begin="1s" end="5s"><span begin="1s" end="2s">Aku </span>
            <span begin="2s" end="5s">cinta </span>
            <span ttm:role="x-bg" begin="2s" end="5s"><span begin="2s" end="5s">Oh</span></span>
            </p></body></tt>"""
        ), duration).let(::online)
        assertEquals(3, lyricQuality(richer))
        assertTrue(lyricsNeedEmbedding(richer, 3, embeddedHasBackingVocals = false))
        assertFalse(lyricsNeedEmbedding(richer, 3, embeddedHasBackingVocals = true))
    }

}
