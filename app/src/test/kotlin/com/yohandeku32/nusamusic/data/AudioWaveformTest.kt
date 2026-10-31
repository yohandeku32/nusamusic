package com.yohandeku32.nusamusic.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class AudioWaveformTest {
    @Test fun temporalPeaksFollowLoudnessAndRetainSilence() {
        val accumulator = PcmWaveformAccumulator(4_000_000, 4)
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        listOf(0, 8192, -32768, 16384).forEach { bytes.putShort(it.toShort()) }
        bytes.flip()
        accumulator.add(bytes, 0, 1, 1, WaveformPcm.SIGNED_16)
        assertArrayEquals(floatArrayOf(0f, 0.25f, 1f, 0.5f), accumulator.finish()!!.peaks, 0f)
    }

    @Test fun stereoAntiPhaseCannotCancelTheWaveform() {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        bytes.putShort(16384); bytes.putShort(-16384); bytes.flip()
        val accumulator = PcmWaveformAccumulator(1_000_000, 2)
        accumulator.add(bytes, 0, 48_000, 2, WaveformPcm.SIGNED_16)
        assertEquals(0.5f, accumulator.finish()!!.peaks.first(), 0f)
    }

    @Test fun outputTimestampsPlaceSeparateBuffersAtTheCorrectSongPosition() {
        val accumulator = PcmWaveformAccumulator(4_000_000, 4)
        for ((time, value) in listOf(0L to 0.25f, 3_000_000L to 0.75f)) {
            val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value)
            buffer.flip()
            accumulator.add(buffer, time, 1, 1, WaveformPcm.FLOAT_32)
        }
        assertArrayEquals(floatArrayOf(0.25f, 0f, 0f, 0.75f), accumulator.finish()!!.peaks, 0f)
    }

    @Test fun eightBitPcmHasAnUnsignedSilenceMidpoint() {
        val accumulator = PcmWaveformAccumulator(3_000_000, 3)
        accumulator.add(ByteBuffer.wrap(byteArrayOf(128.toByte(), 0, 255.toByte())), 0, 1, 1, WaveformPcm.UNSIGNED_8)
        assertArrayEquals(floatArrayOf(0f, 1f, 127 / 128f), accumulator.finish()!!.peaks, 0f)
    }

    @Test fun packed24BitUsesSignedMagnitudeInBothByteOrders() {
        for (order in listOf(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN)) {
            val data = if (order == ByteOrder.LITTLE_ENDIAN) byteArrayOf(0, 0, 0x80.toByte(), 0, 0, 0x40)
                else byteArrayOf(0x80.toByte(), 0, 0, 0x40, 0, 0)
            val accumulator = PcmWaveformAccumulator(2_000_000, 2)
            accumulator.add(ByteBuffer.wrap(data).order(order), 0, 1, 1, WaveformPcm.PACKED_24)
            assertArrayEquals(floatArrayOf(1f, 0.5f), accumulator.finish()!!.peaks, 0f)
        }
    }

    @Test fun signed32BitDoesNotOverflowAtItsNegativeLimit() {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(Int.MIN_VALUE).putInt(1 shl 30)
        buffer.flip()
        val accumulator = PcmWaveformAccumulator(2_000_000, 2)
        accumulator.add(buffer, 0, 1, 1, WaveformPcm.SIGNED_32)
        assertArrayEquals(floatArrayOf(1f, 0.5f), accumulator.finish()!!.peaks, 0f)
    }

    @Test fun floatingPointBoundsAreFiniteAndClamped() {
        val buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -2f, 0.3f).forEach(buffer::putFloat)
        buffer.flip()
        val accumulator = PcmWaveformAccumulator(4_000_000, 4)
        accumulator.add(buffer, 0, 1, 1, WaveformPcm.FLOAT_32)
        assertArrayEquals(floatArrayOf(0f, 0f, 1f, 0.3f), accumulator.finish()!!.peaks, 0f)
    }

    @Test fun emptyOutputCannotBePublishedAsACompletedWaveform() {
        assertNull(PcmWaveformAccumulator(1_000_000).finish())
    }

    @Test(expected = IllegalArgumentException::class)
    fun truncatedPcmFramesAreRejected() {
        PcmWaveformAccumulator(1_000_000).add(ByteBuffer.allocate(3), 0, 44_100, 2, WaveformPcm.SIGNED_16)
    }

    @Test fun silenceRemainsAFlatSignal() {
        val accumulator = PcmWaveformAccumulator(1_000_000)
        accumulator.add(ByteBuffer.allocate(88_200), 0, 44_100, 1, WaveformPcm.SIGNED_16)
        assertTrue(accumulator.finish()!!.peaks.all { it == 0f })
    }
}
