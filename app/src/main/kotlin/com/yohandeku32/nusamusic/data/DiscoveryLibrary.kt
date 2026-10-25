package com.yohandeku32.nusamusic.data

import com.yohandeku32.nusamusic.model.Song

internal data class DiscoveryArtist(
    val key: String, val name: String, val tracks: List<Song>, val plays: Long
)

internal data class DiscoveryAlbum(
    val key: String, val albumKey: String, val artist: String,
    val title: String, val tracks: List<Song>
)

internal data class DiscoveryContent(
    val albums: List<DiscoveryAlbum>, val artists: List<DiscoveryArtist>,
    val recent: List<Song>, val hasListeningHistory: Boolean
)

internal object DiscoveryLibrary {
    fun build(songs: List<Song>, counts: Map<String, Long>, recent: Map<String, Long>): DiscoveryContent {
        val available = songs.distinctBy { it.uri }
        val artists = available.groupBy { ArtistNameUtils.cacheKey(it.artist) }
            .filterKeys { it.isNotBlank() && it != "unknown artist" && it != "<unknown>" }
            .map { (key, tracks) ->
                DiscoveryArtist(key, ArtistNameUtils.firstArtist(tracks.first().artist),
                    tracks.sortedBy { it.title.lowercase(java.util.Locale.ROOT) },
                    tracks.sumOf { counts[it.uri]?.coerceAtLeast(0L) ?: 0L })
            }.sortedWith(compareByDescending<DiscoveryArtist> { it.plays }
                .thenBy { it.key })
        val albums = artists.flatMap { artist ->
            ArtistLibrary.albums(artist.tracks).filter { it.title.isNotBlank() }.map { album ->
                DiscoveryAlbum(artist.key + "\u0000" + album.key, album.key,
                    artist.name, album.title, album.tracks)
            }
        }.sortedWith(compareByDescending<DiscoveryAlbum> { album ->
            album.tracks.maxOf { it.dateAddedMs }
        }.thenBy { it.key }).take(12)
        val recentSongs = available.filter { (recent[it.uri] ?: 0L) > 0L }
            .sortedWith(compareByDescending<Song> { recent[it.uri] ?: 0L }.thenBy { it.uri })
            .take(20)
        return DiscoveryContent(albums, artists.take(12), recentSongs, artists.any { it.plays > 0L })
    }
}
