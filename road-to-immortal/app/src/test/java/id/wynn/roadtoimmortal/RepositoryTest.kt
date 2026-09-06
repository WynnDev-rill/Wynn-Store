package id.wynn.roadtoimmortal

import id.wynn.roadtoimmortal.data.*
import java.io.File
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class RepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun setup() {
        File(context.filesDir, "catalog").deleteRecursively()
        val httpCache =
            File(context.cacheDir, "test-http").apply {
                deleteRecursively()
                mkdirs()
            }
        server = MockWebServer().apply { start() }
        client =
            OkHttpClient.Builder()
                .cache(Cache(httpCache, 10L * 1024 * 1024))
                .addInterceptor { chain ->
                    chain.proceed(
                        chain
                            .request()
                            .newBuilder()
                            .url(server.url(chain.request().url.encodedPath))
                            .build()
                    )
                }
                .build()
    }

    @After
    fun close() {
        client.cache?.close()
        server.shutdown()
    }

    private fun response(body: String, status: Int = 200) =
        MockResponse().setResponseCode(status).setBody(body)

    @Test
    fun corruptAndUnavailableMirrorsKeepUsableData() = runBlocking {
        val repo = CatalogRepository(context, client)
        repo.initialize()
        val initial = checkNotNull(repo.state.value.catalog)
        server.enqueue(response("broken config"))
        server.enqueue(response("{truncated catalog"))
        server.enqueue(response("unavailable", 503))
        assertFalse(repo.refresh(true))
        assertEquals(initial, repo.state.value.catalog)
        assertFalse(repo.state.value.refreshing)
        assertNotNull(repo.state.value.error)
    }

    @Test
    fun healthyMirrorUpdatesAndSurvivesRepositoryRestart() = runBlocking {
        val repo = CatalogRepository(context, client)
        repo.initialize()
        val initial = checkNotNull(repo.state.value.catalog)
        val next =
            initial.copy(
                generatedAt = Instant.parse(initial.generatedAt).plusSeconds(60).toString()
            )
        server.enqueue(response("{}"))
        server.enqueue(response("unavailable", 503))
        server.enqueue(response(CatalogRepository.json.encodeToString(next)))
        assertTrue(repo.refresh(true))
        assertEquals(next.generatedAt, repo.state.value.catalog?.generatedAt)
        val reopened = CatalogRepository(context, client)
        reopened.initialize()
        assertEquals(next.generatedAt, reopened.state.value.catalog?.generatedAt)
    }

    @Test
    fun olderRemoteSnapshotCannotReplaceNewerLocalData() = runBlocking {
        val repo = CatalogRepository(context, client)
        repo.initialize()
        val initial = checkNotNull(repo.state.value.catalog)
        val old =
            initial.copy(
                generatedAt = Instant.parse(initial.generatedAt).minusSeconds(60).toString()
            )
        server.enqueue(response("{}"))
        server.enqueue(response(CatalogRepository.json.encodeToString(old)))
        assertTrue(repo.refresh(true))
        assertEquals(initial.generatedAt, repo.state.value.catalog?.generatedAt)
    }

    @Test
    fun manualRefreshRevalidatesAnOtherwiseFreshHttpCache() = runBlocking {
        val repo = CatalogRepository(context, client)
        repo.initialize()
        val initial = checkNotNull(repo.state.value.catalog)
        server.enqueue(response("{}").setHeader("Cache-Control", "public, max-age=3600"))
        server.enqueue(
            response(CatalogRepository.json.encodeToString(initial))
                .setHeader("Cache-Control", "public, max-age=3600")
        )
        assertTrue(repo.refresh())
        repeat(3) { server.enqueue(response("offline", 503)) }
        assertFalse(repo.refresh(true))
        assertEquals(5, server.requestCount)
        assertEquals(initial, repo.state.value.catalog)
        assertNotNull(repo.state.value.error)
    }
}
