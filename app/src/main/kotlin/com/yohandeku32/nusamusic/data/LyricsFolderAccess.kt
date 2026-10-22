package com.yohandeku32.nusamusic.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.provider.MediaStore
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

    /**
     * A single SAF grant to the parent Music directory covers every descendant.
     * The MediaStore relative path lets us locate deep songs without walking
     * thousands of unrelated artists, folders or files on each Save.
     */
    suspend fun find(song: Song): Uri? = withContext(Dispatchers.IO) {
        val trees = authorizedTrees()
        if (trees.isEmpty()) return@withContext null
        val (name, relativePath) = indexedLocation(song)
        if (name == null || !name.endsWith(".mp3", ignoreCase = true)) {
            return@withContext null
        }

        // The digest is an identity check, never a filename-based guess.
        val original = resolver.openInputStream(Uri.parse(song.uri))?.use {
            digest(it)
        } ?: throw IOException("Cannot read the currently playing MP3.")
        for (tree in trees) {
            coroutineContext.ensureActive()
            val direct = sourceDocumentWithinTree(song, tree)
            if (direct != null && matches(direct, original)) return@withContext direct

            // Fast path for regular MediaStore audio, including nested album
            // directories and libraries containing more than 8,000 files.
            val located = relativePath?.let { findAtMediaStorePath(tree, it, name, original) }
            if (located != null) return@withContext located

            // Providers without RELATIVE_PATH, or virtual providers with
            // unusual IDs, still work using the user-authorized subtree only.
            val scanned = findByFilenameRecursively(tree, name, original)
            if (scanned != null) return@withContext scanned
        }
        null
    }

    private fun indexedLocation(song: Song): Pair<String?, String?> {
        val uri = Uri.parse(song.uri)
        val mediaStoreInfo = if (android.os.Build.VERSION.SDK_INT >= 29 &&
            uri.scheme == "content" && uri.authority == "media"
        ) runCatching {
            resolver.query(uri, arrayOf(
                OpenableColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH
            ), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) null else {
                    val filenameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val locationColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    val filename = if (filenameColumn >= 0 && !cursor.isNull(filenameColumn))
                        cursor.getString(filenameColumn) else null
                    val location = if (locationColumn >= 0 && !cursor.isNull(locationColumn))
                        cursor.getString(locationColumn) else null
                    filename to location
                }
            }
        }.getOrNull() else null

        val filename = mediaStoreInfo?.first ?: runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')
        return filename to mediaStoreInfo?.second
    }

    private suspend fun findAtMediaStorePath(
        tree: Uri, relativePath: String, filename: String, original: Signature
    ): Uri? {
        val root = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull()
            ?: return null
        // A Music grant is a prefix of Music/Artist/Album, not merely an
        // unrelated directory with the same name as a track.
        val subfolders = lyricsSubfoldersUnderParent(relativePath, root) ?: return null
        var parent = root
        for (folder in subfolders) {
            coroutineContext.ensureActive()
            parent = findChildFolder(tree, parent, folder) ?: return null
        }
        return findMatchingChild(tree, parent, filename, original)
    }

    private suspend fun findChildFolder(tree: Uri, parent: String, name: String): String? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        return runCatching {
            resolver.query(children, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null)?.use { cursor ->
                var caseInsensitive: String? = null
                while (cursor.moveToNext()) {
                    coroutineContext.ensureActive()
                    if (cursor.getString(2) != DocumentsContract.Document.MIME_TYPE_DIR) continue
                    val foundName = cursor.getString(1) ?: continue
                    if (foundName == name) return@use cursor.getString(0)
                    if (caseInsensitive == null && foundName.equals(name, ignoreCase = true)) {
                        caseInsensitive = cursor.getString(0)
                    }
                }
                caseInsensitive
            }
        }.getOrNull()
    }

    private suspend fun findMatchingChild(
        tree: Uri, parent: String, filename: String, original: Signature
    ): Uri? {
        val candidates = ArrayList<Uri>(2)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        val cursor = runCatching {
            resolver.query(children, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ), null, null, null)
        }.getOrNull() ?: return null
        cursor.use {
            while (it.moveToNext()) {
                coroutineContext.ensureActive()
                if (it.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR ||
                    !filename.equals(it.getString(1), ignoreCase = true)) continue
                val size = if (it.isNull(3)) -1L else it.getLong(3)
                if (size < 0 || size == original.size) {
                    val id = it.getString(0) ?: continue
                    candidates.add(DocumentsContract.buildDocumentUriUsingTree(tree, id))
                }
            }
        }
        for (candidate in candidates) {
            coroutineContext.ensureActive()
            if (matches(candidate, original)) return candidate
        }
        return null
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
     * Fallback only: search authorized descendant folders without asking for
     * each album. Validate potential matches as they are found, rather than
     * truncating after 30 identically named files.
     */
    private suspend fun findByFilenameRecursively(
        tree: Uri, filename: String, original: Signature
    ): Uri? {
        val root = DocumentsContract.getTreeDocumentId(tree)
        val queue = ArrayDeque<String>().apply { add(root) }
        val seenFolders = HashSet<String>()
        var examined = 0
        // Direct relative-path lookup handles normal large MediaStore folders.
        // Keep fallback bounded for SAF/cloud providers that may be slow.
        while (queue.isNotEmpty() && examined < 80_000) {
            coroutineContext.ensureActive()
            val parent = queue.removeFirst()
            if (!seenFolders.add(parent)) continue
            val candidates = ArrayList<Uri>(2)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
            val cursor = runCatching {
                resolver.query(children, arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ), null, null, null)
            }.getOrNull() ?: continue
            cursor.use {
                while (it.moveToNext() && examined < 80_000) {
                    coroutineContext.ensureActive()
                    examined++
                    val id = it.getString(0) ?: continue
                    if (it.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        queue.add(id)
                    } else if (filename.equals(it.getString(1), ignoreCase = true)) {
                        val size = if (it.isNull(3)) -1L else it.getLong(3)
                        if (size < 0 || size == original.size) {
                            candidates.add(DocumentsContract.buildDocumentUriUsingTree(tree, id))
                        }
                    }
                }
            }
            for (candidate in candidates) {
                coroutineContext.ensureActive()
                if (matches(candidate, original)) return candidate
            }
        }
        return null
    }
}

/**
 * Determine the folders *below* an already-authorized SAF parent.
 * Example: primary:Music + Music/Artist/Album/ => Artist, Album.
 * Return null when that selected parent does not contain the song.
 */
internal fun lyricsSubfoldersUnderParent(relativePath: String, treeDocumentId: String): List<String>? {
    val separator = treeDocumentId.indexOf(':')
    if (separator < 0) return null
    val parent = treeDocumentId.substring(separator + 1)
        .replace('\\', '/').split('/').filter(String::isNotBlank)
    val location = relativePath.replace('\\', '/').split('/').filter(String::isNotBlank)
    if ((parent + location).any { it == "." || it == ".." }) return null
    if (parent.size > location.size) return null
    if (!parent.indices.all { parent[it].equals(location[it], ignoreCase = true) }) return null
    return location.drop(parent.size)
}

}
