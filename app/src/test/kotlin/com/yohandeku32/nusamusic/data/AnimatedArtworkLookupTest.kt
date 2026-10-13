package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedArtworkLookupTest {
    private val song = Song(1, "Cinta Aku Bisa Apa", "Mawar de Jongh", "Unknown album", "content://music/1", 180_000, 1)
    private val albumUrl = "https://music.apple.com/id/album/example/123456"

    @Test
    fun readsEmbeddedTagsAndFillsMissingValuesFromTheLibrary() {
        val fileTags = ArtworkLookupMetadata("Title from file", "<unknown>", "Album from file", "Album artist")
        val metadata = artworkLookupMetadata(song, fileTags)
        assertEquals("Title from file", metadata.title)
        assertEquals(song.artist, metadata.artist)
        assertEquals("Album from file", metadata.album)
        assertTrue("Album artist" in metadata.artists)
        assertEquals(song.durationMs, metadata.durationMs)
        assertEquals(181_000L, artworkLookupMetadata(song, fileTags.copy(durationMs = 181_000L)).durationMs)
    }

    @Test
    fun missingAlbumStillAllowsTitleAndArtistLookup() {
        val metadata = artworkLookupMetadata(song, null)
        assertEquals("", metadata.album)
        assertTrue(metadata.canSearchCatalog)
        assertFalse(metadata.copy(title = "").canSearchCatalog)
        assertFalse(metadata.copy(artist = "", albumArtist = "").canSearchCatalog)
    }

    @Test
    fun failedTrackLookupCannotShareACacheEntryWithOtherTracksOnTheAlbum() {
        val first = artworkLookupMetadata(song.copy(album = "Album"), null)
        val other = artworkLookupMetadata(song.copy(title = "Other song", album = "Album"), null)
        assertNotEquals(first.cacheKey, other.cacheKey)
    }

    @Test
    fun mawarExampleMatchesJoinedArtistSpellingWithoutDependingOnAlbumTags() {
        val metadata = artworkLookupMetadata(song, null)
        val expected = AppleCatalogSong(song.title, "Mawar deJongh", "Cinta Aku Bisa Apa - Single", albumUrl, 181_000)
        val wrongArtist = expected.copy(artist = "Other artist")
        val wrongTitle = expected.copy(title = "Other song")
        assertEquals(listOf(expected), matchingAppleAlbums(metadata, listOf(wrongArtist, wrongTitle, expected)))
    }

    @Test
    fun handlesFeaturedArtistsAndAlbumArtists() {
        val metadata = artworkLookupMetadata(song.copy(title = "Song (feat. Guest)", artist = "Singer; Guest"), null)
        val result = AppleCatalogSong("Song", "Singer", "Album", albumUrl)
        assertEquals(listOf(result), matchingAppleAlbums(metadata, listOf(result)))
        val albumArtistOnly = metadata.copy(artist = "", albumArtist = "Singer")
        assertEquals(listOf(result), matchingAppleAlbums(albumArtistOnly, listOf(result)))
    }

    @Test
    fun prefersMatchingAlbumAndDoesNotMistakeLiveOrRemixVersionsForTheOriginal() {
        val metadata = artworkLookupMetadata(song.copy(album = "Album (Deluxe Edition)"), null)
        val original = AppleCatalogSong(song.title, song.artist, "Album", albumUrl, 181_000)
        val single = original.copy(album = "Single", albumUrl = "https://music.apple.com/id/album/single/789")
        val live = original.copy(title = song.title + " (Live)")
        val remix = original.copy(title = song.title + " (Remix)")
        assertEquals(listOf(original, single), matchingAppleAlbums(metadata, listOf(single, live, remix, original)))
        assertEquals("Album (Taylor's Version)", artworkSearchAlbum("Album (Taylor's Version)"))
    }

    @Test
    fun rejectsDifferentRecordingDurationsAndDeduplicatesAlbumIdsAcrossStorefronts() {
        val metadata = artworkLookupMetadata(song, null)
        val first = AppleCatalogSong(song.title, song.artist, "Album", albumUrl, 181_000)
        val duplicate = first.copy(albumUrl = "https://music.apple.com/us/album/example/123456")
        val differentRecording = first.copy(durationMs = 260_000)
        assertEquals(listOf(first), matchingAppleAlbums(metadata, listOf(differentRecording, first, duplicate)))
    }

    @Test
    fun acceptsOnlyAppleAlbumLinksAndRemovesTrackQueryParameters() {
        assertEquals(albumUrl, appleMusicAlbumUrl(albumUrl + "?i=234&uo=4"))
        assertEquals("https://music.apple.com/us/album/123456", appleMusicAlbumUrl("https://music.apple.com/us/album/123456"))
        assertNull(appleMusicAlbumUrl("https://music.apple.com.example.org/id/album/example/123456"))
        assertNull(appleMusicAlbumUrl("http://music.apple.com/id/album/example/123456"))
        assertNull(appleMusicAlbumUrl("https://music.apple.com/id/artist/example/123456"))
    }
}
