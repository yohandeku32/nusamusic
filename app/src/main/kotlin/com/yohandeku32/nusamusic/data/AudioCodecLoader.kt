package com.yohandeku32.nusamusic.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import com.yohandeku32.nusamusic.model.Song
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object AudioCodecLoader {

    // Metadata is expensive: opening a MediaExtractor for every row that
    // enters the viewport can saturate storage while the user scrolls.
    // Keep a small LRU (including missing/unreadable results) and allow only
    // two native metadata reads at once across the entire app.
    private val metadataCache = object : LinkedHashMap<String, AudioCodecInfo?>(2_048, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, AudioCodecInfo?>?
        ): Boolean = size > 2_048
    }
    private val metadataPermit = Semaphore(2)
    // The LazyColumn recycles track rows when scrolling. Do not tie native
    // audio probing to each row's composition lifecycle: that restarted file
    // reads for ordinary tracks every time they re-entered the viewport.
    // One background request per URI survives row disposal and is remembered
    // even when the bounded detail cache eventually evicts an old result.
    private val inspectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingBadgeInspections = HashSet<String>()
    private val inspectedBadgeUris = object : LinkedHashMap<String, Boolean>(16_384, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, Boolean>?
        ): Boolean = size > 16_384
    }

    fun requestHiResBadgeOnce(context: Context, uriString: String) {
        if (uriString.isBlank()) return
        if (isHiResCached(uriString)) return
        synchronized(metadataCache) {
            if (metadataCache.containsKey(uriString)) return
        }
        synchronized(pendingBadgeInspections) {
            if (uriString in pendingBadgeInspections ||
                inspectedBadgeUris.containsKey(uriString)
            ) return
            pendingBadgeInspections.add(uriString)
        }
        val appContext = context.applicationContext
        inspectScope.launch {
            try {
                // A short one-time grace period keeps native extractors away
                // from the first frames after a fast flick stops.
                delay(260L)
                load(appContext, uriString)
            } finally {
                synchronized(pendingBadgeInspections) {
                    pendingBadgeInspections.remove(uriString)
                    inspectedBadgeUris[uriString] = true
                }
            }
        }
    }
    // Compose observes the verified badge of each song separately. This makes
    // already-visible rows update as soon as metadata becomes available,
    // without recomposing the entire library or decoding its logo per row.
    private val verifiedHiRes = mutableStateMapOf<String, Boolean>()

    fun isHiResCached(uriString: String): Boolean = verifiedHiRes[uriString] == true

    private suspend fun publishHiRes(uriString: String, info: AudioCodecInfo?) {
        if (info?.isHiRes == true) withContext(Dispatchers.Main.immediate) {
            verifiedHiRes[uriString] = true
        }
    }
    private const val HI_RES_PREFS = "hi_res_index_v1"
    private const val HI_RES_TTL_MS = 14L * 24 * 60 * 60 * 1_000

    private fun persistKey(uri: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }

    // Only persist confirmed bit depths; an audio file extension alone
    // cannot prove that a track qualifies for the Hi-Res badge.
    private fun decodeStored(record: String?, now: Long): AudioCodecInfo? {
        val parts = record?.split('|') ?: return null
        if (parts.size != 4 || (parts[0].toLongOrNull() ?: 0L) <= now) return null
        val bits = parts[3].toIntOrNull() ?: return null
        if (bits < 24) return null
        return AudioCodecInfo(
            codecName = parts[1],
            sampleRateHz = parts[2].toIntOrNull()?.takeIf { it > 0 },
            bitDepth = bits
        )
    }

    private fun savedHiRes(context: Context, uri: String): AudioCodecInfo? =
        decodeStored(
            context.applicationContext.getSharedPreferences(
                HI_RES_PREFS, Context.MODE_PRIVATE
            ).getString(persistKey(uri), null),
            System.currentTimeMillis()
        )

    private fun saveHiRes(context: Context, uri: String, info: AudioCodecInfo) {
        if (!info.isHiRes) return
        val record = "${System.currentTimeMillis() + HI_RES_TTL_MS}|${info.codecName}|${info.sampleRateHz ?: -1}|${info.bitDepth}"
        context.applicationContext.getSharedPreferences(HI_RES_PREFS, Context.MODE_PRIVATE)
            .edit().putString(persistKey(uri), record).apply()
    }

    /**
     * Read the persisted index once off-main and publish the entire verified
     * set in one snapshot on main. No file extraction or bitmap work during
     * IME animations or fast LazyColumn flings.
     */
    suspend fun warmVerifiedBadges(context: Context, songs: List<Song>) {
        val restored = withContext(Dispatchers.IO) {
            val records = context.applicationContext.getSharedPreferences(
                HI_RES_PREFS, Context.MODE_PRIVATE
            ).all
            val now = System.currentTimeMillis()
            buildMap<String, AudioCodecInfo> {
                for (song in songs) {
                    currentCoroutineContext().ensureActive()
                    val info = decodeStored(records[persistKey(song.uri)] as? String, now)
                        ?: continue
                    synchronized(metadataCache) {
                        if (!metadataCache.containsKey(song.uri)) metadataCache[song.uri] = info
                    }
                    put(song.uri, info)
                }
            }
        }
        withContext(Dispatchers.Main.immediate) {
            // One Compose snapshot notification rather than thousands of
            // separate row invalidations during library startup.
            verifiedHiRes.putAll(restored.mapValues { true })
        }
    }

    /** Instant, non-blocking lookup for badges in recycled list rows. */
    fun cached(uriString: String): AudioCodecInfo? =
        synchronized(metadataCache) { metadataCache[uriString] }

    suspend fun load(context: Context, uriString: String): AudioCodecInfo? =
        withContext(Dispatchers.IO) {
            synchronized(metadataCache) {
                if (metadataCache.containsKey(uriString)) {
                    return@withContext metadataCache[uriString]
                }
            }
            savedHiRes(context, uriString)?.let { saved ->
                synchronized(metadataCache) { metadataCache[uriString] = saved }
                publishHiRes(uriString, saved)
                return@withContext saved
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
                if (info?.isHiRes == true) {
                    saveHiRes(context, uriString, info)
                    publishHiRes(uriString, info)
                }
                info
            }
        }

    private fun inspect(context: Context, uriString: String): AudioCodecInfo? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
            ?: return null

        // STREAMINFO is authoritative for native FLAC, even when MediaExtractor
        // fails or exposes the decoder's PCM output rather than source bit depth.
        val flac = runCatching {
            context.contentResolver.openInputStream(uri)?.use(::readFlacCodecInfo)
        }.getOrNull()
        if (flac != null) return flac

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
