package com.yohandeku32.nusamusic.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

data class AudioCodecInfo(
    val codecName: String,
    val sampleRateHz: Int?,
    val bitDepth: Int?
) {
    val isHiRes: Boolean
        get() = bitDepth != null && bitDepth >= 24
}

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

                // MediaFormat does not expose a KEY_BITS_PER_SAMPLE field in
                // the Android stubs used by this project. PCM can still be inferred
                // from KEY_PCM_ENCODING; ALAC/FLAC bit depth is read below using
                // MediaMetadataRetriever on API 31+.
                val bitDepth = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    when (runCatching {
                        format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }.getOrNull()) {
                        android.media.AudioFormat.ENCODING_PCM_8BIT -> 8
                        android.media.AudioFormat.ENCODING_PCM_16BIT -> 16
                        android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                        android.media.AudioFormat.ENCODING_PCM_32BIT -> 32
                        else -> null
                    }
                } else {
                    null
                }

                val codec = when {
                    // E-AC-3 JOC is the stream identifier used for Dolby Atmos.
                    // Do not label generic AC-3/E-AC-3 as Atmos unless the
                    // extractor exposes the Atmos/JOC marker.
                    mime.contains("atmos") ||
                        (mime.contains("eac3") && mime.contains("joc")) -> "Dolby Atmos"
                    mime.contains("flac") -> "FLAC"
                    mime.contains("alac") -> "Apple Lossless"
                    mime.contains("mp4a") || mime.contains("aac") -> "AAC"
                    mime.contains("mpeg") -> "MP3"
                    mime.contains("opus") -> "Opus"
                    mime.contains("vorbis") -> "Vorbis"
                    mime.contains("pcm") || mime.contains("raw") -> "PCM"
                    mime.contains("eac3") -> "Dolby Digital+"
                    mime.contains("ac3") -> "Dolby Digital"
                    mime.contains("dts") -> "DTS"
                    mime.contains("amr") -> "AMR"
                    else -> mime.removePrefix("audio/").uppercase()
                }

                val retrieverBitDepth =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && bitDepth == null) {
                        runCatching {
                            android.media.MediaMetadataRetriever().use { retriever ->
                                retriever.setDataSource(context, uri)
                                retriever
                                    .extractMetadata(
                                        android.media.MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE
                                    )
                                    ?.toIntOrNull()
                            }
                        }.getOrNull()
                    } else {
                        null
                    }

                val retrieverSampleRate =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && sampleRate == null) {
                        runCatching {
                            android.media.MediaMetadataRetriever().use { retriever ->
                                retriever.setDataSource(context, uri)
                                retriever
                                    .extractMetadata(
                                        android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
                                    )
                                    ?.toIntOrNull()
                            }
                        }.getOrNull()
                    } else {
                        null
                    }

                return@withContext AudioCodecInfo(
                    codecName = codec,
                    sampleRateHz = sampleRate ?: retrieverSampleRate,
                    bitDepth = bitDepth ?: retrieverBitDepth
                )
            }

            null
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}
