package com.yohandeku32.nusamusic.data

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties

/** Only public biography data is saved here; API keys are never persisted. */
class ArtistBiographyDiskCache(private val directory: File) {
    private fun fileFor(artist: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(ArtistNameUtils.cacheKey(artist).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.properties")
    }

    fun read(artist: String): ArtistBiography? = runCatching {
        val values = Properties().apply {
            fileFor(artist).reader(Charsets.UTF_8).use { load(it) }
        }
        if (values.getProperty("version") != "1") return null
        val name = values.getProperty("artist").orEmpty()
        val text = values.getProperty("text").orEmpty()
        val language = values.getProperty("language").orEmpty()
        val url = values.getProperty("url").orEmpty()
        val source = java.net.URI(url)
        if (name.isBlank() || text.isBlank() || language.isBlank() ||
            source.scheme !in setOf("https", "http") || source.host !in setOf("www.last.fm", "last.fm")
        ) return null
        ArtistBiography(name, text, language, url)
    }.getOrNull()

    fun write(artist: String, biography: ArtistBiography) {
        var temporary: File? = null
        try {
            directory.mkdirs()
            val destination = fileFor(artist)
            temporary = File.createTempFile(destination.name, ".tmp", directory)
            val values = Properties().apply {
                setProperty("version", "1")
                setProperty("artist", biography.artistName)
                setProperty("text", biography.text)
                setProperty("language", biography.sourceLanguage)
                setProperty("url", biography.sourceUrl)
            }
            temporary.writer(Charsets.UTF_8).use { values.store(it, null) }
            try {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (_: Exception) {
            // A full disk must not prevent displaying a successfully fetched biography.
        } finally {
            temporary?.delete()
        }
    }
}
