package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OnlineLyricsServiceTest {
    private val song = Song(
        id = 13L, title = "Aku Cinta Kamu", artist = "Nusa Singer",
        album = "Kisah", uri = "content://music/13",
        durationMs = 180_000, albumId = 1L
    )

    @Test fun amllTtmlWinsBeforeLrclibAndUsesExplicitWordTiming() = runBlocking {
        val calls = ArrayList<String>()
        val service = OnlineLyricsService { url ->
            calls += url
            when {
                url.contains("/lyrics/search") -> """
                    {"status":200,"data":{"items":[
                      {"filename":"trusted.ttml","musicNames":["Aku Cinta Kamu"],
                       "artistNames":["Nusa Singer"],"albumNames":["Kisah"]},
                      {"filename":"wrong.ttml","musicNames":["Different Song"],
                       "artistNames":["Nusa Singer"],"albumNames":["Kisah"]}]}}
                """.trimIndent()
                url.contains("/lyrics/get") -> """
                    {"status":200,"data":{"lyrics":"<tt><body><p begin=\\"1s\\" end=\\"5s\\"><span begin=\\"1s\\" end=\\"2s\\">Aku </span><span begin=\\"2s\\" end=\\"5s\\">cinta kamu</span></p></body></tt>"}}
                """.trimIndent().replace("\\\\", "\\")
                else -> error("Unexpected endpoint $url")
            }
        }
        val result = service.lookup(song).result!!
        assertEquals("AMLL", result.source)
        assertTrue(result.content.startsWith("<tt>"))
        assertEquals(2, calls.size)
        assertTrue(calls[0].contains("musicName=Aku+Cinta+Kamu"))
        assertTrue(calls.none { it.contains("lrclib") || it.contains("wrong.ttml") })
    }

    @Test fun missingAmllFallsBackToSynchronizedLrclib() = runBlocking {
        val service = OnlineLyricsService { url ->
            if (url.contains("api.amll.dev")) """{"status":200,"data":{"items":[]}}"""
            else """{"trackName":"Aku Cinta Kamu","artistName":"Nusa Singer",
                    "duration":180,"instrumental":false,
                    "syncedLyrics":"[00:01]Aku cinta kamu\\n[00:05]Selamanya"}""".trimIndent()
        }
        val lyrics = service.lookup(song).result!!
        assertEquals("LRCLIB", lyrics.source)
        assertTrue(lyrics.content.contains("[00:01]"))
    }

    @Test fun remoteRecordsForWrongArtistOrDurationAreRejected() = runBlocking {
        val service = OnlineLyricsService { url ->
            if (url.contains("api.amll.dev")) """{"status":200,"data":{"items":[
              {"filename":"wrong.ttml","musicNames":["Aku Cinta Kamu"],
               "artistNames":["Wrong Artist"]}]}}"""
            else """{"trackName":"Aku Cinta Kamu","artistName":"Nusa Singer",
                    "duration":225,"instrumental":false,
                    "syncedLyrics":"[00:01]Wrong version"}"""
        }
        assertNull(service.lookup(song).result)
    }

    @Test fun normalizesPunctuationButNeverAcceptsUnrelatedTitles() {
        assertTrue(onlineLyricsMatch(song, listOf("Aku, Cinta Kamu!"), listOf("Nusa Singer")))
        assertFalse(onlineLyricsMatch(song, listOf("Aku Cinta Dia"), listOf("Nusa Singer")))
        assertFalse(onlineLyricsMatch(song, listOf("Aku Cinta Kamu"), listOf("Different Singer")))
        assertEquals("Nusa Singer", primaryLyricsArtist("Nusa Singer feat. Guest"))
    }

    @Test fun onlineSyncedUpgradesPlainButNeverDowngradesEmbeddedKaraoke() {
        val plain = EmbeddedLyricsSelector.select(listOf("Aku cinta kamu"), song.durationMs)
        val timed = EmbeddedLyricsSelector.select(listOf("[00:01]Aku cinta kamu"), song.durationMs)
        val karaoke = EmbeddedLyricsSelector.select(listOf(
            "[00:01]<00:01>Aku <00:02>cinta <00:03>kamu<00:05>"
        ), song.durationMs)
        assertEquals(1, lyricQuality(plain))
        assertEquals(2, lyricQuality(timed))
        assertEquals(3, lyricQuality(karaoke))
        assertEquals(timed, preferLyrics(plain, timed))
        assertEquals(karaoke, preferLyrics(karaoke, timed))
    }
}
