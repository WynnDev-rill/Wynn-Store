package id.wynn.roadtoimmortal.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.*

data class DataState(
    val catalog: Catalog? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val lastCheck: Instant? = null,
)

class CatalogRepository(private val context: Context, transport: OkHttpClient? = null) {
    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
        const val CONFIG_URL =
            "https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/road-to-immortal/data/remote-config.json"

        fun validate(c: Catalog): Catalog {
            require(c.schemaVersion == 1)
            require(c.heroes.size >= 100 && c.heroes.map { it.id }.toSet().size == c.heroes.size)
            require(
                c.heroes.all { it.id > 0 && it.name.isNotBlank() && it.icon.startsWith("https://") }
            )
            require(c.items.size >= 60 && c.spells.size >= 10)
            require(
                c.meta.all {
                    it.days > 0 &&
                        it.heroes.all { m ->
                            listOf(m.win, m.pick, m.ban).all { v ->
                                v == null || v.isFinite() && v in 0.0..100.0
                            }
                        }
                }
            )
            Instant.parse(c.generatedAt)
            return c
        }
    }

    private val mutex = Mutex()
    private val dir = File(context.filesDir, "catalog").apply { mkdirs() }
    private val snapshot = AtomicFile(File(dir, "catalog.json"))
    private val configFile = AtomicFile(File(dir, "config.json"))
    private val mutable = MutableStateFlow(DataState())
    val state: StateFlow<DataState> = mutable
    private var config = RemoteConfig()
    private val client =
        transport
            ?: OkHttpClient.Builder()
                .cache(Cache(File(context.cacheDir, "http"), 16L * 1024 * 1024))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()

    suspend fun initialize() =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (mutable.value.catalog != null) return@withLock
                config =
                    runCatching {
                            json.decodeFromString<RemoteConfig>(
                                configFile.readFully().decodeToString()
                            )
                        }
                        .getOrDefault(RemoteConfig())
                val saved =
                    runCatching {
                            validate(
                                json.decodeFromString<Catalog>(
                                    snapshot.readFully().decodeToString()
                                )
                            )
                        }
                        .getOrNull()
                val bundled =
                    runCatching {
                            context.assets.open("catalog.json").bufferedReader().use {
                                validate(json.decodeFromString<Catalog>(it.readText()))
                            }
                        }
                        .getOrNull()
                val best =
                    listOfNotNull(saved, bundled).maxByOrNull { Instant.parse(it.generatedAt) }
                mutable.value =
                    DataState(
                        best,
                        loading = false,
                        error =
                            if (best == null) "Hubungkan internet untuk mengambil katalog MLBB."
                            else null,
                    )
            }
        }

    suspend fun refresh(force: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val before = mutable.value
                if (
                    !force &&
                        before.lastCheck?.let {
                            Instant.now().epochSecond - it.epochSecond <
                                config.refreshHours.coerceIn(1, 24) * 3600
                        } == true
                )
                    return@withLock true
                mutable.value = before.copy(refreshing = true)
                try {
                    try {
                        val raw = download(CONFIG_URL, 64_000, force)
                        val next = json.decodeFromString<RemoteConfig>(raw)
                        require(
                            next.schemaVersion == 1 &&
                                next.catalogUrls.isNotEmpty() &&
                                next.catalogUrls.all(::trusted)
                        )
                        config = next
                        writeAtomic(configFile, raw.toByteArray())
                    } catch (c: CancellationException) {
                        throw c
                    } catch (_: Exception) {
                        /* A config failure cannot disable catalog fallback. */
                    }
                    if (config.disabled) {
                        mutable.value =
                            before.copy(
                                refreshing = false,
                                error = "Pembaruan sedang dijeda. Data tersimpan tetap tersedia.",
                            )
                        return@withLock false
                    }
                    for (url in
                        (config.catalogUrls + RemoteConfig().catalogUrls)
                            .distinct()
                            .filter(::trusted)) {
                        try {
                            val raw = download(url, 16_000_000, force)
                            val next = validate(json.decodeFromString<Catalog>(raw))
                            val current = before.catalog
                            require(
                                current == null || next.heroes.size >= current.heroes.size * .95
                            )
                            if (
                                current == null ||
                                    Instant.parse(next.generatedAt) >=
                                        Instant.parse(current.generatedAt)
                            ) {
                                writeAtomic(snapshot, raw.toByteArray())
                                mutable.value =
                                    DataState(next, loading = false, lastCheck = Instant.now())
                            } else
                                mutable.value =
                                    before.copy(
                                        refreshing = false,
                                        error = null,
                                        lastCheck = Instant.now(),
                                    )
                            return@withLock true
                        } catch (c: CancellationException) {
                            throw c
                        } catch (_: Exception) {
                            /* Keep last good data and try next mirror. */
                        }
                    }
                    mutable.value =
                        before.copy(
                            loading = false,
                            refreshing = false,
                            error =
                                "Belum bisa memperbarui. Periksa koneksi; data tersimpan tetap bisa dibuka.",
                        )
                    false
                } catch (c: CancellationException) {
                    mutable.value = before.copy(refreshing = false)
                    throw c
                }
            }
        }

    private fun trusted(url: String) =
        runCatching {
                val u = java.net.URI(url)
                u.scheme == "https" &&
                    u.userInfo == null &&
                    when (u.host) {
                        "raw.githubusercontent.com" ->
                            u.path.startsWith("/WynnDev-rill/Wynn-Store/")
                        "cdn.jsdelivr.net" -> u.path.startsWith("/gh/WynnDev-rill/Wynn-Store@")
                        else -> false
                    }
            }
            .getOrDefault(false)

    private fun download(url: String, limit: Int, force: Boolean): String =
        client
            .newCall(
                Request.Builder()
                    .url(url)
                    .header("User-Agent", "RoadToImmortal/1.0 Android")
                    .header("Cache-Control", if (force) "no-cache" else "max-age=300")
                    .build()
            )
            .execute()
            .use { response ->
                check(response.isSuccessful)
                val body = checkNotNull(response.body)
                require(body.contentLength() <= limit)
                body.byteStream().use { stream ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var size = 0
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= limit)
                        out.write(buffer, 0, count)
                    }
                    out.toByteArray().decodeToString()
                }
            }

    private fun writeAtomic(file: AtomicFile, bytes: ByteArray) {
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }
}
