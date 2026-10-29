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

    @Test fun candidatePathsUseDeepRelativeOrAbsoluteMusicLocation() {
        val absolute = "/storage/emulated/0/Music/Genre/Artist/Album/Track.mp3"
        assertEquals("Music/Genre/Artist/Album", lyricsPathFromAbsolute(absolute))
        assertEquals(
            listOf(listOf("Genre", "Artist", "Album")),
            lyricsCandidateParentPaths("Music/Genre/Artist/Album/", absolute, "primary:Music")
        )
        assertEquals(
            listOf(listOf("Genre", "Artist", "Album")),
            lyricsCandidateParentPaths(null, absolute, "primary:Music")
        )
        assertEquals(
            listOf(listOf("Artist", "Album")),
            lyricsCandidateParentPaths(
                "Music/Genre/Artist/Album/", absolute, "primary:Music/Genre"
            )
        )
    }

    @Test fun removableSdMusicTreeGetsSameNestedPaths() {
        val absolute = "/storage/ABCD-1234/Music/Rock/Live/2024/song.mp3"
        assertEquals(
            listOf(listOf("Rock", "Live", "2024")),
            lyricsCandidateParentPaths(null, absolute, "ABCD-1234:Music")
        )
        assertEquals(
            listOf(listOf("Rock", "Live", "2024")),
            lyricsCandidateParentPaths("Music/Rock/Live/2024/", absolute, "ABCD-1234:Music")
        )
    }

    @Test fun unrelatedGrantCannotAccidentallyUseMediaPath() {
        assertEquals(
            emptyList<List<String>>(),
            lyricsCandidateParentPaths(
                "Download/Artist/", "/storage/emulated/0/Download/Artist/track.mp3",
                "primary:Music"
            )
        )
        assertNull(lyricsPathFromAbsolute("/private/app/Music/Artist/track.mp3"))
        assertNull(lyricsPathFromAbsolute("/storage/emulated/0/Music/../Download/song.mp3"))
    }

    @Test fun equivalentUnicodeFilenamesMatchWithoutLooseTitleGuess() {
        assertEquals(true, lyricsSameName("Cafe\u0301.mp3", "Café.mp3"))
        assertEquals(true, lyricsSameName("TRACK.MP3", "track.mp3"))
        assertEquals(false, lyricsSameName("Other.mp3", "track.mp3"))
    }

}
