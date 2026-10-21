package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class OnlineLyricsServiceTest {
    private val song = Song(
        id = 13L, title = "Aku Cinta Kamu", artist = "Nusa Singer",
        album = "Kisah", uri = "content://music/13",
        durationMs = 180_000, albumId = 1L
    )
    private val isrc = "USAAA2600001"
    private val ttml = "<tt><body><p begin=\"1s\" end=\"5s\">" +
        "<span begin=\"1s\" end=\"2s\">Aku </span>" +
        "<span begin=\"2s\" end=\"5s\">cinta kamu</span></p></body></tt>"

    private fun hits(vararg entries: JSONObject): String =
        JSONObject().put("hits", JSONArray().apply { entries.forEach { put(it) } }).toString()

    private fun hit(
        title: String = song.title,
        artist: String = song.artist,
        code: String = isrc
    ) = JSONObject().put("isrc", code).put("title", title).put("artist", artist)

    private fun record(
        title: String = song.title,
        artist: String = song.artist,
        duration: Double = 180.0
    ) = JSONObject().put("title", title).put("artist", artist)
        .put("album", song.album).put("duration", duration)

    @Test fun lrcRedMatchFetchesIsrcSongAndPreservesOriginalSyllableTtml() = runBlocking {
        val urls = mutableListOf<String>()
        val service = OnlineLyricsService { url ->
            urls.add(url)
            if (url.contains("/match.json")) hits(hit())
            else if (url == "https://lrc.red/s/" + isrc + ".song.json")
                record().put("source", ttml).toString()
            else error("Unexpected endpoint: " + url)
        }
        val result = service.lookup(song).result!!
        assertEquals("lrc.red", result.source)
        assertEquals(ttml, result.content)
        assertEquals(2, urls.size)
        assertTrue(urls.first().contains("/match.json?track=Aku+Cinta+Kamu&artist=Nusa+Singer"))
        assertEquals(3, lyricQuality(EmbeddedLyricsSelector.select(
            listOf(result.content), song.durationMs)))
    }

    @Test fun wordSyncedJsonIsConvertedToAuthoredTimedTtml() = runBlocking {
        val line = JSONObject().put("text", "Aku cinta kamu")
            .put("begin", 1.0).put("end", 5.0)
            .put("words", JSONArray().put(JSONArray().put(
                JSONObject().put("text", "Aku").put("begin", 1.0).put("end", 1.6)
            )).put(JSONArray().put(
                JSONObject().put("text", "cinta").put("begin", 1.6).put("end", 2.4)
            )).put(JSONArray().put(
                JSONObject().put("text", "kamu").put("begin", 2.4).put("end", 5.0)
            )))
        val service = OnlineLyricsService { url ->
            if (url.contains("match.json")) hits(hit())
            else record().put("lyrics", JSONObject().put("sync", "word")
                .put("lines", JSONArray().put(line))).toString()
        }
        val content = service.lookup(song).result!!.content
        assertTrue(content.contains("<span begin=\"2.4s\" end=\"5.0s\">kamu</span>"))
        assertEquals(3, lyricQuality(EmbeddedLyricsSelector.select(
            listOf(content), song.durationMs)))
    }

    @Test fun lineSyncedJsonKeepsLineTimingWithoutInventedSyllables() = runBlocking {
        val lines = JSONArray().put(JSONObject().put("text", "Aku cinta kamu")
            .put("begin", 1.0).put("end", 5.0))
            .put(JSONObject().put("text", "Selamanya")
                .put("begin", 5.0).put("end", 8.0))
        val service = OnlineLyricsService { url ->
            if (url.contains("match.json")) hits(hit())
            else record().put("lyrics", JSONObject().put("sync", "line")
                .put("lines", lines)).toString()
        }
        val result = service.lookup(song).result!!
        assertEquals(2, lyricQuality(EmbeddedLyricsSelector.select(
            listOf(result.content), song.durationMs)))
        assertTrue(result.content.contains("Selamanya"))
    }

    @Test fun searchFallbackUsedWhenExactMetadataMatchMisses() = runBlocking {
        val calls = mutableListOf<String>()
        val service = OnlineLyricsService { url ->
            calls.add(url)
            when {
                url.contains("/match.json") -> hits()
                url.contains("/search.json") -> hits(hit())
                url.contains(".song.json") -> record().put("source", ttml).toString()
                else -> null
            }
        }
        assertEquals("lrc.red", service.lookup(song).result?.source)
        assertTrue(calls.any { it.contains("/search.json?q=") })
    }

    @Test fun doesNotAcceptWrongArtistOrDifferentRecording() = runBlocking {
        val service = OnlineLyricsService { url ->
            when {
                url.contains("/match.json") -> hits(hit(), hit(artist = "Unrelated Singer"))
                url.contains("/search.json") -> hits()
                else -> record(duration = 240.0).put("source", ttml).toString()
            }
        }
        assertNull(service.lookup(song).result)
    }

    @Test fun networkErrorsAreNotStoredAsPermanentMissingLyrics() = runBlocking {
        val result = OnlineLyricsService { throw IOException("Offline") }.lookup(song)
        assertNull(result.result)
        assertFalse(result.conclusivelyMissing)
    }

    @Test fun ordinaryUnmatchedSearchesProduceShortLivedMisses() = runBlocking {
        val result = OnlineLyricsService { hits() }.lookup(song)
        assertNull(result.result)
        assertTrue(result.conclusivelyMissing)
    }

    @Test fun normalizesRemasterSuffixButRejectsWrongSongs() {
        assertTrue(onlineLyricsMatch(song, listOf("Aku Cinta Kamu (Remastered)"),
            listOf("Nusa Singer")))
        assertFalse(onlineLyricsMatch(song, listOf("Aku Cinta Dia"),
            listOf("Nusa Singer")))
        assertFalse(onlineLyricsMatch(song, listOf("Aku Cinta Kamu"),
            listOf("Different Singer")))
        assertEquals("Nusa Singer", primaryLyricsArtist("Nusa Singer feat. Guest"))
    }

    @Test fun syncedRemoteUpgradesPlainButNeverReplacesLocalSyllableLyrics() {
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
