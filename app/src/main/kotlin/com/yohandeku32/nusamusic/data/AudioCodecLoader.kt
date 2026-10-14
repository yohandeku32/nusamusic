package com.yohandeku32.nusamusic.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object AudioCodecLoader {

    // Metadata is expensive: opening a MediaExtractor for every row that
    // enters the viewport can saturate storage while the user scrolls.
    // Keep a small LRU (including missing/unreadable results) and allow only
    // two native metadata reads at once across the entire app.
    private val metadataCache = object : LinkedHashMap<String, AudioCodecInfo?>(192, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, AudioCodecInfo?>?
        ): Boolean = size > 192
    }
    private val metadataPermit = Semaphore(2)

    suspend fun load(context: Context, uriString: String): AudioCodecInfo? =
        withContext(Dispatchers.IO) {
            synchronized(metadataCache) {
                if (metadataCache.containsKey(uriString)) {
                    return@withContext metadataCache[uriString]
                }
            }
            metadataPermit.withPermit {
                // A neighbouring row may have finished the same lookup
                // while this coroutine waited for a metadata permit.
                synchronized(metadataCache) {
                    if (metadataCache.containsKey(uriString)) {
                        return@withPermit metadataCache[uriString]
                    }
                }
                val info = inspect(context, uriString)
                synchronized(metadataCache) { metadataCache[uriString] = info }
                info
            }
        }

    private fun inspect(context: Context, uriString: String): AudioCodecInfo? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
            ?: return null

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

                val codec = audioCodecName(mime)

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

                return AudioCodecInfo(
                    codecName = codec,
                    sampleRateHz = sampleRate ?: retrieverSampleRate,
                    bitDepth = bitDepth ?: retrieverBitDepth
                )
            }

            return null
        } catch (_: Exception) {
            return null
        } finally {
            extractor.release()
        }
    }
}
