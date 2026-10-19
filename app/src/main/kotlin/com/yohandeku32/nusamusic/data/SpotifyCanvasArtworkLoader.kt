package com.yohandeku32.nusamusic.data

import android.content.Context
import android.net.Uri
import com.yohandeku32.nusamusic.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * Second-choice animated artwork for personal/local listening.
 *
 * Spotycovs is an independent, unofficial public resolver. It searches a
 * Spotify track by artist/title and returns Canvas data, without any Spotify
 * cookie, access token, or user credentials embedded in NusaMusic.
 *
 * Apple Music remains the first choice; this loader must ONLY be called after
 * Apple artwork is unavailable or its video stream cannot play.
 *
 * Third-party availability and the undocumented Spotify API are best effort.
 * Calls run on Dispatchers.IO, are bounded, and use a small LRU/TTL cache.
 */
internal object SpotifyCanvasArtworkLoader {
    private const val API_ORIGIN = "https://www.spotycovs.lol"
    private const val MAX_ITEMS = 160
    private const val MAX_RESPONSE_CHARS = 128_000
    private const val POSITIVE_TTL = 6 * 60 * 60 * 1000L
    private const val NEGATIVE_TTL = 5 * 60 * 1000L
    private const val FAILURE_TTL = 12_000L
    // Unlike in-memory URL lookups, persisted positive Canvas matches survive
    // closing/reopening NusaMusic, avoiding repeat calls to the slow resolver.
    private const val DISK_POSITIVE_TTL = 3L * 24 * 60 * 60 * 1_000
    private const val MAX_DISK_ENTRIES = 150
    private const val DISK_PREFS = "spotify_canvas_urls_v1"
    private val cache = LinkedHashMap<String, CacheEntry>(32, 0.75f, true)
    // Share a network lookup between the player and predictive prefetch for
    // the same track. Each distinct track can still resolve independently.
    private val inFlight = HashMap<String, CompletableDeferred<String?>>()
    // Resolution outlives a speculative prefetch: when the same song starts
    // playing, the visible lookup must never inherit a cancelled request.
    private val resolverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resolverPermits = Semaphore(3)

    private data class CacheEntry(val url: String?, val expires: Long)
    private data class LookupResult(val videoUrl: String?, val temporaryFailure: Boolean)

    suspend fun findCanvasUrl(context: Context, song: Song): String? = withContext(Dispatchers.IO) {
        val title = song.title.trim().take(160)
        val artist = song.artist.trim().take(160)
        if (title.isBlank() || artist.isBlank() ||
            artist.equals("Unknown artist", true) || artist.equals("<unknown>", true)
        ) return@withContext null

        val key = "${artist.lowercase(Locale.ROOT)}|${title.lowercase(Locale.ROOT)}"
        val now = System.currentTimeMillis()
        synchronized(cache) {
            cache[key]?.takeIf { it.expires > now }?.let { return@withContext it.url }
            cache.remove(key)
        }

        // Local lookup avoids an HTTP request on every track revisit or app
        // launch. Validate again in case cache data becomes corrupted.
        val saved = readSavedUrl(context, key, now)
        if (saved != null) {
            synchronized(cache) {
                cache[key] = CacheEntry(saved, now + POSITIVE_TTL)
                while (cache.size > MAX_ITEMS) cache.remove(cache.entries.first().key)
            }
            return@withContext saved
        }

        currentCoroutineContext().ensureActive()
        // Coalesce all lookups for this track in a bounded background scope.
        // Cancelling an offscreen prefetch only cancels its await, not the
        // resolver needed by a player that became visible a moment later.
        val request: CompletableDeferred<String?>
        val leader: Boolean
        synchronized(inFlight) {
            val existing = inFlight[key]
            if (existing != null) {
                request = existing
                leader = false
            } else {
                request = CompletableDeferred()
                inFlight[key] = request
                leader = true
            }
        }
        if (leader) {
            val app = context.applicationContext
            resolverScope.launch {
                try {
                    val result = resolverPermits.withPermit {
                        val query = Uri.encode("$artist - $title")
                        fetchAndValidate("$API_ORIGIN/api/canvas?track=$query&results=1", title, artist)
                    }
                    val ttl = when {
                        result.videoUrl != null -> POSITIVE_TTL
                        result.temporaryFailure -> FAILURE_TTL
                        else -> NEGATIVE_TTL
                    }
                    synchronized(cache) {
                        cache[key] = CacheEntry(result.videoUrl, System.currentTimeMillis() + ttl)
                        while (cache.size > MAX_ITEMS) cache.remove(cache.entries.first().key)
                    }
                    if (result.videoUrl != null) saveUrl(app, key, result.videoUrl)
                    request.complete(result.videoUrl)
                } catch (_: Exception) {
                    // Never strand a foreground waiter if an unexpected
                    // resolver or cache error interrupts the lookup.
                    request.complete(null)
                } finally {
                    synchronized(inFlight) {
                        if (inFlight[key] === request) inFlight.remove(key)
                    }
                }
            }
        }
        request.await()
    }

