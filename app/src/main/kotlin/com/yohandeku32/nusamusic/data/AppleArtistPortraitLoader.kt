package com.yohandeku32.nusamusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/**
 * Optional Apple Music artist portrait via the community-maintained unavatar
 * proxy (https://github.com/microlinkhq/unavatar). This is NOT Apple's
 * authenticated MusicKit API. It only resolves public Apple Music portraits.
 *
 * Keep the local embedded-cover hero until an image is cached/resolved:
 * networking, decoding, disk writes, and failures never delay page navigation.
 * An explicit fallback=false prevents unavatar's default placeholder picture
 * from being misidentified as an official artist photo.
 */
/** Pure JVM-safe URL builder: no Android image cache initialization. */
internal fun buildAppleMusicPortraitUrl(name: String): String? {
    val clean = ArtistNameUtils.firstArtist(name).trim()
    if (clean.length !in 2..100 ||
        clean.equals("unknown artist", true) ||
        clean.equals("artis tidak dikenal", true)
    ) return null
    val encoded = URLEncoder.encode("artist:$clean", StandardCharsets.UTF_8.name())
        .replace("+", "%20")
    return "https://unavatar.io/apple-music/$encoded?fallback=false"
}

/**
 * Match queue order, not alphabetical Library order. Multiple songs by the
 * same artist need one portrait, and invalid artist tags do not use bandwidth.
 */
internal fun portraitPrefetchArtists(queueArtistNames: List<String>): List<String> =
    queueArtistNames.asSequence()
        .map(ArtistNameUtils::firstArtist)
        .filter { buildAppleMusicPortraitUrl(it) != null }
        .distinctBy(ArtistNameUtils::cacheKey)
        .take(7)
        .toList()

internal object AppleArtistPortraitLoader {
    private const val POSITIVE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    private const val NEGATIVE_TTL_MS = 12L * 60 * 60 * 1000
    private const val MAX_BYTES = 4 * 1024 * 1024
    private const val MAX_EDGE = 1080

