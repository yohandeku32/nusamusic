package com.yohandeku32.nusamusic.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val durationMs: Long,
    val albumId: Long,
    val dateAddedMs: Long = 0L
)
