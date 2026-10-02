package com.yohandeku32.nusamusic.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

data class AudioCodecInfo(
    val codecName: String,
    val sampleRateHz: Int?
)

object AudioCodecLoader {

    suspend fun load(
        context: Context,
        uriString: String
    ): AudioCodecInfo? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
            ?: return@withContext null

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)

            for (trackIndex in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME)?.lowercase()
                    ?: continue

                if (!mime.startsWith("audio/")) continue

                val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else {
                    null
                }

                val codec = when {
                    mime.contains("flac") -> "FLAC"
                    mime.contains("alac") -> "Apple Lossless"
                    mime.contains("mp4a") || mime.contains("aac") -> "AAC"
                    mime.contains("opus") -> "Opus"
                    mime.contains("vorbis") -> "Vorbis"
                    mime.contains("pcm") || mime.contains("raw") -> "PCM"
                    mime.contains("ac3") -> "Dolby Digital"
                    mime.contains("eac3") -> "Dolby Digital+"
                    mime.contains("dts") -> "DTS"
                    mime.contains("amr") -> "AMR"
                    else -> mime.removePrefix("audio/").uppercase()
                }

                return@withContext AudioCodecInfo(codec, sampleRate)
            }

            null
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}
