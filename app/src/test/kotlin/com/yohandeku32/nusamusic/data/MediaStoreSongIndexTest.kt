package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MediaStoreSongIndexTest {
    private fun song(id: Long, title: String = "Track $id") = Song(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        uri = "content://media/external/audio/media/$id",
        durationMs = 200_000L,
        albumId = 1L
    )

    @Test fun preservesUnchangedObjectsAndUpdatesOnlyChangedTags() {
        val first = song(1)
        val unchanged = song(2)
        val renamed = song(1, title = "Updated title")

        val result = MediaStoreSongIndex.merge(
            cached = listOf(first, unchanged),
            visibleIds = setOf(1, 2),
            changed = listOf(renamed)
        )

        assertEquals(listOf(renamed, unchanged), result)
        assertSame(unchanged, result[1])
    }

    @Test fun prunesDeletedAndNewlyExcludedMediaStoreTracks() {
        val result = MediaStoreSongIndex.merge(
            cached = listOf(song(1), song(2), song(3)),
            visibleIds = setOf(1),
            changed = emptyList()
        )
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test fun includesNewSongsWithoutDuplicatingAnExistingRow() {
        val result = MediaStoreSongIndex.merge(
            cached = listOf(song(1)),
            visibleIds = setOf(1, 2),
            changed = listOf(song(2), song(1, "Retagged"))
        )

        assertEquals(listOf(1L, 2L), result.map { it.id })
        assertEquals("Retagged", result.first().title)
    }

    @Test fun neverResurrectsRowsMissingFromCurrentIdCursor() {
        val result = MediaStoreSongIndex.merge(
            cached = listOf(song(1)),
            visibleIds = setOf(1),
            changed = listOf(song(2))
        )
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test fun handlesLargeCachedCollectionWithSingleNewTrack() {
        val cached = (1L..650L).map { song(it) }
        val changed = song(651)

        val result = MediaStoreSongIndex.merge(
            cached = cached,
            visibleIds = (1L..651L).toSet(),
            changed = listOf(changed)
        )

        assertEquals(651, result.size)
        assertSame(cached[349], result[349])
        assertEquals(changed, result.last())
    }
}