    // Seven square 1080px portraits can exceed 12 MiB; keep the current
    // and next-six queue window in RAM so Artist opens without disk decode.
    private val images = object : LruCache<String, Bitmap>(20 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val misses = LruCache<String, Long>(128)
    // Single-flight by artist: a background warmup and an Artist tap reuse
    // the same request, without unrelated artists waiting on each other.
    private val requestLocks = LruCache<String, Mutex>(48)
    private fun artistLock(key: String): Mutex = synchronized(requestLocks) {
        requestLocks.get(key) ?: Mutex().also { requestLocks.put(key, it) }
    }
    // Automatic prefetch is intentionally bounded so six queue lookaheads do
    // not exhaust the community image provider's shared daily allowance.
    // Cached images never consume the quota; foreground taps still load on demand.
    private const val MAX_DAILY_AUTOMATIC_FETCHES = 36
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private val warmupMonitor = Any()
    private var automaticFetchDay = -1L
    private val automaticFetchKeys = HashSet<String>()

    /** Load a cached portrait now; fetch an uncached one only if quota permits. */
    suspend fun warm(context: Context, artistName: String): Bitmap? =
        withContext(Dispatchers.IO) {
            if (artistUrl(artistName) == null) return@withContext null
            cached(artistName)?.let { return@withContext it }
            val key = ArtistNameUtils.cacheKey(artistName)
            val dir = File(context.applicationContext.cacheDir, "apple_artist_portraits")
            val hashed = portraitFileKey(key)
            val now = System.currentTimeMillis()
            val imageFile = File(dir, "$hashed.img")
            val missFile = File(dir, "$hashed.miss")
            // Disk hits and known misses skip reservation and network work.
            if ((imageFile.isFile && now - imageFile.lastModified() < POSITIVE_TTL_MS) ||
                (missFile.isFile && now - missFile.lastModified() < NEGATIVE_TTL_MS)) {
                return@withContext load(context, artistName)
            }
            val mayFetch = synchronized(warmupMonitor) {
                val day = now / DAY_MS
                val prefs = context.applicationContext.getSharedPreferences(
                    "apple_artist_prefetch_budget", Context.MODE_PRIVATE
                )
                if (day != automaticFetchDay) {
                    automaticFetchDay = day
                    automaticFetchKeys.clear()
                }
                if (key in automaticFetchKeys) {
                    true
                } else {
                    val savedDay = prefs.getLong("day", -1L)
                    val count = if (savedDay == day) prefs.getInt("count", 0) else 0
                    if (count >= MAX_DAILY_AUTOMATIC_FETCHES) false
                    else {
                        automaticFetchKeys.add(key)
                        prefs.edit().putLong("day", day)
                            .putInt("count", count + 1).apply()
                        true
                    }
                }
            }
            if (!mayFetch) return@withContext null
            load(context, artistName)
        }

    private fun portraitFileKey(key: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    fun artistUrl(name: String): String? = buildAppleMusicPortraitUrl(name)

    fun cached(name: String): Bitmap? = images.get(ArtistNameUtils.cacheKey(name))

    suspend fun load(context: Context, artistName: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val url = artistUrl(artistName) ?: return@withContext null
            val key = ArtistNameUtils.cacheKey(artistName)
            images.get(key)?.let { return@withContext it }
            artistLock(key).withLock {
                images.get(key)?.let { return@withLock it }

            val directory = File(context.cacheDir, "apple_artist_portraits")
            val fileKey = portraitFileKey(key)
            val cachedFile = File(directory, "$fileKey.img")
            val missFile = File(directory, "$fileKey.miss")
            val now = System.currentTimeMillis()
            if (cachedFile.isFile && now - cachedFile.lastModified() < POSITIVE_TTL_MS) {
                runCatching { decode(cachedFile.readBytes()) }.getOrNull()?.let {
                    images.put(key, it)
                    return@withLock it
                }
            }
            if (misses.get(key)?.let { now - it < NEGATIVE_TTL_MS } == true ||
                (missFile.isFile && now - missFile.lastModified() < NEGATIVE_TTL_MS)
            ) return@withLock null

            // Rate-limited third-party service; cache both successes and misses.
            val bitmapBytes = runCatching { requestImage(url) }.getOrNull()
            val bitmap = bitmapBytes?.let(::decode)
            if (bitmap == null) {
                misses.put(key, now)
                runCatching { directory.mkdirs(); missFile.writeText("") }
                return@withLock null
            }
            images.put(key, bitmap)
            runCatching {
                directory.mkdirs()
                val temp = File(directory, "$fileKey.tmp")
                temp.writeBytes(bitmapBytes)
                if (!temp.renameTo(cachedFile)) {
                    cachedFile.writeBytes(bitmapBytes)
                    temp.delete()
                }
                missFile.delete()
                // Bounded disk usage, never grow unbounded as artists change.
                directory.listFiles { it -> it.extension == "img" }
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(48)?.forEach(File::delete)
            }
            bitmap
            }
        }

    private fun requestImage(urlText: String): ByteArray? {
        var next = URL(urlText)
        repeat(4) {
            if (next.protocol != "https" ||
                !(next.host == "unavatar.io" ||
                    next.host.endsWith(".unavatar.io") ||
                    next.host.endsWith(".mzstatic.com"))
            ) return null
            val connection = (next.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2600
                readTimeout = 3700
                instanceFollowRedirects = false
                setRequestProperty("Accept", "image/webp,image/jpeg,image/png")
                setRequestProperty("User-Agent", "NusaMusic/ArtistPortrait")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: return null
                    next = URL(next, location)
                    return@repeat
                }
                if (code != 200 ||
                    !connection.contentType.orEmpty().lowercase(Locale.ROOT).startsWith("image/")
                ) return null
                if (connection.contentLengthLong > MAX_BYTES) return null
                return connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val chunk = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(chunk)
                        if (count < 0) break
                        total += count
                        if (total > MAX_BYTES) return null
                        output.write(chunk, 0, count)
                    }
                    output.toByteArray()
                }
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth < 120 || bounds.outHeight < 120) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE &&
            sample < 32
        ) sample *= 2
        return BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        )
    }
}
