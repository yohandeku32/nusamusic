package com.yohandeku32.nusamusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal fun portraitPrefetchArtists(queueArtistNames: List<String>): List<String> =
    queueArtistNames.asSequence().map(ArtistNameUtils::firstArtist)
        .filter { buildAppleMusicPortraitUrl(it) != null }
        .distinctBy(ArtistNameUtils::cacheKey).take(7).toList()

internal object AppleArtistPortraitLoader {
    private val store = AppleArtistPortraitStore()
    private val warmupMonitor = Any()
    private var automaticFetchDay = -1L
    private val automaticFetchKeys = HashSet<String>()

    fun cached(name: String): Bitmap? = store.cached(name)
    fun artistUrl(name: String): String? = buildAppleMusicPortraitUrl(name)
    suspend fun load(context: Context, artistName: String): Bitmap? = store.load(context, artistName)

    suspend fun warm(context: Context, artistName: String): Bitmap? = withContext(Dispatchers.IO) {
        if (artistUrl(artistName) == null) return@withContext null
        cached(artistName)?.let { return@withContext it }
        if (store.hasFreshDiskEntry(context, artistName)) return@withContext load(context, artistName)
        val key = ArtistNameUtils.cacheKey(artistName)
        val now = System.currentTimeMillis()
        val mayFetch = synchronized(warmupMonitor) {
            val day = now / (24L * 60 * 60 * 1000)
            val prefs = context.applicationContext.getSharedPreferences("apple_artist_prefetch_budget_v2", Context.MODE_PRIVATE)
            if (day != automaticFetchDay) { automaticFetchDay = day; automaticFetchKeys.clear() }
            if (key in automaticFetchKeys) true else {
                val count = if (prefs.getLong("day", -1L) == day) prefs.getInt("count", 0) else 0
                if (count >= 36) false else {
                    automaticFetchKeys.add(key)
                    prefs.edit().putLong("day", day).putInt("count", count + 1).apply()
                    true
                }
            }
        }
        if (mayFetch) load(context, artistName) else null
    }
}

