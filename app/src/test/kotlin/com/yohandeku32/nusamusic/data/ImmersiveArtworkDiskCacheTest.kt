package com.yohandeku32.nusamusic.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ImmersiveArtworkDiskCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private val sample = ImmersiveArtworkBytes("album:12", byteArrayOf(1, 2, 3), byteArrayOf(4, 5), 0xFF123456.toInt())

    @Test fun launchPreviewSurvivesNewInstanceWithAllPresentationData() {
        val file = File(temporary.newFolder(), "preview.zip")
        assertTrue(ImmersiveArtworkDiskCache(file).write(sample))
        val restored = ImmersiveArtworkDiskCache(file).read(sample.key)!!
        assertArrayEquals(sample.cover, restored.cover)
        assertArrayEquals(sample.blur, restored.blur)
        assertEquals(sample.backdrop, restored.backdrop)
    }

    @Test fun anotherAlbumNeverReceivesThePreviousCover() {
        val cache = ImmersiveArtworkDiskCache(File(temporary.newFolder(), "preview.zip"))
        cache.write(sample)
        assertNull(cache.read("album:13"))
        assertNull(cache.read("uri:12"))
    }

    @Test fun oldBlurPreviewIsRebuiltAfterTheRenderingChange() {
        val file = File(temporary.newFolder(), "preview.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("metadata"))
            Properties().apply {
                setProperty("version", "1")
                setProperty("key", sample.key)
                setProperty("backdrop", sample.backdrop.toString())
            }.store(zip, null)
            zip.closeEntry()
            for ((name, bytes) in listOf("cover" to sample.cover, "blur" to sample.blur)) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val cache = ImmersiveArtworkDiskCache(file)
        assertNull(cache.read(sample.key))
        assertTrue(cache.write(sample))
        assertArrayEquals(sample.blur, cache.read(sample.key)!!.blur)
    }

    @Test fun replacementKeepsOnlyTheLastPreviewAndNoTemporaryFiles() {
        val directory = temporary.newFolder()
        val cache = ImmersiveArtworkDiskCache(File(directory, "preview.zip"))
        cache.write(sample)
        assertTrue(cache.write(sample.copy(key = "uri:content://song/日本語")))
        assertNull(cache.read(sample.key))
        assertNotNull(cache.read("uri:content://song/日本語"))
        assertEquals(1, directory.listFiles()!!.size)
    }

    @Test fun missingTruncatedAndIncompletePreviewsAreCacheMisses() {
        val file = File(temporary.newFolder(), "preview.zip")
        val cache = ImmersiveArtworkDiskCache(file)
        assertNull(cache.read(sample.key))
        file.writeText("truncated")
        assertNull(cache.read(sample.key))
        ZipOutputStream(file.outputStream()).use {
            it.putNextEntry(ZipEntry("cover"))
            it.write(sample.cover)
            it.closeEntry()
        }
        assertNull(cache.read(sample.key))
        assertTrue(cache.write(sample))
        assertNotNull(cache.read(sample.key))
    }

    @Test fun oversizedOrEmptyWriteDoesNotDestroyTheWorkingPreview() {
        val cache = ImmersiveArtworkDiskCache(File(temporary.newFolder(), "preview.zip"))
        cache.write(sample)
        assertFalse(cache.write(sample.copy(cover = ByteArray(4 * 1024 * 1024))))
        assertFalse(cache.write(sample.copy(blur = byteArrayOf())))
        assertNotNull(cache.read(sample.key))
    }

    @Test fun highlyCompressedOversizedFileIsRejectedOnRead() {
        val file = File(temporary.newFolder(), "preview.zip")
        ZipOutputStream(file.outputStream()).use {
            it.putNextEntry(ZipEntry("cover"))
            it.write(ByteArray(5 * 1024 * 1024))
            it.closeEntry()
        }
        assertNull(ImmersiveArtworkDiskCache(file).read(sample.key))
    }

    @Test fun invalidPreviewCanBeClearedAndReplaced() {
        val cache = ImmersiveArtworkDiskCache(File(temporary.newFolder(), "preview.zip"))
        cache.write(sample)
        cache.clear()
        assertNull(cache.read(sample.key))
        assertTrue(cache.write(sample))
    }
}
