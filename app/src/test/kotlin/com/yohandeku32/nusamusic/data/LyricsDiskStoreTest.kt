package com.yohandeku32.nusamusic.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LyricsDiskStoreTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun importedUnicodeLyricsSurviveRestart() {
        val entry = StoredLyrics("[00:01]Halo 世界\n[00:03]Dunia", "imported", checkedAt = 100)
        LyricsDiskStore(temp.root).write("content://music/1", entry)
        assertEquals(entry, LyricsDiskStore(temp.root).read("content://music/1", Long.MAX_VALUE))
    }
    @Test fun missingLyricsExpireWithoutExpiringSuccessfulResults() {
        val store = LyricsDiskStore(temp.root)
        store.write("missing", StoredLyrics("", "LRCLIB", checkedAt = 100))
        assertNotNull(store.read("missing", 101))
        assertNull(store.read("missing", 100 + LyricsDiskStore.MISSING_TTL_MS))
        store.write("success", StoredLyrics("[00:01]Halo", "LRCLIB", checkedAt = 100))
        assertNotNull(store.read("success", Long.MAX_VALUE))
    }
    @Test fun changedClockDoesNotKeepNegativeCacheForever() {
        val store = LyricsDiskStore(temp.root)
        store.write("missing", StoredLyrics("", "LRCLIB", checkedAt = 10_000))
        assertNull(store.read("missing", 500))
    }
    @Test fun malformedCacheIsIgnored() {
        File(temp.root, LyricsDiskStore.digest("song") + ".properties").writeText("version=1\ncontent=x\nsource=LRCLIB\ncheckedAt=oops")
        assertNull(LyricsDiskStore(temp.root).read("song"))
    }
    @Test fun boundedCacheDoesNotGrowIndefinitely() {
        val store = LyricsDiskStore(temp.root, maxEntries = 2)
        repeat(5) { store.write("song$it", StoredLyrics("[00:01]$it", "LRCLIB")) }
        assertEquals(2, temp.root.listFiles()!!.size)
        assertNotNull(store.read("song4"))
    }
    @Test fun replacementLeavesNoTemporaryFiles() {
        val store = LyricsDiskStore(temp.root)
        store.write("song", StoredLyrics("[00:01]Old", "imported"))
        val replacement = StoredLyrics("[00:01]New", "imported")
        store.write("song", replacement)
        assertEquals(replacement, store.read("song"))
        assertEquals(1, temp.root.listFiles()!!.size)
    }

    @Test fun embeddedCreditsSurviveRestartWithUnicodeAndMultipleNames() {
        val entry = StoredLyrics("Lyrics", "embedded", credits =
            SongCredits(listOf("Composer 世界", "Second"), listOf("Writer")))
        LyricsDiskStore(temp.root).write("song", entry)
        assertEquals(entry, LyricsDiskStore(temp.root).read("song"))
    }
    @Test fun oldCacheEntriesHaveNoFabricatedCredits() {
        File(temp.root, LyricsDiskStore.digest("song") + ".properties")
            .writeText("version=1\ncontent=Lyrics\nsource=embedded\ncheckedAt=100")
        assertTrue(LyricsDiskStore(temp.root).read("song")!!.credits.isEmpty)
    }

    @Test fun ttmlSongwritersSurviveRestartWithoutChangingTheAuthoredRoles() {
        val entry = StoredLyrics("Lyrics", "lrc.red", credits = SongCredits(
            composers = listOf("Composer"), songwriters = listOf("Writer 世界", "Second")))
        LyricsDiskStore(temp.root).write("song", entry)
        assertEquals(entry, LyricsDiskStore(temp.root).read("song"))
    }
}