    private fun diskKey(key: String): String = MessageDigest.getInstance("SHA-256")
        .digest(key.toByteArray(Charsets.UTF_8))
        .take(16).joinToString("") { "%02x".format(it) }

    private fun readSavedUrl(context: Context, key: String, now: Long): String? {
        val pref = context.applicationContext.getSharedPreferences(DISK_PREFS, Context.MODE_PRIVATE)
        val record = pref.getString(diskKey(key), null) ?: return null
        val expiresAt = record.substringBefore('|').toLongOrNull() ?: 0L
        val url = record.substringAfter('|', "")
        if (expiresAt <= now || !isSpotifyCanvasVideo(url)) {
            pref.edit().remove(diskKey(key)).apply()
            return null
        }
        return url
    }

    private fun saveUrl(context: Context, key: String, url: String) {
        val pref = context.applicationContext.getSharedPreferences(DISK_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val editor = pref.edit()
            .putString(diskKey(key), "${now + DISK_POSITIVE_TTL}|$url")
        // Bound the on-disk metadata just like the in-memory LRU. This work
        // happens in Dispatchers.IO and never touches Compose's frame loop.
        val records = pref.all.mapNotNull { (id, value) ->
            val expiry = (value as? String)?.substringBefore('|')?.toLongOrNull()
            if (expiry == null) null else id to expiry
        }.sortedBy { it.second }
        if (records.size >= MAX_DISK_ENTRIES) {
            val toRemove = records.size - MAX_DISK_ENTRIES + 1
            records.take(toRemove).forEach { editor.remove(it.first) }
        }
        editor.apply()
    }

    /** A failed playback must not pin a stale/unplayable stream for three days. */
    fun invalidate(context: Context, failedUrl: String) {
        synchronized(cache) {
            cache.entries.removeAll { it.value.url == failedUrl }
        }
        val pref = context.applicationContext.getSharedPreferences(DISK_PREFS, Context.MODE_PRIVATE)
        val editor = pref.edit()
        pref.all.forEach { (id, value) ->
            if ((value as? String)?.substringAfter('|', "") == failedUrl) editor.remove(id)
        }
        editor.apply()
    }

    private suspend fun fetchAndValidate(endpoint: String, title: String, artist: String): LookupResult {
        var connection: HttpsURLConnection? = null
        return try {
            currentCoroutineContext().ensureActive()
            connection = URL(endpoint).openConnection() as HttpsURLConnection
            connection.connectTimeout = 3_500
            connection.readTimeout = 5_500
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "NusaMusic/SpotifyCanvasFallback")
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return LookupResult(null, false)
            if (status != HttpURLConnection.HTTP_OK) return LookupResult(null, true)
            val input = connection.inputStream.bufferedReader(Charsets.UTF_8)
            val body = input.use { reader ->
                val out = StringBuilder()
                val buffer = CharArray(4_096)
                while (out.length <= MAX_RESPONSE_CHARS) {
                    currentCoroutineContext().ensureActive()
                    val count = reader.read(buffer)
                    if (count < 0) break
                    out.append(buffer, 0, count)
                }
                if (out.length > MAX_RESPONSE_CHARS) return LookupResult(null, true)
                out.toString()
            }
            val json = JSONObject(body)
            if (json.has("success") && !json.optBoolean("success")) return LookupResult(null, true)
            if (json.has("hasCanvas") && !json.optBoolean("hasCanvas")) return LookupResult(null, false)
            val track = json.optJSONObject("track") ?: json.optJSONObject("data")?.optJSONObject("track")
            if (track != null && !matchingSong(track, title, artist)) {
                return LookupResult(null, false)
            }
            currentCoroutineContext().ensureActive()
            LookupResult(canvasVideoUrl(json), false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            LookupResult(null, true)
        } finally {
            connection?.disconnect()
        }
    }

    private fun matchingSong(track: JSONObject, title: String, artist: String): Boolean {
        val foundTitle = track.optString("name").ifBlank { track.optString("title") }
        if (foundTitle.isNotBlank() && !similarTitle(foundTitle, title)) return false
        val artistValue = track.opt("artists")
        val foundArtists = when (artistValue) {
            is JSONArray -> (0 until artistValue.length()).mapNotNull {
                artistValue.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank)
                    ?: artistValue.optString(it).takeIf(String::isNotBlank)
            }
            else -> emptyList()
        }
        val directArtist = track.optString("artist").ifBlank { track.optString("artistName") }
        val candidates = if (foundArtists.isNotEmpty()) foundArtists else listOf(directArtist).filter(String::isNotBlank)
        return candidates.isEmpty() || candidates.any { similarArtist(it, artist) }
    }

