package com.yohandeku32.nusamusic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.yohandeku32.nusamusic.model.Song

class MusicRepository(private val context: Context) {
    fun loadSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val sort = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown title",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    album = cursor.getString(albumCol) ?: "Unknown album",
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    durationMs = cursor.getLong(durationCol),
                    albumId = cursor.getLong(albumIdCol)
                )
            }
        }
        return songs
    }
}
