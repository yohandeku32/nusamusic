package com.yohandeku32.nusamusic.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.yohandeku32.nusamusic.model.Song
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicFolderRefreshTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val tree = DocumentsContract.buildTreeDocumentUri("aruna.music.test", "music")
    private lateinit var provider: FolderProvider
    private lateinit var cached: Song
    private fun document(id: String) = DocumentsContract.buildDocumentUriUsingTree(tree, id)

    @Before fun setup() {
        provider = FolderProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = "aruna.music.test" })
        ShadowContentResolver.registerProviderInternal("aruna.music.test", provider)
        provider.children["music"] = listOf(Entry("album", "Album", DocumentsContract.Document.MIME_TYPE_DIR, 1))
        provider.children["album"] = listOf(Entry("old", "old.mp3", "audio/mpeg", 1000))
        cached = Song(-10, "Old song", "Artist", "Album", document("old").toString(), 180000, -1, 1000)
        ShadowMediaMetadataRetriever.addException(DataSource.toDataSource(context, document("old")),
            IllegalStateException("Unchanged metadata must be reused"))
    }

    @Test fun explicitRefreshFindsUnindexedNestedFolderSongsWhileReusingExistingTags() {
        val repository = MusicRepository(context)
        val fast = repository.loadSongs(setOf(tree.toString()), listOf(cached),
            scanSelectedFolders = false, includeMediaStore = false)
        assertEquals(listOf(cached), fast.songs)
        assertEquals(0, provider.queries)

        provider.children["album"] = provider.children.getValue("album") + Entry("new", "new.flac", "audio/flac", 2000)
        val source = DataSource.toDataSource(context, document("new"))
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_DURATION, "180000")
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_TITLE, "New arrival")
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_ARTIST, "Artist")
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_ALBUM, "Album")

        val refreshed = repository.loadSongs(setOf(tree.toString()), fast.songs,
            scanSelectedFolders = true, includeMediaStore = false)
        assertEquals(setOf("Old song", "New arrival"), refreshed.songs.map { it.title }.toSet())
        assertSame(cached, refreshed.songs.single { it.uri == cached.uri })
        assertTrue(provider.queries >= 2)

        ShadowMediaMetadataRetriever.addException(source, IllegalStateException("Do not reload unchanged tags"))
        val repeated = repository.loadSongs(setOf(tree.toString()), refreshed.songs,
            scanSelectedFolders = true, includeMediaStore = false)
        assertEquals(refreshed.songs, repeated.songs)
        assertEquals(2, repeated.songs.map { it.uri }.distinct().size)
    }

    @Test fun explicitRefreshAlsoReconcilesRemovedFolderSongs() {
        provider.children["album"] = emptyList()
        val refreshed = MusicRepository(context).loadSongs(setOf(tree.toString()), listOf(cached),
            scanSelectedFolders = true, includeMediaStore = false)
        assertTrue(refreshed.songs.isEmpty())
    }

    data class Entry(val id: String, val name: String, val mime: String, val modified: Long)
    class FolderProvider : ContentProvider() {
        val children = mutableMapOf<String, List<Entry>>()
        var queries = 0
        override fun onCreate() = true
        override fun getType(uri: Uri) = DocumentsContract.Document.MIME_TYPE_DIR
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            queries++
            val columns = requireNotNull(projection)
            return MatrixCursor(columns).apply {
                for (entry in children[DocumentsContract.getDocumentId(uri)].orEmpty()) {
                    addRow(columns.map { column -> when (column) {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> entry.id
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> entry.name
                        DocumentsContract.Document.COLUMN_MIME_TYPE -> entry.mime
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED -> entry.modified
                        else -> null
                    } })
                }
            }
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
