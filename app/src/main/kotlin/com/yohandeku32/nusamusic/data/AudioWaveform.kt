package com.yohandeku32.nusamusic.data

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

data class AudioWaveform(val durationMs: Long, val peaks: FloatArray)

internal enum class WaveformPcm(val bytes: Int) {
    UNSIGNED_8(1), SIGNED_16(2), PACKED_24(3), SIGNED_32(4), FLOAT_32(4)
}

/** Fixed-size temporal peaks; channels are combined by magnitude, never averaged out. */
internal class PcmWaveformAccumulator(private val durationUs: Long, count: Int = 256) {
    init { require(durationUs > 0 && count in 2..512) }
    private val peaks = FloatArray(count)
    private var frames = 0L

    fun add(pcm: ByteBuffer, presentationUs: Long, sampleRate: Int, channels: Int, encoding: WaveformPcm) {
        require(sampleRate in 1..768_000 && channels in 1..32)
        val frameBytes = channels * encoding.bytes
        require(pcm.remaining() % frameBytes == 0) { "Incomplete PCM frame" }
        var frame = 0L
        while (pcm.remaining() >= frameBytes) {
            var peak = 0f
            repeat(channels) {
                val sample = when (encoding) {
                    WaveformPcm.UNSIGNED_8 -> ((pcm.get().toInt() and 255) - 128) / 128f
                    WaveformPcm.SIGNED_16 -> pcm.short / 32768f
                    WaveformPcm.SIGNED_32 -> (pcm.int.toDouble() / 2147483648.0).toFloat()
                    WaveformPcm.FLOAT_32 -> pcm.float
                    WaveformPcm.PACKED_24 -> {
                        val a = pcm.get().toInt() and 255
                        val b = pcm.get().toInt() and 255
                        val c = pcm.get().toInt() and 255
                        val raw = if (pcm.order() == java.nio.ByteOrder.LITTLE_ENDIAN)
                            a or (b shl 8) or (c shl 16) else c or (b shl 8) or (a shl 16)
                        ((raw shl 8) shr 8) / 8388608f
                    }
                }
                if (sample.isFinite()) peak = max(peak, abs(sample).coerceAtMost(1f))
            }
            val timeUs = presentationUs + frame * 1_000_000L / sampleRate
            if (timeUs >= 0 && timeUs <= durationUs) {
                val index = (timeUs.toDouble() / durationUs * peaks.size).toInt().coerceIn(peaks.indices)
                peaks[index] = max(peaks[index], peak)
            }
            frame++
        }
        frames += frame
    }

    fun finish(): AudioWaveform? = if (frames == 0L) null
        else AudioWaveform((durationUs / 1000).coerceAtLeast(1), peaks.copyOf())
}
