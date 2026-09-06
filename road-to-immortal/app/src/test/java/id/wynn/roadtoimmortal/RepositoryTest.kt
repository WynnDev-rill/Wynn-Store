package id.wynn.roadtoimmortal

import id.wynn.roadtoimmortal.data.*
import java.io.File
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
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
@Config(sdk = [28])
class RepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun setup() {
        File(context.filesDir, "catalog").deleteRecursively()
        server = MockWebServer().apply { start() }
        client =
            OkHttpClient.Builder()
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
}
