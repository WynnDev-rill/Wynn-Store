package id.wynn.vocamemo

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class VocaApp : Application() {
    val store by lazy { PersonalStore(VocaDatabase.open(this)) }
    val dictionary by lazy { Dictionary(this) }

    override fun onCreate() {
        super.onCreate()
        Updates.schedule(this)
    }
}

data class Practice(
    val cards: List<Memo>,
    val mode: String,
    val reverse: Boolean,
    val index: Int = 0,
    val revealed: Boolean = false,
    val checked: Boolean = false,
    val correct: Boolean = false,
    val typed: String = "",
    val reviewed: Int = 0,
    val right: Int = 0,
    val remainingMs: Long = 60000,
    val finished: Boolean = false,
    val attempt: String = UUID.randomUUID().toString(),
    val elapsedMs: Long = 0,
) {
    val current
        get() = cards[index % cards.size]

    val prompt
        get() = if (reverse) current.meaning else current.word

    val answer
        get() = if (reverse) current.word else current.meaning
}

class VocaModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val root = app as VocaApp
    val store = root.store
    val memos =
        store.dao
            .observeMemos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reviews =
        store.dao
            .observeReviews()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val prefs = app.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    var page by mutableStateOf(saved.get<String>("page") ?: "Kamus")
        private set

    var query by mutableStateOf(saved.get<String>("query") ?: "")
        private set

    var results by mutableStateOf<List<WordResult>>(emptyList())
        private set

    var searching by mutableStateOf(false)
        private set

    var count by mutableIntStateOf(0)
        private set

    var selected by mutableStateOf("")
        private set

    var senses by mutableStateOf<List<Sense>>(emptyList())
        private set

    var examples by mutableStateOf<List<Example>>(emptyList())
        private set

    var detailLoading by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
    var goal by mutableIntStateOf(prefs.getInt("goal", 20))
        private set

    var theme by mutableStateOf(prefs.getString("theme", "Sistem") ?: "Sistem")
        private set

    var practice by mutableStateOf<Practice?>(null)
        private set

    var busy by mutableStateOf(false)
        private set

    var release by mutableStateOf<Release?>(null)
        private set

    var updateBusy by mutableStateOf(false)
        private set

    var updateStatus by mutableStateOf("Pemeriksaan otomatis setiap hari saat online.")
        private set

    private var searchJob: Job? = null
    private var detailJob: Job? = null

    init {
        launch { count = withContext(Dispatchers.IO) { root.dictionary.count() } }
        if (query.isNotBlank()) search(query)
        checkUpdates(false)
    }

    private fun launch(block: suspend () -> Unit) =
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message ?: "Operasi gagal. Coba lagi."
            }
        }

    fun page(value: String) {
        page = value
        saved["page"] = value
    }

    fun search(value: String) {
        query = value.take(200)
        saved["query"] = query
        searchJob?.cancel()
        results = emptyList()
        if (query.isBlank()) {
            searching = false
            return
        }
        searching = true
        val q = query
        searchJob = launch {
            try {
                delay(180)
                val found = withContext(Dispatchers.IO) { root.dictionary.search(q) }
                if (query == q) results = found
            } finally {
                if (query == q) searching = false
            }
        }
    }

    fun openWord(word: String) {
        detailJob?.cancel()
        selected = word
        senses = emptyList()
        examples = emptyList()
        detailLoading = true
        detailJob = launch {
            try {
                val data =
                    withContext(Dispatchers.IO) {
                        root.dictionary.senses(word) to root.dictionary.examples(word)
                    }
                if (selected == word) {
                    senses = data.first
                    examples = data.second
                }
            } finally {
                if (selected == word) detailLoading = false
            }
        }
    }

    fun closeWord() {
        detailJob?.cancel()
        selected = ""
        detailLoading = false
    }

    fun add(s: Sense, meaning: String) {
        launch {
            val id =
                store.dao.insert(
                    Memo(
                        word = s.word,
                        meaning = meaning,
                        context = s.context,
                        pos = s.pos,
                        ipa = s.ipa,
                        sourceSense = s.source + "|" + s.context,
                    )
                )
            message = if (id == -1L) "Arti ini sudah ada di Memo" else "Disimpan ke Memo"
        }
    }

    fun save(m: Memo) {
        launch {
            require(m.groupName.trim().length in 1..80) {
                "Nama koleksi perlu diisi (maksimal 80 karakter)"
            }
            require(m.meaning.trim().length in 1..1000) {
                "Arti perlu diisi (maksimal 1000 karakter)"
            }
            try {
                val changed =
                    store.db.withTransaction {
                        val previous = store.dao.memo(m.id) ?: return@withTransaction false
                        val meaningChanged =
                            Answers.normalize(previous.meaning) != Answers.normalize(m.meaning)
                        var next =
                            previous.copy(
                                groupName = m.groupName.trim(),
                                meaning = m.meaning.trim(),
                                note = m.note.take(5000),
                                favourite = m.favourite,
                                mastered = m.mastered,
                                memoKey =
                                    memoKey(
                                        previous.word,
                                        m.meaning,
                                        previous.context,
                                        previous.sourceSense,
                                    ),
                            )
                        if (meaningChanged)
                            next =
                                next.copy(
                                    due = 0,
                                    interval = 0,
                                    repetitions = 0,
                                    ease = 2.5,
                                    mastered = false,
                                )
                        store.dao.update(next)
                        meaningChanged
                    }
                message =
                    if (changed) "Arti diperbarui; jadwal hafalan dimulai kembali."
                    else "Memo diperbarui"
            } catch (_: android.database.sqlite.SQLiteConstraintException) {
                message = "Kata dengan arti tersebut sudah ada di Memo."
            }
        }
    }

    fun delete(m: Memo) {
        launch {
            store.dao.delete(m.id)
            message = "Memo dihapus. Riwayat latihan tetap tersimpan."
        }
    }

    fun goal(value: Int) {
        goal = value.coerceIn(1, 500)
        prefs.edit().putInt("goal", goal).apply()
    }

    fun theme(value: String) {
        theme = value
        prefs.edit().putString("theme", value).apply()
    }

    fun start(mode: String, reverse: Boolean, group: String) {
        var cards =
            memos.value.filter { !it.mastered && (group == "Semua" || it.groupName == group) }
        if (mode == "Spaced repetition")
            cards = cards.filter { it.due <= System.currentTimeMillis() }.sortedBy { it.due }
        else cards = cards.shuffled()
        if (cards.isEmpty()) {
            message =
                if (mode == "Spaced repetition") "Belum ada kata yang jatuh tempo. Coba mode lain."
                else "Tambahkan kata yang belum dikuasai ke Memo terlebih dahulu."
            return
        }
        practice = Practice(cards, mode, reverse)
    }

    fun type(value: String) {
        practice = practice?.copy(typed = value.take(1000))
    }

    fun reveal() {
        practice = practice?.copy(revealed = true)
    }

    fun checkAnswer() {
        val p = practice ?: return
        if (p.checked || p.finished) return
        practice =
            p.copy(checked = true, revealed = true, correct = Answers.matches(p.typed, p.answer))
    }

    fun nextUnscored() {
        val p = practice ?: return
        practice =
            p.copy(
                index = p.index + 1,
                revealed = false,
                checked = false,
                typed = "",
                attempt = UUID.randomUUID().toString(),
                elapsedMs = 0,
            )
    }

    fun grade(rating: Int) {
        val p = practice ?: return
        if (busy || p.finished) return
        busy = true
        launch {
            try {
                val result =
                    store.grade(
                        p.current.id,
                        rating,
                        p.mode,
                        if (p.reverse) "ID → EN" else "EN → ID",
                        p.elapsedMs,
                        p.attempt,
                    )
                val current = practice
                if (current?.attempt == p.attempt && result) {
                    val total = p.reviewed + 1
                    val finish = p.mode == "Spaced repetition" && total >= p.cards.size
                    practice =
                        current.copy(
                            index = p.index + 1,
                            revealed = false,
                            checked = false,
                            typed = "",
                            reviewed = total,
                            right = p.right + if (rating >= 2) 1 else 0,
                            finished = current.finished || finish,
                            attempt = UUID.randomUUID().toString(),
                            elapsedMs = 0,
                        )
                }
            } finally {
                busy = false
            }
        }
    }

    fun tick(ms: Long) {
        val p = practice ?: return
        if (p.finished) return
        val delta = ms.coerceIn(0, 2000)
        val remain =
            if (p.mode == "Tes kecepatan") (p.remainingMs - delta).coerceAtLeast(0)
            else p.remainingMs
        practice =
            p.copy(
                remainingMs = remain,
                elapsedMs = p.elapsedMs + delta,
                finished = p.mode == "Tes kecepatan" && remain == 0L,
            )
    }

    fun end() {
        practice = null
    }

    suspend fun exportBackup(): String = store.export(goal)

    fun imported(text: String) {
        launch {
            val added = withContext(Dispatchers.IO) { store.import(text) }
            goal(added.goal)
            message = "${added.added} memo dipulihkan; data yang sudah ada dipertahankan."
        }
    }

    fun report(text: String) {
        message = text
    }

    fun checkUpdates(manual: Boolean = true) {
        if (updateBusy) return
        updateBusy = true
        viewModelScope.launch {
            try {
                release = Updates(root).check()
                updateStatus =
                    if (release == null) "VocaMemo sudah versi terbaru."
                    else "Versi ${release?.versionName} tersedia."
            } catch (e: Exception) {
                updateStatus =
                    if (manual) "Gagal memeriksa update. Periksa koneksi dan coba lagi."
                    else "Offline? Pemeriksaan akan dicoba kembali otomatis."
            } finally {
                updateBusy = false
            }
        }
    }

    fun installUpdate() {
        val r = release ?: return
        if (updateBusy) return
        updateBusy = true
        launch {
            try {
                updateStatus = "Mengunduh dan memverifikasi APK…"
                val file = Updates(root).download(r)
                Updates(root).install(file)
                updateStatus = "APK terverifikasi. Lanjutkan pemasangan di Android."
            } finally {
                updateBusy = false
            }
        }
    }
}
