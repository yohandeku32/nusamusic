package com.yohandeku32.nusamusic.data

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@androidx.annotation.OptIn(UnstableApi::class)
class SpotifyCanvasVideoCacheTest {
    @get:Rule val temp = TemporaryFolder()
    private val first = "https://www.spotycovs.lol/api/canvas?id=4iZ4pt7kvcaH6Yo8UoZ4s2&stream=1"
    private val equivalent = "https://spotycovs.lol/api/canvas?stream=1&id=4iZ4pt7kvcaH6Yo8UoZ4s2"

    @Test fun equivalentProxyUrlsReuseDownloadedBytesInTheRealMedia3Cache() {
        val measurements = JSONArray()
        repeat(3) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val db = StandaloneDatabaseProvider(context)
            val cache = SimpleCache(temp.newFolder(), NoOpCacheEvictor(), db)
            val bytes = ByteArray(512 * 1024) { index -> (index % 251).toByte() }
            var opens = 0
            var transferred = 0L
            val upstream = DataSource.Factory {
                val delegate = ByteArrayDataSource(bytes)
                object : DataSource by delegate {
                    override fun open(spec: DataSpec): Long { opens++; return delegate.open(spec) }
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                        delegate.read(buffer, offset, length).also { if (it > 0) transferred += it }
                }
            }
            try {
                val factory = spotifyCanvasDataSourceFactory(cache, upstream)
                for (url in listOf(first, equivalent)) {
                    val reader = factory.createDataSource()
                    try {
                        reader.open(DataSpec.Builder().setUri(url).build())
                        val buffer = ByteArray(16 * 1024)
                        var total = 0
                        while (true) {
                            val count = reader.read(buffer, 0, buffer.size)
                            if (count == -1) break
                            for (index in 0 until count) assertEquals(bytes[total + index], buffer[index])
                            total += count
                        }
                        assertEquals(bytes.size, total)
                    } finally { reader.close() }
                }
                measurements.put(JSONObject().put("opens", opens).put("upstreamBytes", transferred))
            } finally { cache.release(); db.close() }
        }
        val file = File("build/reports/canvas-cache-metrics.json").apply { parentFile!!.mkdirs() }
        file.writeText(measurements.toString(2))
        for (index in 0 until measurements.length()) {
            assertEquals("An alias must reuse the completed clip instead of downloading it again", 1,
                measurements.getJSONObject(index).getInt("opens"))
            assertEquals(512L * 1024, measurements.getJSONObject(index).getLong("upstreamBytes"))
        }
    }

    @Test fun foregroundDemandReleasesThePrefetchReaderAfterTheCurrentChunk() = runBlocking {
        var busy = false
        var transferred = 0
        var closed = false
        val delegate = ByteArrayDataSource(ByteArray(512 * 1024))
        val reader = object : DataSource by delegate {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                delegate.read(buffer, offset, length).also { if (it > 0) transferred += it; busy = true }
            override fun close() { closed = true; delegate.close() }
        }
        fillSpotifyCanvasCache(reader, first, 512 * 1024L) { busy }
        assertEquals(32 * 1024, transferred)
        assertTrue("Do not keep a cache writer locked while waiting for foreground buffering", closed)
    }

    @Test fun failedOpenAndCancelledPrefetchAlwaysCloseTheReader() = runBlocking {
        var closed = false
        val broken = object : DataSource by ByteArrayDataSource(ByteArray(1)) {
            override fun open(spec: DataSpec): Long = throw IOException("Synthetic failed open")
            override fun close() { closed = true }
        }
        try { fillSpotifyCanvasCache(broken, first, 10L) { false }; fail("Open should fail") }
        catch (_: IOException) { assertTrue(closed) }
        closed = false
        lateinit var job: Job
        val delegate = ByteArrayDataSource(ByteArray(128 * 1024))
        val reader = object : DataSource by delegate {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                delegate.read(buffer, offset, length).also { job.cancel() }
            override fun close() { closed = true; delegate.close() }
        }
        job = launch(start = CoroutineStart.LAZY) { fillSpotifyCanvasCache(reader, first, 128 * 1024L) { false } }
        job.start(); job.join()
        assertTrue(job.isCancelled)
        assertTrue(closed)
    }

    @Test fun prefetchHonorsTheExactByteBudgetAndSkipsWhileForegroundIsBusy() = runBlocking {
        var transferred = 0
        val delegate = ByteArrayDataSource(ByteArray(128 * 1024))
        val reader = object : DataSource by delegate {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                delegate.read(buffer, offset, length).also { if (it > 0) transferred += it }
        }
        fillSpotifyCanvasCache(reader, first, 40_000L) { false }
        assertEquals(40_000, transferred)
        val mustNotOpen = object : DataSource by ByteArrayDataSource(ByteArray(1)) {
            override fun open(spec: DataSpec): Long { fail("Busy foreground must prevent an upstream request"); return 0L }
        }
        fillSpotifyCanvasCache(mustNotOpen, first, 100L) { true }
    }

    @Test fun differentTrackIdsSignedCdnUrlsAndUnknownHostsNeverShareKeys() {
        assertEquals(first, spotifyCanvasCacheKey(equivalent))
        assertNotEquals(spotifyCanvasCacheKey(first), spotifyCanvasCacheKey(first.replace("4iZ4", "5iZ4")))
        val direct = "https://canvaz.scdn.co/upload/a.mp4?token=one"
        assertEquals(direct, spotifyCanvasCacheKey(direct))
        assertNotEquals(spotifyCanvasCacheKey(direct), spotifyCanvasCacheKey(direct.replace("one", "two")))
        val unrelated = "https://example.com/api/canvas?id=4iZ4pt7kvcaH6Yo8UoZ4s2&stream=1"
        assertEquals(unrelated, spotifyCanvasCacheKey(unrelated))
    }

    @Test fun anInterruptedPrefixResumesThroughAnAliasWithoutDownloadingItsPrefixAgain() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = StandaloneDatabaseProvider(context)
        val cache = SimpleCache(temp.newFolder(), NoOpCacheEvictor(), db)
        val bytes = ByteArray(512 * 1024) { (it % 251).toByte() }
        val starts = mutableListOf<Long>()
        var transferred = 0L
        val upstream = DataSource.Factory {
            val delegate = ByteArrayDataSource(bytes)
            object : DataSource by delegate {
                override fun open(spec: DataSpec): Long { starts += spec.position; return delegate.open(spec) }
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    delegate.read(buffer, offset, length).also { if (it > 0) transferred += it }
            }
        }
        try {
            val factory = spotifyCanvasDataSourceFactory(cache, upstream)
            fillSpotifyCanvasCache(factory.createDataSource(), first, 128 * 1024L) { false }
            assertEquals(128 * 1024L, transferred)
            fillSpotifyCanvasCache(factory.createDataSource(), equivalent, bytes.size.toLong()) { false }
            assertEquals(listOf(0L, 128 * 1024L), starts)
            assertEquals(bytes.size.toLong(), transferred)
            fillSpotifyCanvasCache(factory.createDataSource(), first, bytes.size.toLong()) { false }
            assertEquals("A completed loop must replay without another upstream read", bytes.size.toLong(), transferred)
        } finally { cache.release(); db.close() }
    }
}
