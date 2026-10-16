package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import org.junit.Assert.*
import org.junit.Test

class ArtistLibraryTest {
    private fun song(id: Long, artist: String = "Adele", album: String = "25", title: String = "Track $id") =
        Song(id, title, artist, album, "content://music/$id", 180_000L, id)

    @Test fun matchesWholePrimaryArtistAndDeduplicatesLocalUris() {
        val a = song(1)
        val tracks = ArtistLibrary.tracks(listOf(a, a.copy(id = 99), song(2, " ADELE feat. Guest "), song(3, "Adele Tribute")), "adele")
        assertEquals(listOf(1L, 2L), tracks.map { it.id })
        assertTrue(ArtistLibrary.tracks(listOf(song(4, "Unknown artist")), "Unknown artist").isEmpty())
    }

    @Test fun ranksOnlyPlayedLocalTracksAndLimitsToFive() {
        val songs = (1L..8L).map { song(it) }
        val counts = songs.associate { it.uri to it.id } + ("content://removed" to 999L)
        assertEquals(listOf(8L, 7L, 6L, 5L, 4L), ArtistLibrary.mostPlayed(songs, counts).map { it.id })
        assertTrue(ArtistLibrary.mostPlayed(songs, emptyMap()).isEmpty())
    }

    @Test fun groupsFolderAndMediaStoreTracksByAlbumTags() {
        val albums = ArtistLibrary.albums(listOf(song(1), song(2, album = " 25 "), song(3, album = "21")))
        assertEquals(listOf("21", "25"), albums.map { it.title })
        assertEquals(listOf(1L, 2L), albums.last().tracks.map { it.id })
    }
}
