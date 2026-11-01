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

    /** Reject unsupported formats before queuing a metadata write. */
    suspend fun requireSupportedSong(song: Song) = withContext(Dispatchers.IO) {
        require(EmbeddedAudioFormat.fromFilename(indexedLocation(song).filename.orEmpty()) != null) {
            "Metadata lyrics can currently be saved only to MP3, FLAC and M4A files."
        }
    }

    /** Resolve the original song inside a persistently authorized parent folder. */
    suspend fun find(song: Song): Uri? = withContext(Dispatchers.IO) {
        val trees = authorizedTrees()
        if (trees.isEmpty()) return@withContext null
        val location = indexedLocation(song)
        val filename = location.filename?.takeIf {
            EmbeddedAudioFormat.fromFilename(it) != null
        } ?: throw IOException("This song format cannot store lyrics yet.")

        // Compare bytes from the playing MediaStore URI against the exact SAF
        // audio document before writing. A matching filename is never sufficient.
        val original = resolver.openInputStream(Uri.parse(song.uri))?.use { digest(it) }
            ?: throw IOException("Cannot read the currently playing song.")
        for (tree in trees) {
            coroutineContext.ensureActive()
            val direct = sourceDocumentWithinTree(song, tree)
            if (direct != null && matches(direct, original)) return@withContext direct

            val rootId = runCatching { DocumentsContract.getTreeDocumentId(tree) }
                .getOrNull() ?: continue
            val parentOptions = lyricsCandidateParentPaths(
                location.relativePath, location.absolutePath, rootId
            )
            for (subfolders in parentOptions) {
                coroutineContext.ensureActive()
                // Android ExternalStorageProvider uses stable path-based IDs,
                // even for Music/Genre/Artist/Album deeply nested in the tree.
                // This avoids a multi-thousand-file directory walk.
                val directPath = documentUriForNestedSong(tree, rootId, subfolders, filename)
                if (directPath != null && matches(directPath, original))
                    return@withContext directPath

                // SAF providers can use opaque IDs or case-different names:
                // enumerate each directory by name without guessing IDs.
                val nested = findAtMediaStorePath(tree, rootId, subfolders, filename, original)
                if (nested != null) return@withContext nested
            }

            // The selected tree remains the only scope of fallback discovery;
            // no unrestricted filesystem access and no permission escalation.
            val scanned = findByFilenameRecursively(tree, filename, original)
            if (scanned != null) return@withContext scanned
        }
        null
    }

    private data class IndexedLocation(
        val filename: String?, val relativePath: String?, val absolutePath: String?
    )

    private fun indexedLocation(song: Song): IndexedLocation {
        val uri = Uri.parse(song.uri)
        // Android's media provider can return RELATIVE_PATH (the directory)
        // and DATA (a location hint only; never open this filesystem path).
        // Query separately: some OEM providers reject either column.
        val displayName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null }
        }.getOrNull()
        fun mediaColumn(column: String): String? {
            if (uri.scheme != "content" || uri.authority != "media") return null
            return runCatching {
                resolver.query(uri, arrayOf(column), null, null, null)
                    ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null }
            }.getOrNull()
        }
        val absolute = mediaColumn(MediaStore.MediaColumns.DATA)
        val relative = mediaColumn(MediaStore.MediaColumns.RELATIVE_PATH)
        // Missing DISPLAY_NAME must not force a folder picker for every song.
        val name = displayName ?: absolute?.substringAfterLast('/')
            ?: uri.lastPathSegment?.substringAfterLast('/')
        return IndexedLocation(name, relative, absolute)
    }

    private fun documentUriForNestedSong(
        tree: Uri, rootId: String, subfolders: List<String>, filename: String
    ): Uri? {
        if (tree.authority != "com.android.externalstorage.documents") return null
        val path = (listOf(rootId) + subfolders + filename).joinToString("/")
        return runCatching {
            DocumentsContract.buildDocumentUriUsingTree(tree, path)
        }.getOrNull()
    }

    private suspend fun findAtMediaStorePath(
        tree: Uri, root: String, subfolders: List<String>,
        filename: String, original: Signature
    ): Uri? {
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
                    if (caseInsensitive == null && lyricsSameName(foundName, name)) {
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
                    !lyricsSameName(filename, it.getString(1))) continue
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
            if (count > 384L * 1024 * 1024) throw IOException("This song exceeds the safe lyrics-embedding size limit.")
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
                    } else if (lyricsSameName(filename, it.getString(1))) {
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

/** Candidate subdirectory chains relative to the SAF folder the user picked. */
internal fun lyricsCandidateParentPaths(
    mediaRelativePath: String?, mediaAbsolutePath: String?, rootDocumentId: String
): List<List<String>> {
    val candidates = ArrayList<List<String>>(2)
    for (path in listOfNotNull(mediaRelativePath, lyricsPathFromAbsolute(mediaAbsolutePath))) {
        val subfolders = lyricsSubfoldersUnderParent(path, rootDocumentId) ?: continue
        if (subfolders !in candidates) candidates.add(subfolders)
    }
    return candidates
}

/** Only use DATA as a directory hint for known Android external-storage mounts. */
internal fun lyricsPathFromAbsolute(path: String?): String? {
    val clean = path?.replace('\\', '/') ?: return null
    if (clean.split('/').any { it == "." || it == ".." }) return null
    val storage = when {
        clean.startsWith("/storage/emulated/") -> {
            val rest = clean.removePrefix("/storage/emulated/")
            if (!rest.startsWith("0/")) return null
            rest.removePrefix("0/")
        }
        clean.startsWith("/storage/") -> {
            val rest = clean.removePrefix("/storage/")
            if (!rest.contains('/')) return null
            rest.substringAfter('/')
        }
        clean.startsWith("/mnt/media_rw/") -> {
            val rest = clean.removePrefix("/mnt/media_rw/")
            if (!rest.contains('/')) return null
            rest.substringAfter('/')
        }
        else -> return null
    }
    return storage.substringBeforeLast('/', "").takeIf { it.isNotBlank() }
}

internal fun lyricsSameName(a: String?, b: String?): Boolean =
    a != null && b != null &&
        java.text.Normalizer.normalize(a, java.text.Normalizer.Form.NFC)
            .equals(java.text.Normalizer.normalize(b, java.text.Normalizer.Form.NFC),
                ignoreCase = true)
