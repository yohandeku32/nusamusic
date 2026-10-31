package com.yohandeku32.nusamusic.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object AudioWaveformLoader {
    private var cache: AudioWaveformCache? = null
    private fun cache(context: Context): AudioWaveformCache = synchronized(this) {
        cache ?: AudioWaveformCache(File(context.applicationContext.cacheDir, "audio_waveforms_v1")).also { cache = it }
    }

    suspend fun load(context: Context, song: Song): AudioWaveform? = withContext(Dispatchers.IO) {
        val uri = song.uri.toUri()
        if (uri.scheme !in setOf("file", "content")) return@withContext null
        try {
            val source = source(context, song, uri)
            cache(context).load(source) {
                withTimeoutOrNull(45_000L) { decode(context, song, uri) }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }

    private fun source(context: Context, song: Song, uri: Uri): WaveformSource {
        if (uri.scheme == "file") {
            val file = File(requireNotNull(uri.path))
            return WaveformSource(song.uri, file.length(), file.lastModified(), song.durationMs)
        }
        var size = -1L
        var modified = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
                    val documentTime = cursor.getColumnIndex("last_modified")
                    val mediaTime = cursor.getColumnIndex("date_modified")
                    if (documentTime >= 0 && !cursor.isNull(documentTime)) modified = cursor.getLong(documentTime)
                    else if (mediaTime >= 0 && !cursor.isNull(mediaTime)) modified = cursor.getLong(mediaTime) * 1000L
                }
            }
        }
        return WaveformSource(song.uri, size, modified, song.durationMs)
    }

    private fun pcm(format: MediaFormat): WaveformPcm = when (
        if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        else AudioFormat.ENCODING_PCM_16BIT
    ) {
        AudioFormat.ENCODING_PCM_8BIT -> WaveformPcm.UNSIGNED_8
        AudioFormat.ENCODING_PCM_16BIT -> WaveformPcm.SIGNED_16
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> WaveformPcm.PACKED_24
        AudioFormat.ENCODING_PCM_32BIT -> WaveformPcm.SIGNED_32
        AudioFormat.ENCODING_PCM_FLOAT -> WaveformPcm.FLOAT_32
        else -> error("Unsupported waveform PCM encoding")
    }

    private suspend fun decode(context: Context, song: Song, uri: Uri): AudioWaveform? {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION)
                else song.durationMs * 1000L
            if (durationUs <= 0 || durationUs > 24L * 60 * 60 * 1_000_000) return null
            extractor.selectTrack(track)
            val peaks = PcmWaveformAccumulator(durationUs)
            if (mime == "audio/raw") {
                val bytes = ByteBuffer.allocate(2 * 1024 * 1024).order(ByteOrder.nativeOrder())
                while (true) {
                    currentCoroutineContext().ensureActive()
                    bytes.clear()
                    val count = extractor.readSampleData(bytes, 0)
                    if (count < 0) break
                    bytes.position(0); bytes.limit(count)
                    peaks.add(bytes, extractor.sampleTime, format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT), pcm(format))
                    if (!extractor.advance()) break
                }
                return peaks.finish()
            }
            val codec = MediaCodec.createDecoderByType(mime)
            decoder = codec
            codec.configure(format, null, null, 0)
            codec.start()
            var outputFormat = format
            var inputEnded = false
            var outputEnded = false
            val info = MediaCodec.BufferInfo()
            while (!outputEnded) {
                currentCoroutineContext().ensureActive()
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = requireNotNull(codec.getInputBuffer(inputIndex))
                        input.clear()
                        val count = extractor.readSampleData(input, 0)
                        if (count < 0) {
                            inputEnded = true
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, count, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val index = codec.dequeueOutputBuffer(info, 10_000)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) outputFormat = codec.outputFormat
                else if (index >= 0) {
                    try {
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val output = requireNotNull(codec.getOutputBuffer(index))
                            output.position(info.offset); output.limit(info.offset + info.size)
                            peaks.add(output.slice().order(ByteOrder.nativeOrder()), info.presentationTimeUs,
                                outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT), pcm(outputFormat))
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    } finally { codec.releaseOutputBuffer(index, false) }
                }
            }
            return peaks.finish()
        } finally {
            decoder?.let { codec -> runCatching { codec.stop() }; runCatching { codec.release() } }
            extractor.release()
        }
    }
}
