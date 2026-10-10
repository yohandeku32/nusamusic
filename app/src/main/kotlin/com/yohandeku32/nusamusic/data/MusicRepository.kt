package com.yohandeku32.nusamusic.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.yohandeku32.nusamusic.model.Song
import java.util.ArrayDeque
import java.util.Locale

class MusicRepository(private val context: Context) {
    companion object {
        private const val MIN_TRACK_DURATION_MS = 10_000L

        private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
            "aac",
            "alac",
            "flac",
            "m4a",
            "mp3",
            "ogg",
            "opus",
            "wav",
            "wma"
        )
    }

    /**
     * Loads music indexed by MediaStore and augments it with any SAF folders
     * the user explicitly granted to Nusa.
     *
     * The normal MediaStore collection remains the primary source so existing
     * local-library behavior is preserved. Manually selected folders are an
     * additional source and are recursively scanned.
     */
    fun loadSongs(
        extraFolderUris: Set<String> = emptySet(),
        cachedSongs: List<Song> = emptyList()
    ): List<Song> {
        val songs = loadMediaStoreSongs().toMutableList()
        val existingUris = songs.mapTo(HashSet()) { it.uri }

        val existingKeys = songs
            .mapTo(HashSet()) { songFingerprint(it.title, it.artist, it.album, it.durationMs) }

        val cachedSongsByUri = cachedSongs.associateBy { it.uri }

        extraFolderUris.forEach { folderUriString ->
            val folderSongs = runCatching {
                loadFolderSongs(Uri.parse(folderUriString), cachedSongsByUri)
            }.getOrElse { emptyList() }

            for (song in folderSongs) {
                val key = songFingerprint(
                    song.title,
                    song.artist,
                    song.album,
                    song.durationMs
                )

                if (existingUris.add(song.uri) && existingKeys.add(key)) {
                    songs += song
                }
            }
        }

        return songs.sortedBy { it.title.lowercase(Locale.ROOT) }
    }

    private fun loadMediaStoreSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection =
            MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                MediaStore.Audio.Media.DURATION + " >= ?"
        val selectionArgs = arrayOf(MIN_TRACK_DURATION_MS.toString())
        val sort = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sort
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DATE_ADDED
                )

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val durationMs = cursor.getLong(durationCol)

                    if (durationMs < MIN_TRACK_DURATION_MS) continue

                    songs += Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown title",
                        artist = cursor.getString(artistCol) ?: "Unknown artist",
                        album = cursor.getString(albumCol) ?: "Unknown album",
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        durationMs = durationMs,
                        albumId = cursor.getLong(albumIdCol),
                        dateAddedMs = cursor.getLong(dateAddedCol) * 1_000L
                    )
                }
            }
        }

        return songs
    }

    private fun loadFolderSongs(
        treeUri: Uri,
        cachedSongsByUri: Map<String, Song>
    ): List<Song> {
        val resolver = context.contentResolver
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val pendingDocumentIds = ArrayDeque<String>()
        val visitedDocumentIds = HashSet<String>()
        val songs = mutableListOf<Song>()

        pendingDocumentIds.add(rootDocumentId)

        while (pendingDocumentIds.isNotEmpty()) {
            val parentDocumentId = pendingDocumentIds.removeFirst()

            if (!visitedDocumentIds.add(parentDocumentId)) continue

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentDocumentId
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            resolver.query(
                childrenUri,
                projection,
                null,
                null,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                )
                val nameCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )
                val mimeCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                val modifiedCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idCol)
                    val displayName =
                        cursor.getString(nameCol)?.trim().orEmpty()
                    val mimeType =
                        cursor.getString(mimeCol)?.lowercase(Locale.ROOT).orEmpty()

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pendingDocumentIds.addLast(documentId)
                        continue
                    }

                    if (!isSupportedAudio(displayName, mimeType)) continue

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        documentId
                    )

                    val modifiedTimeMs = cursor.getLong(modifiedCol)
                    val cachedSong = cachedSongsByUri[documentUri.toString()]
                    val song = cachedSong?.takeIf {
                        // SAF providers expose a modified timestamp for most files.
                        // Reuse parsed tags when the file has not changed; providers
                        // returning 0 still get a full metadata read for correctness.
                        modifiedTimeMs > 0L && it.dateAddedMs == modifiedTimeMs
                    } ?: readSongFromDocument(
                        documentUri = documentUri,
                        displayName = displayName,
                        modifiedTimeMs = modifiedTimeMs
                    )

                    song?.let { songs += it }
                }
            }
        }

        return songs
    }

    private fun readSongFromDocument(
        documentUri: Uri,
        displayName: String,
        modifiedTimeMs: Long
    ): Song? {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, documentUri)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L

            if (durationMs < MIN_TRACK_DURATION_MS) return null

            val title = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: displayName.substringBeforeLast('.', displayName)

            val artist = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown artist"

            val album = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown album"

            val uriString = documentUri.toString()

            Song(
                id = stableDocumentId(uriString),
                title = title,
                artist = artist,
                album = album,
                uri = uriString,
                durationMs = durationMs,
                albumId = stableDocumentId("$uriString#album"),
                dateAddedMs = modifiedTimeMs
            )
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun isSupportedAudio(
        displayName: String,
        mimeType: String
    ): Boolean {
        if (mimeType.startsWith("audio/")) return true

        val extension = displayName
            .substringAfterLast('.', "")
            .lowercase(Locale.ROOT)

        return extension in SUPPORTED_AUDIO_EXTENSIONS
    }

    private fun songFingerprint(
        title: String,
        artist: String,
        album: String,
        durationMs: Long
    ): String {
        return listOf(
            title.trim().lowercase(Locale.ROOT),
            artist.trim().lowercase(Locale.ROOT),
            album.trim().lowercase(Locale.ROOT),
            durationMs.toString()
        ).joinToString("|")
    }

    private fun stableDocumentId(value: String): Long {
        var hash = 1125899906842597L

        for (character in value) {
            hash = hash * 31L + character.code
        }

        return -(hash and Long.MAX_VALUE).coerceAtLeast(1L)
    }
}