internal class AppleArtistPortraitStore(
    private val request: suspend (String) -> PortraitHttpResponse = ::requestPortraitResource,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val images = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.byteCount / 1024)
    }
    private val locks = LruCache<String, Mutex>(48)
    private fun lock(key: String) = synchronized(locks) { locks.get(key) ?: Mutex().also { locks.put(key, it) } }
    private fun directory(context: Context) = File(context.applicationContext.cacheDir, "apple_artist_portraits_v2")
    private fun fileKey(name: String): String = MessageDigest.getInstance("SHA-256")
        .digest(ArtistNameUtils.cacheKey(name).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun fresh(file: File) = file.isFile && clock() - file.lastModified() in 0 until PORTRAIT_TTL

    fun cached(name: String): Bitmap? = images.get(ArtistNameUtils.cacheKey(name))

    fun hasFreshDiskEntry(context: Context, name: String): Boolean {
        val dir = directory(context)
        val key = fileKey(name)
        return fresh(File(dir, "$key.img")) || failureUntil(File(dir, "$key.miss")) > clock()
    }

    suspend fun load(context: Context, name: String): Bitmap? = withContext(Dispatchers.IO) {
        if (buildAppleMusicPortraitUrl(name) == null) return@withContext null
        val key = ArtistNameUtils.cacheKey(name)
        cached(name)?.let { return@withContext it }
        lock(key).withLock {
            cached(name)?.let { return@withLock it }
            val dir = directory(context)
            val fileKey = fileKey(name)
            val imageFile = File(dir, "$fileKey.img")
            val urlFile = File(dir, "$fileKey.url")
            val missFile = File(dir, "$fileKey.miss")
            val disk = runCatching { if (imageFile.isFile) decode(imageFile.readBytes()) else null }.getOrNull()
            if (disk != null && fresh(imageFile)) {
                images.put(key, disk)
                return@withLock disk
            }
            if (failureUntil(missFile) > clock()) return@withLock disk
            try {
                val savedUrl = if (fresh(urlFile)) runCatching { urlFile.readText() }.getOrNull()
                    ?.let(::applePortraitImage) else null
                val imageUrl = savedUrl ?: resolveAppleArtistPortrait(name, request)
                if (imageUrl == null) {
                    writeFailure(missFile, clock() + NOT_FOUND_TTL)
                    return@withLock disk
                }
                currentCoroutineContext().ensureActive()
                if (savedUrl == null) runCatching { dir.mkdirs(); urlFile.writeText(imageUrl); urlFile.setLastModified(clock()) }
                val response = request(imageUrl)
                if (response.status == 404) urlFile.delete()
                if (response.status != 200 || !response.contentType.startsWith("image/", true)) {
                    throw IOException("Artist image unavailable: ${response.status}")
                }
                val bitmap = decode(response.bytes) ?: throw IOException("Invalid artist image")
                currentCoroutineContext().ensureActive()
                images.put(key, bitmap)
                runCatching {
                    dir.mkdirs()
                    val temp = File(dir, "$fileKey.tmp")
                    temp.writeBytes(response.bytes)
                    if (!temp.renameTo(imageFile)) { imageFile.writeBytes(response.bytes); temp.delete() }
                    imageFile.setLastModified(clock())
                    missFile.delete()
                    dir.listFiles { file -> file.extension == "img" }?.sortedByDescending { it.lastModified() }
                        ?.drop(48)?.forEach { file ->
                            file.delete()
                            File(dir, "${file.nameWithoutExtension}.url").delete()
                        }
                    trimMetadata(dir)
                }
                bitmap
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                writeFailure(missFile, clock() + RETRY_TTL)
                // An expired but valid portrait is preferable to flashing an album cover offline.
                if (disk != null) images.put(key, disk)
                disk
            }
        }
    }

    private fun failureUntil(file: File): Long = runCatching { file.readText().toLong() }.getOrDefault(0L)
    private fun writeFailure(file: File, until: Long) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(until.toString())
            file.parentFile?.let(::trimMetadata)
        }
    }
    private fun trimMetadata(directory: File) {
        directory.listFiles { file -> file.extension in setOf("miss", "url") }
            ?.sortedByDescending { it.lastModified() }?.drop(128)?.forEach(File::delete)
    }
    private fun decode(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty() || bytes.size > MAX_PORTRAIT_BYTES) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth < 120 || bounds.outHeight < 120) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 600 && sample < 32) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
            inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565
        })
    }
}

private const val PORTRAIT_TTL = 30L * 24 * 60 * 60 * 1000
private const val NOT_FOUND_TTL = 6L * 60 * 60 * 1000
private const val RETRY_TTL = 30_000L
private const val MAX_PORTRAIT_BYTES = 2 * 1024 * 1024

private suspend fun requestPortraitResource(urlText: String): PortraitHttpResponse {
    var next = URL(urlText)
    repeat(3) {
        if (!isApplePortraitResource(next.toString())) throw IOException("Invalid artist resource")
        currentCoroutineContext().ensureActive()
        val connection = (next.openConnection() as HttpURLConnection).apply {
            connectTimeout = 2_000; readTimeout = 3_000; instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Aruna/ArtistPortrait")
            setRequestProperty("Accept", "application/json,text/html,image/jpeg,image/png")
        }
        try {
            val status = connection.responseCode
            if (status in 300..399) {
                next = URL(next, connection.getHeaderField("Location") ?: throw IOException("Missing artist redirect"))
                return@repeat
            }
            if (status != 200) return PortraitHttpResponse(status, byteArrayOf())
            val limit = if (next.host.endsWith(".mzstatic.com")) MAX_PORTRAIT_BYTES else 1024 * 1024
            if (connection.contentLengthLong > limit) throw IOException("Artist response too large")
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(chunk)
                    if (count < 0) break
                    if (output.size() + count > limit) throw IOException("Artist response too large")
                    output.write(chunk, 0, count)
                }
                output.toByteArray()
            }
            return PortraitHttpResponse(status, bytes, connection.contentType.orEmpty())
        } finally { connection.disconnect() }
    }
    throw IOException("Too many artist redirects")
}
