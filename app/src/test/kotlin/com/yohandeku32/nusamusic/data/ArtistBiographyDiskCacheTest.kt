package com.yohandeku32.nusamusic.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Locale

class ArtistBiographyDiskCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private val biography = ArtistBiography("Björk", "Biografi artis dengan aksen é dan 日本語.\nBaris kedua.", "id", "https://www.last.fm/music/Bj%C3%B6rk")

    @Test fun survivesNewCacheInstanceAndPreservesUnicode() {
        val directory = temporary.newFolder()
        ArtistBiographyDiskCache(directory).write("Björk", biography)
        assertEquals(biography, ArtistBiographyDiskCache(directory).read("Björk"))
        assertEquals(1, directory.listFiles()!!.size)
    }

    @Test fun collaborationsCaseAndWhitespaceShareOneEntry() {
        val cache = ArtistBiographyDiskCache(temporary.newFolder())
        cache.write("  BJÖRK feat. Guest ", biography)
        assertEquals(biography, cache.read("Björk"))
        assertEquals(ArtistNameUtils.cacheKey("Band  Name"), ArtistNameUtils.cacheKey(" band name "))
        assertNotEquals(ArtistNameUtils.cacheKey("東京"), ArtistNameUtils.cacheKey("大阪"))
    }

    @Test fun artistKeysDoNotDependOnDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("inxs", ArtistNameUtils.cacheKey("INXS"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun missingAndCorruptFilesAreCacheMisses() {
        val directory = temporary.newFolder()
        val cache = ArtistBiographyDiskCache(directory)
        assertNull(cache.read("Björk"))
        cache.write("Björk", biography)
        directory.listFiles()!!.single().writeText("version=1\ntext=partial")
        assertNull(cache.read("Björk"))
    }

    @Test fun updatingOneArtistDoesNotReplaceAnotherArtist() {
        val cache = ArtistBiographyDiskCache(temporary.newFolder())
        cache.write("Björk", biography)
        val adele = biography.copy(artistName = "Adele", sourceUrl = "http://www.last.fm/music/Adele")
        cache.write("Adele", adele)
        val updated = biography.copy(text = "Updated biography")
        cache.write("Björk", updated)
        assertEquals(updated, cache.read("Björk"))
        assertEquals(adele, cache.read("Adele"))
    }

    @Test fun unwritableCacheDoesNotPreventUsingFetchedBiography() {
        val cache = ArtistBiographyDiskCache(temporary.newFile())
        cache.write("Björk", biography)
        assertNull(cache.read("Björk"))
    }
}
