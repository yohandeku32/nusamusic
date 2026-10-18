package com.yohandeku32.nusamusic.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class ImmersiveArtworkBytes(val key: String, val cover: ByteArray, val blur: ByteArray, val backdrop: Int)

/** One bounded, atomic launch preview, never an ever-growing copy of the library. */
internal class ImmersiveArtworkDiskCache(private val file: File) {
    fun clear() { file.delete() }

    fun read(key: String): ImmersiveArtworkBytes? = runCatching {
        if (!file.isFile || file.length() > MAX_BYTES) return null
        val entries = mutableMapOf<String, ByteArray>()
        var total = 0
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name !in setOf("metadata", "cover", "blur") || entries.containsKey(entry.name)) return null
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_BYTES) return null
                    output.write(buffer, 0, count)
                }
                entries[entry.name] = output.toByteArray()
                entry = zip.nextEntry
            }
        }
        val metadata = Properties().apply { load(ByteArrayInputStream(entries["metadata"] ?: return null)) }
        if (metadata.getProperty("version") != VERSION || metadata.getProperty("key") != key) return null
        ImmersiveArtworkBytes(key,
            entries["cover"]?.takeIf { it.isNotEmpty() } ?: return null,
            entries["blur"]?.takeIf { it.isNotEmpty() } ?: return null,
            metadata.getProperty("backdrop").toInt())
    }.getOrNull()

    fun write(value: ImmersiveArtworkBytes): Boolean {
        var temporary: File? = null
        return try {
            if (value.cover.isEmpty() || value.blur.isEmpty() || value.cover.size.toLong() + value.blur.size > MAX_BYTES - 8192) return false
            val directory = file.absoluteFile.parentFile ?: return false
            directory.mkdirs()
            temporary = File.createTempFile("immersive-preview-", ".tmp", directory)
            val metadata = ByteArrayOutputStream().also { output ->
                Properties().apply {
                    setProperty("version", VERSION)
                    setProperty("key", value.key)
                    setProperty("backdrop", value.backdrop.toString())
                }.store(output, null)
            }.toByteArray()
            ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                for ((name, bytes) in listOf("metadata" to metadata, "cover" to value.cover, "blur" to value.blur)) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            temporary?.delete()
        }
    }

    private companion object {
        // Version 7 refreshes previews for the slightly softer, less textured real-artwork blur.
        const val VERSION = "7"
        const val MAX_BYTES = 4 * 1024 * 1024
    }
}
