package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song
import java.text.Normalizer
import java.util.Locale

data class ArtistAlbum(val key: String, val title: String, val tracks: List<Song>)

/** Uses local tags only; remote catalog entries must never become playable tracks. */
object ArtistLibrary {
    private fun key(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFC)
        .lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    fun tracks(songs: List<Song>, artist: String): List<Song> {
        val target = key(ArtistNameUtils.firstArtist(artist))
        if (target.isBlank() || target == "unknown artist" || target == "<unknown>") return emptyList()
        return songs.filter { key(ArtistNameUtils.firstArtist(it.artist)) == target }
            .distinctBy { it.uri }
            .sortedWith(compareBy<Song> { key(it.title) }.thenBy { it.uri })
    }

    fun mostPlayed(tracks: List<Song>, counts: Map<String, Long>): List<Song> = tracks
        .filter { (counts[it.uri] ?: 0L) > 0L }
        .sortedWith(compareByDescending<Song> { counts[it.uri] ?: 0L }.thenBy { key(it.title) })
        .take(5)

    fun albums(tracks: List<Song>): List<ArtistAlbum> = tracks
        .groupBy { key(it.album).ifBlank { "<unknown>" } }
        .map { (key, songs) -> ArtistAlbum(key, songs.first().album.trim(), songs) }
        .sortedBy { key(it.title) }
}
