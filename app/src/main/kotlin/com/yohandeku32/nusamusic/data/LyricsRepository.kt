package com.yohandeku32.nusamusic.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

data class SongLyrics(val stored: StoredLyrics, val synced: SyncedLyrics?)

class LyricsRepository(context: Context) {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver
    private val imports = LyricsDiskStore(File(this.context.filesDir, "lyrics/imported"))
    private val cache = LyricsDiskStore(File(this.context.cacheDir, "lyrics/remote"), maxEntries = 120)

    companion object {
        fun cacheKey(song: Song) = listOf(song.uri, song.title, song.artist, song.album, song.durationMs.toString()).joinToString("\u0000")
        fun readText(input: InputStream): String {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8_192)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                require(output.size() + size <= LyricsDecoder.MAX_BYTES) { "Lyrics file is too large" }
                output.write(buffer, 0, size)
            }
            val bytes = output.toByteArray()
            // UTF-16 is common in sidecars exported by desktop music tools.
            val charset = when {
                bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
                bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
                else -> Charsets.UTF_8
            }
            return bytes.toString(charset).trimStart('\uFEFF')
        }
    }

    private fun parsed(entry: StoredLyrics, song: Song): SongLyrics = SongLyrics(entry,
        if (entry.plain || entry.content.isBlank() || entry.instrumental) null
        else LyricsDecoder.decode(entry.content, song.durationMs))

    suspend fun load(song: Song, retry: Boolean = false): SongLyrics = withContext(Dispatchers.IO) {
        imports.read(song.uri)?.let { return@withContext parsed(it, song) }
        val key = cacheKey(song)
        localSidecar(song)?.let { entry ->
            val result = parsed(entry, song)
            coroutineContext.ensureActive()
            runCatching { cache.write(key, entry) }
            return@withContext result
        }
        if (!retry) cache.read(key)?.let { cached ->
            runCatching { parsed(cached, song) }.getOrNull()?.let { return@withContext it }
        }
        val entry = fetch(song)
        val result = parsed(entry, song)
        coroutineContext.ensureActive()
        // Cache failures must not hide lyrics successfully loaded from the network.
        runCatching { cache.write(key, entry) }
        result
    }

    suspend fun import(song: Song, uri: Uri): SongLyrics = withContext(Dispatchers.IO) {
        val content = resolver.openInputStream(uri)?.use(::readText) ?: error("Cannot open lyrics")
        val entry = StoredLyrics(content, "imported")
        val result = parsed(entry, song) // Validate before replacing the user's existing choice.
        coroutineContext.ensureActive()
        imports.write(song.uri, entry)
        result
    }

    private fun fetch(song: Song): StoredLyrics {
        val url = Uri.parse("https://lrclib.net/api/get").buildUpon()
            .appendQueryParameter("track_name", song.title)
            .appendQueryParameter("artist_name", song.artist)
            .appendQueryParameter("album_name", song.album)
            .appendQueryParameter("duration", (song.durationMs / 1_000.0).toString()).build()
        val connection = URL(url.toString()).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "NusaMusic/3.13.4 (https://github.com/yohandeku32/nusamusic)")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode == 404) return StoredLyrics("", "LRCLIB")
            check(connection.responseCode == 200) { "Lyrics service unavailable" }
            val json = JSONObject(connection.inputStream.use(::readText))
            if (json.optBoolean("instrumental")) return StoredLyrics("", "LRCLIB", instrumental = true)
            val synced = json.optString("syncedLyrics").takeUnless { it.isBlank() || it == "null" }
            val plain = json.optString("plainLyrics").takeUnless { it.isBlank() || it == "null" }
            return when {
                synced != null -> StoredLyrics(synced, "LRCLIB")
                plain != null -> StoredLyrics(plain, "LRCLIB", plain = true)
                else -> StoredLyrics("", "LRCLIB")
            }
        } finally { connection.disconnect() }
    }

    /** Scoped storage: automatically read siblings only in user-granted music folders. */
    private fun localSidecar(song: Song): StoredLyrics? {
        val uri = Uri.parse(song.uri)
        val candidates = ArrayList<Pair<Uri, String>>()
        if (DocumentsContract.isTreeUri(uri)) {
            runCatching {
                val docId = DocumentsContract.getDocumentId(uri)
                val name = docId.substringAfterLast('/')
                candidates += uri to docId.substringBeforeLast('/', docId.substringBefore(':') + ":")
                return findSibling(candidates, name, song.durationMs)
            }
        }
        // A song indexed by MediaStore may also live inside an already granted SAF tree.
        runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.VOLUME_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val name = cursor.getString(0) ?: return@use
                val relative = cursor.getString(1)?.trimEnd('/') ?: return@use
                val volume = cursor.getString(2)
                val documentVolume = if (volume == "external_primary") "primary" else volume
                val parentId = "$documentVolume:$relative"
                resolver.persistedUriPermissions.filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }.forEach {
                    val root = DocumentsContract.getTreeDocumentId(it.uri).trimEnd('/')
                    if (parentId == root || parentId.startsWith(if (root.endsWith(':')) root else "$root/")) candidates += it.uri to parentId
                }
                return findSibling(candidates, name, song.durationMs)
            }
        }
        return null
    }

    private fun findSibling(parents: List<Pair<Uri, String>>, audioName: String, durationMs: Long): StoredLyrics? {
        val stem = audioName.substringBeforeLast('.', audioName)
        for ((tree, parentId) in parents.distinct()) {
            val matches = ArrayList<Pair<String, Uri>>()
            runCatching {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
                resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(1) ?: continue
                        if (name.equals("$stem.ttml", true) || name.equals("$stem.lrc", true)) matches += name to DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                    }
                }
            }
            for ((_, uri) in matches.sortedBy { if (it.first.endsWith(".ttml", true)) 0 else 1 }) {
                runCatching {
                    val content = resolver.openInputStream(uri)?.use(::readText) ?: return@runCatching null
                    StoredLyrics(content, "local").also { LyricsDecoder.decode(content, durationMs) }
                }.getOrNull()?.let { return it }
            }
        }
        return null
    }
}
