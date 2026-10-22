package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A single parent SAF grant must cover nested artist/album folders. */
class LyricsFolderPathTest {
    @Test fun musicParentCoversAnyAlbumDepth() {
        assertEquals(
            listOf("Artist", "2026", "Album"),
            lyricsSubfoldersUnderParent("Music/Artist/2026/Album/", "primary:Music")
        )
        assertEquals(
            emptyList<String>(),
            lyricsSubfoldersUnderParent("Music/", "primary:Music")
        )
    }

    @Test fun nestedParentAndRemovableVolumeAreSupported() {
        assertEquals(
            listOf("Album"),
            lyricsSubfoldersUnderParent("Music/Artist/Album/", "0123-4567:Music/Artist")
        )
        assertEquals(
            listOf("Music", "Artist", "Album"),
            lyricsSubfoldersUnderParent("Music/Artist/Album/", "primary:")
        )
    }

    @Test fun unrelatedOrTooDeepGrantsDoNotMatch() {
        assertNull(lyricsSubfoldersUnderParent("Music/Artist/", "primary:Download"))
        assertNull(lyricsSubfoldersUnderParent("Music/Artist/", "primary:Music/Other"))
        assertNull(lyricsSubfoldersUnderParent("Music/Artist/", "primary:Music/Artist/Album"))
        assertNull(lyricsSubfoldersUnderParent("Music/", "not-a-tree-document-id"))
    }

    @Test fun rejectTraversalAndHandleMixedCase() {
        assertNull(lyricsSubfoldersUnderParent("Music/../Download/", "primary:Music"))
        assertNull(lyricsSubfoldersUnderParent("Music/Artist/", "primary:Music/.."))
        assertEquals(
            listOf("Artist"),
            lyricsSubfoldersUnderParent("music/Artist/", "primary:Music")
        )
    }
}
