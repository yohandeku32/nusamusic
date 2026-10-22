package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LyricsPendingSaveQueueTest {
    @Test fun savingCurrentSongPersistsQueueWithoutReadingOrRewritingAudio() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val song = Song(
            id = 970000001L, title = "Test", artist = "Artist", album = "Album",
            uri = "content://media/external/audio/media/970000001", durationMs = 200_000L,
            albumId = 1L
        )
        val file = File(context.filesDir,
            "pending_embedded_lyrics/" + song.id.toString() + ".json")
        try {
            val queue = LyricsPendingSaveQueue(context)
            queue.enqueue(song, SongLyrics(
                stored = StoredLyrics("[00:01]Kita bersama", "lrc.red"), synced = null
            ))
            assertTrue(queue.isQueued(song.id))
            assertTrue(file.isFile)

            // Even without a playable source, Save MUST NOT access or modify
            // the audio while Media3 has this same track selected.
            var callbacks = 0
            queue.finishInactive(
                currentSongId = { song.id },
                onResult = { _, _, _ -> callbacks++ }
            )
            assertEquals(0, callbacks)
            assertTrue(queue.isQueued(song.id))
            assertTrue(file.isFile)

            // Recreated activity/process still discovers the durable request.
            val restored = LyricsPendingSaveQueue(context)
            assertTrue(restored.isQueued(song.id))
        } finally {
            file.delete()
        }
    }
}