    private fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s*(?:\\(.*?\\)|\\[.*?])"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    internal fun similarTitle(expected: String, actual: String): Boolean {
        val left = normalized(expected)
        val right = normalized(actual)
        return left.isNotBlank() && right.isNotBlank() &&
            (left == right || left.startsWith("$right ") || right.startsWith("$left "))
    }

    internal fun similarArtist(expected: String, actual: String): Boolean {
        val left = normalized(expected)
        val right = normalized(actual)
        return left.isNotBlank() && right.isNotBlank() &&
            (left == right || right.startsWith("$left ") || left.startsWith("$right "))
    }

    private fun canvasVideoUrl(root: JSONObject): String? {
        // Spotycovs returns a proxied /api/canvas?id=...&stream=1 MP4,
        // not a canvaz.scdn.co CDN link. Accept ONLY that exact trusted
        // streaming route or a genuine Spotify CDN MP4; unrelated URLs
        // from third-party JSON must never reach the Android video player.
        fun direct(obj: JSONObject): String? {
            for (name in listOf("canvasUrl", "videoUrl", "video_url", "mediaUrl", "url")) {
                val value = obj.optString(name)
                if (isSpotifyCanvasVideo(value)) return value
            }
            return null
        }
        fun fromArray(arr: JSONArray?): String? {
            if (arr == null) return null
            for (index in 0 until minOf(arr.length(), 12)) {
                val obj = arr.optJSONObject(index) ?: continue
                direct(obj)?.let { return it }
                obj.optJSONObject("media")?.let(::direct)?.let { return it }
            }
            return null
        }
        direct(root)?.let { return it }
        for (container in listOf(root, root.optJSONObject("data")).filterNotNull()) {
            for (key in listOf("canvases", "canvasesList", "canvas", "videos")) {
                fromArray(container.optJSONArray(key))?.let { return it }
                container.optJSONObject(key)?.let(::direct)?.let { return it }
            }
            direct(container)?.let { return it }
        }
        return null
    }

    internal fun isSpotifyCanvasVideo(value: String): Boolean = runCatching {
        val url = URL(value)
        if (!url.protocol.equals("https", true) || url.userInfo != null ||
            (url.port != -1 && url.port != 443) || url.ref != null
        ) return@runCatching false

        when (url.host.lowercase(Locale.ROOT)) {
            "canvaz.scdn.co" -> url.path.endsWith(".mp4", true)
            "www.spotycovs.lol", "spotycovs.lol" -> {
                // Service response example:
                // https://www.spotycovs.lol/api/canvas?id=<22-char>&stream=1
                // Never allow arbitrary proxy paths, URLs, or open-redirect
                // parameters from a JSON response.
                if (url.path != "/api/canvas") return@runCatching false
                val parameters = url.query?.split('&')
                    ?.map { it.split('=', limit = 2) } ?: return@runCatching false
                if (parameters.size != 2 || parameters.any { it.size != 2 }) {
                    return@runCatching false
                }
                val pairs = parameters.associate { it[0] to it[1] }
                pairs.size == 2 && pairs["stream"] == "1" &&
                    pairs["id"]?.matches(Regex("[A-Za-z0-9]{22}")) == true
            }
            else -> false
        }
    }.getOrDefault(false)
}
