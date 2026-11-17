package id.wynn.arithmemo.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.data.MemoStore
import id.wynn.arithmemo.update.AppUpdater
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MemoViewModel(app: Application) : AndroidViewModel(app) {
    private val store = MemoStore(app)
    private val mutex = Mutex()
    val snapshot = MutableStateFlow(Snapshot())
    val loaded = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val updater = AppUpdater(app)
    private var downloadJob: Job? = null
    private var clock = 0L
    private var lastSaved = 0L
    private var foreground = false

    init {
        action {
            snapshot.value = withContext(Dispatchers.IO) { store.read() }
            loaded.value = true
        }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                busy.value = true
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message.value = e.message ?: "Tidak dapat menyimpan. Coba lagi."
                } finally {
                    busy.value = false
                }
            }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    fun settings(next: UserSettings) = action {
        withContext(Dispatchers.IO) { store.saveSettings(next) }
        snapshot.value = snapshot.value.copy(settings = next)
    }

    fun start(mode: StudyMode) = action {
        val s = snapshot.value
        val session =
            withContext(Dispatchers.Default) { Facts.session(s.settings.config, mode, s.progress) }
        withContext(Dispatchers.IO) { store.saveSession(session) }
        snapshot.value = s.copy(session = session)
        clock = SystemClock.elapsedRealtime()
        lastSaved = clock
    }

    fun abandon() = action {
        withContext(Dispatchers.IO) { store.saveSession(null) }
        snapshot.value = snapshot.value.copy(session = null)
    }

    fun input(value: String) {
        if (busy.value) return
        val s = snapshot.value.session ?: return
        if (s.feedback != null || s.finished) return
        if (value.length <= 6 && value.all { it in '0'..'9' })
            snapshot.value = snapshot.value.copy(session = s.copy(typed = value))
    }

    fun reveal() = action {
        val s = snapshot.value.session ?: return@action
        val next = s.copy(revealed = true)
        withContext(Dispatchers.IO) { store.saveSession(next) }
        snapshot.value = snapshot.value.copy(session = next)
    }

    fun answer(rating: Rating? = null) = action {
        updateClock()
        val s = snapshot.value.session ?: return@action
        val fact = s.current ?: return@action
        if (s.finished || s.feedback != null) return@action
        if (s.mode != StudyMode.FLASHCARD && s.typed.isBlank()) return@action
        if (s.mode == StudyMode.FLASHCARD && !s.revealed) return@action
        val correct =
            if (s.mode == StudyMode.FLASHCARD) rating != Rating.AGAIN
            else s.typed.toIntOrNull() == fact.answer
        val review = rating ?: if (correct) Rating.GOOD else Rating.AGAIN
        val next =
            s.copy(
                feedback = correct,
                revealed = true,
                answered = s.answered + 1,
                correct = s.correct + if (correct) 1 else 0,
            )
        val attempt =
            Attempt(
                "${s.id}:${s.index}",
                fact,
                correct,
                if (s.mode == StudyMode.FLASHCARD) 0 else s.answerMs.coerceAtMost(86_400_000),
                review,
                System.currentTimeMillis(),
            )
        withContext(Dispatchers.IO) { store.record(attempt, next) }
        snapshot.value = withContext(Dispatchers.IO) { store.read() }
    }

    fun next() = action {
        updateClock()
        val s = snapshot.value.session ?: return@action
        if (s.feedback == null) return@action
        val index = s.index + 1
        val next =
            s.copy(
                index = index,
                active = index < s.cards.size && (s.mode != StudyMode.SPEED || s.remainingMs > 0),
                feedback = null,
                revealed = false,
                typed = "",
                answerMs = 0,
            )
        withContext(Dispatchers.IO) { store.saveSession(next) }
        snapshot.value = snapshot.value.copy(session = next)
    }

    private fun updateClock() {
        val now = SystemClock.elapsedRealtime()
        val delta = if (foreground && clock > 0) (now - clock).coerceAtLeast(0) else 0
        clock = now
        val s = snapshot.value.session ?: return
        if (s.finished) return
        val remaining =
            if (s.mode == StudyMode.SPEED) (s.remainingMs - delta).coerceAtLeast(0)
            else s.remainingMs
        snapshot.value =
            snapshot.value.copy(
                session =
                    s.copy(
                        remainingMs = remaining,
                        answerMs = if (s.feedback == null) s.answerMs + delta else s.answerMs,
                    )
            )
    }

    fun tick() {
        if (!loaded.value || busy.value) return
        updateClock()
        if (SystemClock.elapsedRealtime() - lastSaved >= 2000) {
            lastSaved = SystemClock.elapsedRealtime()
            persistSession()
        }
    }

    fun foreground(value: Boolean) {
        if (!value) {
            updateClock()
            foreground = false
            persistSession()
        } else {
            foreground = true
            clock = SystemClock.elapsedRealtime()
        }
    }

    private fun persistSession() = action {
        val s = snapshot.value.session
        withContext(Dispatchers.IO) { store.saveSession(s) }
    }

    fun export(write: (String) -> Unit) = action {
        withContext(Dispatchers.IO) { write(store.export()) }
        message.value = "Cadangan berhasil disimpan"
    }

    fun import(read: () -> String) = action {
        withContext(Dispatchers.IO) { store.import(read()) }
        snapshot.value = withContext(Dispatchers.IO) { store.read() }
        message.value = "Cadangan dipulihkan"
    }

    fun checkUpdate() {
        viewModelScope.launch { updater.check() }
    }

    fun downloadUpdate() {
        if (downloadJob?.isActive != true)
            downloadJob = viewModelScope.launch { updater.download() }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    fun install(launch: (android.content.Intent) -> Unit) {
        viewModelScope.launch {
            updater.install()?.let { intent ->
                runCatching { launch(intent) }
                    .onFailure { message.value = "Pemasang Android tidak tersedia" }
            }
        }
    }

    override fun onCleared() {
        store.close()
        super.onCleared()
    }
}
