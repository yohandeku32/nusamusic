package com.yohandeku32.nusamusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCodecInfoTest {
    @Test
    fun hiResRequiresAtLeast24BitsForBothLosslessFormats() {
        for (codec in listOf("FLAC", "Apple Lossless")) {
            for (depth in listOf(null, 16, 20, 23)) {
                assertFalse(AudioCodecInfo(codec, 192_000, depth).isHiRes)
            }
            for (depth in listOf(24, 32)) {
                assertTrue(AudioCodecInfo(codec, 44_100, depth).isHiRes)
            }
        }
    }
    @Test
    fun dolbyStreamsGetTheirOwnFormatLabelAndBadge() {
        val formats = mapOf(
            "audio/ac3" to "Dolby Digital",
            "audio/eac3" to "Dolby Digital+",
            "audio/eac3-joc" to "Dolby Atmos",
            "audio/true-hd" to "Dolby TrueHD",
            "audio/vnd.dolby.mlp" to "Dolby TrueHD",
            "AUDIO/AC4" to "Dolby AC-4"
        )
        for ((mime, expected) in formats) {
            val info = AudioCodecInfo(audioCodecName(mime), 48_000, null)
            assertEquals(expected, info.codecName)
            assertTrue(info.isDolby)
        }
    }

    @Test
    fun ordinarySurroundStreamsAreNotLabelledAtmos() {
        for (mime in listOf("audio/ac3", "audio/eac3", "audio/true-hd", "audio/ac4")) {
            assertFalse(audioCodecName(mime) == "Dolby Atmos")
        }
        assertEquals("Dolby Atmos", audioCodecName("audio/eac3-joc"))
    }

    @Test
    fun nonDolbyCodecsKeepTheirLabelsAndHaveNoDolbyBadge() {
        for ((mime, expected) in mapOf("audio/flac" to "FLAC", "audio/alac" to "Apple Lossless", "audio/mp4a-latm" to "AAC", "audio/vnd.dts" to "DTS")) {
            val info = AudioCodecInfo(audioCodecName(mime), 48_000, 24)
            assertEquals(expected, info.codecName)
            assertFalse(info.isDolby)
        }
    }
}
