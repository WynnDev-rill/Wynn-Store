package id.wynn.screenguard.data

import android.content.Context
import android.util.AtomicFile
import id.wynn.screenguard.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** One process, serialized atomic writes. No cloud backup or exported file provider. */
class GuardStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "guard-state.json"))
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    val loadError: String?
    private val mutable: MutableStateFlow<GuardState>
    val state get() = mutable.asStateFlow()
    init {
        val result = runCatching { if (file.baseFile.exists()) file.openRead().bufferedReader().use { reader ->
            val text = reader.readText()
            val decoded = json.decodeFromString<GuardState>(text)
            GuardMigration.migrate(if ("\"schemaVersion\"" in text) decoded else decoded.copy(schemaVersion = 1))
        } else GuardState() }
        loadError = result.exceptionOrNull()?.let { "Data lokal tidak terbaca. Jangan hapus data aplikasi; pulihkan aplikasi melalui panduan pemulihan." }
        mutable = MutableStateFlow(result.getOrDefault(GuardState()))
    }
    suspend fun update(transform: (GuardState) -> GuardState) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(loadError == null) { loadError!! }
            val next = transform(mutable.value)
            val out = file.startWrite()
            try { out.write(json.encodeToString(next).toByteArray()); file.finishWrite(out) }
            catch (e: Exception) { file.failWrite(out); throw e }
            mutable.value = next
        }
    }
    suspend fun rule(pkg: String, change: (AppRule) -> AppRule) = update { state ->
        state.copy(rules = state.rules + (pkg to change(state.rules[pkg] ?: AppRule(pkg))))
    }
    suspend fun settings(change: (GuardSettings) -> GuardSettings) = update { it.copy(settings = change(it.settings)) }
}
