package com.yohandeku32.nusamusic.data

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.runtime.mutableStateMapOf
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Media3 may hold the currently playing file open. Rewriting that original
 * through SAF can truncate its active read and freeze playback. Save requests
 * are therefore durable and completed only after switching to another song.
 */
internal class LyricsPendingSaveQueue(context: Context) {
    private val app = context.applicationContext
    private val folder = File(app.filesDir, "pending_embedded_lyrics").apply { mkdirs() }
    private val mutex = Mutex()
    private val pending = mutableStateMapOf<Long, Boolean>()

    init {
        folder.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }
            ?.forEach { file ->
                file.name.removeSuffix(".json").toLongOrNull()?.let { pending[it] = true }
            }
    }

    fun isQueued(songId: Long): Boolean = pending[songId] == true

    suspend fun enqueue(song: Song, selectedUri: Uri, lyrics: SongLyrics) {
        val raw = lyrics.stored.content
        require(raw.isNotBlank() && raw.toByteArray(Charsets.UTF_8).size <= LyricsDecoder.MAX_BYTES) {
            "Lyrics are empty or too large to save."
        }
        val payload = JSONObject()
            .put("songId", song.id)
            .put("songUri", song.uri)
            .put("selectedUri", selectedUri.toString())
            .put("authored", raw)
            .put("plain", readableLyrics(lyrics))
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val atomic = AtomicFile(File(folder, song.id.toString() + ".json"))
                val output = atomic.startWrite()
                try {
                    output.write(payload.toString().toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                    atomic.finishWrite(output)
                } catch (error: Exception) {
                    atomic.failWrite(output)
                    throw error
                }
            }
            pending[song.id] = true
        }
    }

    /**
     * Pass the *actual* Media3 current mediaId so Compose's cached song does
     * not allow a write to the track still feeding the audio decoder.
     */
    suspend fun finishInactive(
        currentSongId: () -> Long?,
        onResult: suspend (songId: Long, success: Boolean, message: String) -> Unit
    ) = mutex.withLock {
        val files = withContext(Dispatchers.IO) {
            folder.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }
                ?.sortedBy { it.lastModified() }.orEmpty()
        }
        for (file in files) {
            val id = file.name.removeSuffix(".json").toLongOrNull() ?: continue
            if (currentSongId() == id) continue
            val item = try {
                withContext(Dispatchers.IO) {
                    JSONObject(AtomicFile(file).readFully().toString(Charsets.UTF_8))
                }.also { require(it.getLong("songId") == id) }
            } catch (error: Exception) {
                withContext(Dispatchers.IO) {
                    file.renameTo(File(folder, id.toString() + ".invalid"))
                }
                pending.remove(id)
                onResult(id, false, "Queued lyrics could not be read. Try Save again.")
                continue
            }
            try {
                AudioLyricsEmbedder(app).embedPrepared(
                    songUri = item.getString("songUri"),
                    selectedUri = Uri.parse(item.getString("selectedUri")),
                    raw = item.getString("authored"),
                    plain = item.getString("plain"),
                    beforeWrite = {
                        check(currentSongId() != id) {
                            "This song is playing again. Save will resume after the next song."
                        }
                    }
                )
                withContext(Dispatchers.IO) { file.delete() }
                pending.remove(id)
                onResult(id, true, "Lyrics saved to this song")
            } catch (error: Exception) {
                // A return to this song is transient, not a failed save.
                if (currentSongId() == id) continue
                withContext(Dispatchers.IO) {
                    file.renameTo(File(folder, id.toString() + ".failed"))
                }
                pending.remove(id)
                onResult(id, false,
                    error.message ?: "Lyrics could not be saved. Original song is unchanged.")
            }
        }
    }
}
