package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import org.junit.Assert.*
import org.junit.Test

class DiscoveryLibraryTest {
    private fun song(id: Long, artist: String, album: String = "Album", added: Long = id) =
        Song(id, "Song $id", artist, album, "content://music/$id", 180000, id, added)

    @Test fun popularArtistsUseListeningCountsAndMergeCollaborationTags() {
        val a = song(1, "Artist A feat. Guest")
        val b = song(2, "artist a")
        val c = song(3, "Artist B")
        val data = DiscoveryLibrary.build(listOf(a, b, c, a),
            mapOf(a.uri to 2, b.uri to 3, c.uri to 8), emptyMap())
        assertEquals(listOf("Artist B", "Artist A"), data.artists.map { it.name })
        assertEquals(listOf(8L, 5L), data.artists.map { it.plays })
        assertEquals(2, data.artists[1].tracks.size)
        assertTrue(data.hasListeningHistory)
    }

    @Test fun sameAlbumTitleByDifferentArtistsKeepsSeparatePlayableAlbums() {
        val first = song(1, "Artist A", "Same title", 100)
        val second = song(2, "Artist B", "Same title", 200)
        val data = DiscoveryLibrary.build(listOf(first, second), emptyMap(), emptyMap())
        assertEquals(2, data.albums.size)
        assertEquals(2, data.albums.map { it.key }.distinct().size)
        assertEquals(second, data.albums[0].tracks.single())
        assertEquals(first, data.albums[1].tracks.single())
        assertEquals(ArtistLibrary.albums(listOf(second)).single().key, data.albums[0].albumKey)
    }

    @Test fun recentTracksUseLastListenedTimeAndExcludeRemovedFiles() {
        val a = song(1, "Artist")
        val b = song(2, "Artist")
        val unseen = song(3, "Artist")
        val deleted = song(4, "Artist")
        val data = DiscoveryLibrary.build(listOf(a, b, unseen), mapOf(a.uri to 100),
            mapOf(a.uri to 1000, b.uri to 2000, deleted.uri to 3000))
        assertEquals(listOf(b, a), data.recent)
    }

    @Test fun emptyHistoryDoesNotInventRecentPlaysOrPopularity() {
        val data = DiscoveryLibrary.build(listOf(song(1, "Artist")), emptyMap(), emptyMap())
        assertFalse(data.hasListeningHistory)
        assertTrue(data.recent.isEmpty())
        assertEquals(1, data.artists.size)
        assertEquals(1, data.albums.size)
        val empty = DiscoveryLibrary.build(emptyList(), emptyMap(), emptyMap())
        assertTrue(empty.albums.isEmpty() && empty.artists.isEmpty() && empty.recent.isEmpty())
    }
}
