package com.yohandeku32.nusamusic.data

import java.io.File
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AudioWaveformCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private val source = WaveformSource("content://audio/123", 1000, 5000, 4000)
    private fun waveform(level: Float = 0.5f) = AudioWaveform(4000, FloatArray(256) { level })

    @Test fun memoryAndProcessRestoredDiskHitsDoNotDecodeAgain() = runBlocking {
        var calls = 0
        val cache = AudioWaveformCache(temporary.root)
        val first = cache.load(source) { calls++; waveform() }
        assertSame(first, cache.load(source) { calls++; waveform() })
        val restored = AudioWaveformCache(temporary.root).load(source) { calls++; waveform() }
        assertArrayEquals(first!!.peaks, restored!!.peaks, 0f)
        assertEquals(1, calls)
        assertEquals(1044L, temporary.root.listFiles()!!.single().length())
    }

    @Test fun changedSourceMetadataInvalidatesOldPeaks() = runBlocking {
        val cache = AudioWaveformCache(temporary.root)
        assertEquals(0.2f, cache.load(source) { waveform(0.2f) }!!.peaks.first(), 0f)
        assertEquals(0.8f, cache.load(source.copy(modifiedMs = 6000)) { waveform(0.8f) }!!.peaks.first(), 0f)
    }

    @Test fun anUnversionedProviderIsNotPersistentlyCached() = runBlocking {
        var calls = 0
        val cache = AudioWaveformCache(temporary.root)
        repeat(2) { cache.load(source.copy(modifiedMs = 0)) { calls++; waveform() } }
        assertEquals(2, calls)
        assertTrue(temporary.root.listFiles().isNullOrEmpty())
    }

    @Test fun corruptOrPartialCacheFilesAreRecomputed() = runBlocking {
        AudioWaveformCache(temporary.root).load(source) { waveform() }
        temporary.root.listFiles()!!.single().writeBytes(byteArrayOf(1, 2, 3))
        val restored = AudioWaveformCache(temporary.root).load(source) { waveform(0.9f) }
        assertEquals(0.9f, restored!!.peaks.first(), 0f)
    }

    @Test fun concurrentConsumersShareOneCompletedDecode() = runBlocking {
        val cache = AudioWaveformCache(temporary.root)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val decode: suspend () -> AudioWaveform = { calls++; started.complete(Unit); release.await(); waveform() }
        val first = async { cache.load(source, decode) }
        started.await()
        val second = async { cache.load(source, decode) }
        release.complete(Unit)
        assertSame(first.await(), second.await())
        assertEquals(1, calls)
    }

    @Test fun cancelledAndUnavailableDecodesNeverBecomeCachedPeaks() = runBlocking {
        val cache = AudioWaveformCache(temporary.root)
        val started = CompletableDeferred<Unit>()
        val job = launch { cache.load(source) { started.complete(Unit); awaitCancellation() } }
        started.await(); job.cancelAndJoin()
        assertTrue(temporary.root.listFiles().isNullOrEmpty())
        assertNull(cache.load(source) { null })
        assertNotNull(cache.load(source) { waveform() })
    }

    @Test fun diskEvictionIsBoundedAndLeavesUnrelatedFilesAlone() = runBlocking {
        repeat(270) { File(temporary.root, "$it.wave").apply { writeText("old"); setLastModified(1) } }
        val unrelated = File(temporary.root, "keep.txt").apply { writeText("fixture") }
        AudioWaveformCache(temporary.root).load(source) { waveform() }
        assertTrue(temporary.root.listFiles { file -> file.extension == "wave" }!!.size <= 256)
        assertTrue(unrelated.isFile)
    }
}
