package com.yohandeku32.nusamusic.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import java.util.ArrayDeque
import kotlin.coroutines.coroutineContext

/** A folder grant is enough. Never request per-track selection or broad storage writes. */
internal class LyricsFolderAccess(private val context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences("nusa_lyrics_folder", Context.MODE_PRIVATE)

    fun rememberFolder(tree: Uri) {
        resolver.takePersistableUriPermission(
            tree,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        check(resolver.persistedUriPermissions.any {
            it.uri == tree && it.isReadPermission && it.isWritePermission
        }) { "This folder did not grant persistent write access." }
        preferences.edit().putString("last_tree", tree.toString()).apply()
    }

    fun authorizedTrees(): List<Uri> {
        val selected = preferences.getString("last_tree", null)
        return resolver.persistedUriPermissions.asSequence()
            .filter { it.isReadPermission && it.isWritePermission }
            .map { it.uri }
            .filter { runCatching { DocumentsContract.isTreeUri(it) }.getOrDefault(false) }
            .distinct().sortedBy { if (it.toString() == selected) 0 else 1 }
            .toList()
    }

    /** Returns the verified original file under a granted tree, or null. */
    suspend fun find(song: Song): Uri? = withContext(Dispatchers.IO) {
        val trees = authorizedTrees()
        if (trees.isEmpty()) return@withContext null
        val name = resolver.query(
            Uri.parse(song.uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: Uri.parse(song.uri).lastPathSegment?.substringAfterLast('/')
            ?: return@withContext null
        if (!name.endsWith(".mp3", ignoreCase = true)) return@withContext null

        // Hash the currently indexed song once. Filename and size alone are
        // not identity: two albums can have identical file names.
        val original = resolver.openInputStream(Uri.parse(song.uri))?.use {
            digest(it)
        } ?: throw IOException("Cannot read the currently playing MP3.")
        for (tree in trees) {
            coroutineContext.ensureActive()
            val exact = sourceDocumentWithinTree(song, tree)
            if (exact != null && matches(exact, original)) return@withContext exact

            for (candidate in filenamesInTree(tree, name, original.size)) {
                coroutineContext.ensureActive()
                if (matches(candidate, original)) return@withContext candidate
            }
        }
        null
    }

    private fun sourceDocumentWithinTree(song: Song, tree: Uri): Uri? {
        val uri = Uri.parse(song.uri)
        if (!DocumentsContract.isDocumentUri(context, uri) ||
            uri.authority != tree.authority) return null
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: return null
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull()
            ?: return null
        if (documentId != rootId && !documentId.startsWith("$rootId/")) return null
        return DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
    }

    private data class Signature(val size: Long, val checksum: ByteArray)

    private suspend fun matches(uri: Uri, original: Signature): Boolean {
        val size = runCatching {
            resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_SIZE),
                null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
            }
        }.getOrNull() ?: -1L
        if (size > 0 && size != original.size) return false
        return runCatching {
            resolver.openInputStream(uri)?.use { digest(it) }
                ?.let { it.size == original.size && it.checksum.contentEquals(original.checksum) }
                ?: false
        }.getOrDefault(false)
    }

    private suspend fun digest(input: java.io.InputStream): Signature {
        val sha = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        var count = 0L
        while (true) {
            coroutineContext.ensureActive()
            val n = input.read(buffer)
            if (n < 0) break
            count += n
            if (count > 384L * 1024 * 1024) throw IOException("Song exceeds MP3 embed size limit.")
            sha.update(buffer, 0, n)
        }
        return Signature(count, sha.digest())
    }

    /**
     * BFS through the selected subtree, not unrestricted device storage.
     * Providers may have a very large library, so cap traversal. If no exact
     * match is found, the UI can ask for the correct folder again.
     */
    private suspend fun filenamesInTree(
        tree: Uri, filename: String, originalSize: Long
    ): List<Uri> {
        val results = ArrayList<Uri>(8)
        val root = DocumentsContract.getTreeDocumentId(tree)
        val queue = ArrayDeque<String>().apply { add(root) }
        var inspected = 0
        while (queue.isNotEmpty() && inspected < 8_000 && results.size < 30) {
            coroutineContext.ensureActive()
            val folderId = queue.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderId)
            val cursor = runCatching {
                resolver.query(children, arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ), null, null, null)
            }.getOrNull() ?: continue
            cursor.use {
                while (it.moveToNext() && inspected++ < 8_000 && results.size < 30) {
                    val id = it.getString(0) ?: continue
                    val mime = it.getString(2)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        queue.add(id)
                    } else if (filename.equals(it.getString(1), ignoreCase = true)) {
                        val size = if (it.isNull(3)) -1L else it.getLong(3)
                        if (size < 0 || size == originalSize) {
                            results.add(DocumentsContract.buildDocumentUriUsingTree(tree, id))
                        }
                    }
                }
            }
        }
        return results
    }
}
