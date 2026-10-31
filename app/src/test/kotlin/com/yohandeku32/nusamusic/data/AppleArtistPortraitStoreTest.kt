package com.yohandeku32.nusamusic.data

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Collections
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppleArtistPortraitStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private val context by lazy {
        object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun getApplicationContext(): Context = this
            override fun getCacheDir(): File = temp.root
        }
    }
    private var now = System.currentTimeMillis()
    private val page = "https://music.apple.com/us/artist/first-singer/123"
    private val image = "https://is1-ssl.mzstatic.com/image/artist/600x600bb.jpg"
    private val catalog = """{"results":[{"wrapperType":"artist","artistName":"First Singer","artistId":123,
        "artistLinkUrl":"$page"}]}""".toByteArray()
    private val html = """<script type="application/ld+json">{"@type":"MusicGroup","name":"First Singer",
        "image":"https://is1-ssl.mzstatic.com/image/artist/486x486bb.png"}</script>""".toByteArray()
    private val requests = Collections.synchronizedList(mutableListOf<String>())
    private val pixels by lazy {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            .also { bitmap.recycle() }
    }
    private fun reply(url: String): PortraitHttpResponse {
        requests.add(url)
        return when {
            url.startsWith("https://itunes.apple.com/search?") -> PortraitHttpResponse(200, catalog)
            url == page -> PortraitHttpResponse(200, html)
            url == image -> PortraitHttpResponse(200, pixels, "image/png")
            else -> throw AssertionError("Unexpected artist request: $url")
        }
    }
    private fun store(request: suspend (String) -> PortraitHttpResponse = { reply(it) }) =
        AppleArtistPortraitStore(request, { now })

    @Test fun repeatedSongsAndGuestArtistsReuseOnePhotoWithoutMoreRequests() = runBlocking {
        val store = store()
        val portrait = store.load(context, "First Singer feat. Guest")
        assertNotNull(portrait)
        for (name in listOf("First Singer", "FIRST SINGER", "First Singer & Other", "First Singer, Guest")) {
            assertSame(portrait, store.load(context, name))
            assertSame(portrait, store.cached(name))
        }
        assertEquals(3, requests.size)
        assertEquals(listOf(buildAppleMusicPortraitUrl("First Singer"), page, image), requests.toList())
    }

    @Test fun processRestartReadsDiskWithoutNetwork() = runBlocking {
        assertNotNull(store().load(context, "First Singer"))
        val restored = store { throw AssertionError("Disk hit must not use network") }
        assertNotNull(restored.load(context, "First Singer feat. Guest"))
        assertEquals(3, requests.size)
    }

    @Test fun simultaneousForegroundAndWarmupShareTheSameRequests() = runBlocking {
        val store = store { delay(20); reply(it) }
        val portraits = awaitAll(async { store.load(context, "First Singer") },
            async { store.load(context, "First Singer & Guest") })
        assertNotNull(portraits.first())
        assertSame(portraits.first(), portraits.last())
        assertEquals(3, requests.size)
    }

    @Test fun transientErrorCanRecoverAfterThirtySecondsInsteadOfTwelveHours() = runBlocking {
        var offline = true
        val store = store { if (offline) { requests.add(it); throw IOException("offline") } else reply(it) }
        assertNull(store.load(context, "First Singer"))
        offline = false
        assertNull(store.load(context, "First Singer"))
        assertEquals(1, requests.size)
        now += 31_000
        assertNotNull(store.load(context, "First Singer"))
        assertEquals(4, requests.size)
    }

    @Test fun failedImageRetryUsesTheSavedCdnUrlWithoutRepeatingArtistSearch() = runBlocking {
        var unavailable = true
        val store = store {
            if (it == image && unavailable) { requests.add(it); PortraitHttpResponse(503, byteArrayOf()) }
            else reply(it)
        }
        assertNull(store.load(context, "First Singer"))
        unavailable = false
        now += 31_000
        assertNotNull(store.load(context, "First Singer"))
        assertEquals(4, requests.size)
        assertEquals(image, requests.last())
    }

    @Test fun aRealCatalogMissIsCachedAcrossRestarts() = runBlocking {
        val store = store { requests.add(it); PortraitHttpResponse(200, """{"results":[]}""".toByteArray()) }
        assertNull(store.load(context, "First Singer"))
        assertNull(store().load(context, "First Singer"))
        assertEquals(1, requests.size)
    }

    @Test fun expiredPhotoRemainsAvailableWhenTheNetworkFails() = runBlocking {
        assertNotNull(store().load(context, "First Singer"))
        now += 31L * 24 * 60 * 60 * 1000
        val offline = store { throw IOException("offline") }
        assertNotNull(offline.load(context, "First Singer"))
        assertNotNull(offline.cached("First Singer"))
    }

    @Test fun cancellationDoesNotCreateANegativeCacheEntry() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var cancelRequest = true
        val store = store {
            if (cancelRequest) { started.complete(Unit); awaitCancellation() }
            else reply(it)
        }
        val job = launch { store.load(context, "First Singer") }
        started.await()
        job.cancelAndJoin()
        cancelRequest = false
        assertNotNull(store.load(context, "First Singer"))
        assertEquals(3, requests.size)
    }

    @Test fun oldProxyFailureCacheCannotBlockTheDirectAppleSource() = runBlocking {
        val oldKey = java.security.MessageDigest.getInstance("SHA-256")
            .digest("first singer".toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        File(context.cacheDir, "apple_artist_portraits").apply { mkdirs() }
            .resolve("$oldKey.miss").writeText("")
        assertNotNull(store().load(context, "First Singer"))
        assertEquals(3, requests.size)
    }

    @Test fun repeatedMissingArtistsCannotGrowFailureFilesWithoutBound() = runBlocking {
        val directory = File(context.cacheDir, "apple_artist_portraits_v2").apply { mkdirs() }
        repeat(150) { File(directory, "$it.miss").writeText("0") }
        val missing = store { PortraitHttpResponse(200, """{"results":[]}""".toByteArray()) }
        assertNull(missing.load(context, "First Singer"))
        assertTrue(directory.listFiles().orEmpty().size <= 128)
    }
}
