package com.yohandeku32.nusamusic.data

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.security.MessageDigest
import java.util.Properties

data class StoredLyrics(
    val content: String,
    val source: String,
    val plain: Boolean = false,
    val instrumental: Boolean = false,
    val checkedAt: Long = System.currentTimeMillis()
)

class LyricsDiskStore(private val directory: File, private val maxEntries: Int? = null) {
    companion object {
        const val MISSING_TTL_MS = 6 * 60 * 60 * 1_000L
        fun digest(key: String): String = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
    private fun file(key: String) = File(directory, "${digest(key)}.properties")

    @Synchronized fun read(key: String, now: Long = System.currentTimeMillis()): StoredLyrics? = runCatching {
        val target = file(key)
        if (!target.isFile || target.length() > LyricsDecoder.MAX_BYTES * 3L) return null
        val props = Properties().apply { target.reader(Charsets.UTF_8).use { load(it) } }
        if (props.getProperty("version") != "1") return null
        val entry = StoredLyrics(props.getProperty("content") ?: return null,
            props.getProperty("source") ?: return null, props.getProperty("plain").toBoolean(),
            props.getProperty("instrumental").toBoolean(), props.getProperty("checkedAt").toLong())
        if (entry.content.isEmpty() && !entry.instrumental && (now < entry.checkedAt || now - entry.checkedAt >= MISSING_TTL_MS)) return null
        entry
    }.getOrNull()

    @Synchronized fun write(key: String, entry: StoredLyrics) {
        require(entry.content.toByteArray(Charsets.UTF_8).size <= LyricsDecoder.MAX_BYTES)
        check(directory.isDirectory || directory.mkdirs())
        val target = file(key)
        val temporary = File.createTempFile(target.name, ".tmp", directory)
        try {
            Properties().apply {
                setProperty("version", "1"); setProperty("content", entry.content)
                setProperty("source", entry.source); setProperty("plain", entry.plain.toString())
                setProperty("instrumental", entry.instrumental.toString()); setProperty("checkedAt", entry.checkedAt.toString())
            }.also { props -> temporary.writer(Charsets.UTF_8).use { props.store(it, null) } }
            try { Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        } finally { temporary.delete() }
        maxEntries?.let { limit ->
            directory.listFiles { f -> f.extension == "properties" && f != target }?.sortedByDescending { it.lastModified() }
                ?.drop((limit - 1).coerceAtLeast(0))?.forEach { it.delete() }
        }
    }
}
