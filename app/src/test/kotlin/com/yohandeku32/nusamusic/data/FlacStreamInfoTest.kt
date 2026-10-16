package com.yohandeku32.nusamusic.data

import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class FlacStreamInfoTest {
    private fun stream(depth: Int, rate: Int): ByteArray {
        val bytes = ByteArray(42)
        byteArrayOf(102, 76, 97, 67, 128.toByte(), 0, 0, 34).copyInto(bytes)
        bytes[18] = (rate ushr 12).toByte()
        bytes[19] = (rate ushr 4).toByte()
        bytes[20] = (((rate and 15) shl 4) or 2 or ((depth - 1) ushr 4)).toByte()
        bytes[21] = ((depth - 1) shl 4).toByte()
        return bytes
    }
    @Test fun readsActualBitDepthAndSampleRate() {
        for (depth in listOf(16, 20, 24, 32)) {
            for (rate in listOf(44100, 96000, 192000)) {
                val info = readFlacCodecInfo(ByteArrayInputStream(stream(depth, rate)))!!
                assertEquals("FLAC", info.codecName)
                assertEquals(depth, info.bitDepth)
                assertEquals(rate, info.sampleRateHz)
                assertEquals(depth >= 24, info.isHiRes)
            }
        }
    }
    @Test fun acceptsId3PrefixAndPartialReads() {
        val id3 = byteArrayOf(73, 68, 51, 4, 0, 0, 0, 0, 0, 3, 1, 2, 3)
        val source = object : ByteArrayInputStream(id3 + stream(24, 96000)) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
                super.read(bytes, offset, minOf(length, 1))
        }
        assertTrue(readFlacCodecInfo(source)!!.isHiRes)
    }
    @Test fun rejectsTruncatedOrInvalidHeadersWithoutGuessingFromExtension() {
        val valid = stream(24, 96000)
        for (length in 0 until valid.size) {
            assertNull(readFlacCodecInfo(ByteArrayInputStream(valid.copyOf(length))))
        }
        assertNull(readFlacCodecInfo(ByteArrayInputStream("not FLAC".toByteArray())))
        assertNull(readFlacCodecInfo(ByteArrayInputStream(stream(24, 0))))
        valid[7] = 33
        assertNull(readFlacCodecInfo(ByteArrayInputStream(valid)))
    }
}
