package com.yohandeku32.nusamusic.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

internal data class WaveformSource(val uri: String, val size: Long, val modifiedMs: Long, val durationMs: Long) {
    val cacheable: Boolean get() = size >= 0 && modifiedMs > 0
    val key: String get() = MessageDigest.getInstance("SHA-256")
        .digest("$uri\u0000$size\u0000$modifiedMs\u0000$durationMs".toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal class AudioWaveformCache(private val directory: File) {
    private val mutex = Mutex()
    private val memory = LinkedHashMap<String, AudioWaveform>(32, 0.75f, true)

    suspend fun load(source: WaveformSource, decode: suspend () -> AudioWaveform?): AudioWaveform? = mutex.withLock {
        currentCoroutineContext().ensureActive()
        if (!source.cacheable) return@withLock decode()
        val key = source.key
        memory[key]?.let { return@withLock it }
        val target = File(directory, "$key.wave")
        val disk = runCatching {
            if (target.length() != 1044L) return@runCatching null
            DataInputStream(target.inputStream().buffered()).use { input ->
                if (input.readInt() != 0x41525731 || input.readInt() != 1) return@use null
                val duration = input.readLong()
                if (duration <= 0 || input.readInt() != 256) return@use null
                val peaks = FloatArray(256) { input.readFloat() }
                if (peaks.any { !it.isFinite() || it !in 0f..1f }) null else AudioWaveform(duration, peaks)
            }
        }.getOrNull()
        val result = disk ?: decode() ?: return@withLock null
        currentCoroutineContext().ensureActive()
        memory[key] = result
        while (memory.size > 32) memory.remove(memory.keys.first())
        runCatching {
            directory.mkdirs()
            if (disk == null) {
                val temporary = File.createTempFile("peaks-", ".tmp", directory)
                try {
                    DataOutputStream(temporary.outputStream().buffered()).use { output ->
                        output.writeInt(0x41525731); output.writeInt(1)
                        output.writeLong(result.durationMs); output.writeInt(result.peaks.size)
                        result.peaks.forEach(output::writeFloat)
                    }
                    Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } finally { temporary.delete() }
            }
            target.setLastModified(System.currentTimeMillis())
            directory.listFiles { file -> file.extension == "wave" }?.sortedByDescending { it.lastModified() }
                ?.drop(256)?.forEach(File::delete)
        }
        result
    }
}
