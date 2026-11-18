package id.wynn.vocamemo

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: VocaModel = viewModel()
            val dark = vm.theme == "Gelap" || vm.theme == "Sistem" && isSystemInDarkTheme()
            SideEffect {
                window.setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(
                        if (dark) 0xFF1D1921.toInt() else 0xFFF8F5FC.toInt()
                    )
                )
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                    .apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
            }
            VocaTheme(vm.theme) { VocaScreen(vm, intent.getBooleanExtra("updates", false)) }
        }
    }
}

class Pronouncer(context: android.content.Context, private val report: (String) -> Unit) {
    private var ready = false
    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }
    }

    fun speak(word: String) {
        val t = engine
        if (!ready || t == null) {
            report(
                "TTS belum siap. Coba lagi atau pasang suara Inggris melalui pengaturan Android."
            )
            return
        }
        val voice =
            t.voices
                ?.filter {
                    it.locale.language == "en" &&
                        !it.isNetworkConnectionRequired &&
                        "notInstalled" !in it.features.orEmpty()
                }
                ?.sortedByDescending { it.quality }
                ?.firstOrNull()
        if (voice == null) {
            report(
                "Suara Inggris offline belum terpasang. Buka Setelan → Suara TTS, lalu unduh suara Inggris."
            )
            return
        }
        t.voice = voice
        t.setSpeechRate(0.85f)
        if (t.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word") == TextToSpeech.ERROR)
            report("Pengucapan gagal. Periksa suara TTS di Android.")
    }

    fun stop() {
        engine?.stop()
    }

    fun close() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocaScreen(vm: VocaModel, fromUpdate: Boolean = false) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val memos by vm.memos.collectAsStateWithLifecycle()
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var settings by rememberSaveable { mutableStateOf(fromUpdate) }
    var account by rememberSaveable { mutableStateOf(false) }
    val welcome = !settings && (vm.showWelcome || account)
    val showNav = vm.practice == null && !settings && !welcome
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val goToPage: (String) -> Unit = {
        if (vm.page != it) snackbar.currentSnackbarData?.dismiss()
        vm.page(it)
        vm.closeWord()
    }
    val tts = remember { Pronouncer(context.applicationContext, vm::report) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) tts.stop()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            tts.close()
        }
    }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }
    BackHandler(settings || account || vm.selected.isNotEmpty() || vm.practice != null || vm.page != "Kamus") {
        when {
            settings -> settings = false
            account -> account = false
            vm.selected.isNotEmpty() -> vm.closeWord()
            vm.practice != null -> vm.end()
            else -> vm.page("Kamus")
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (vm.practice == null && !welcome && (settings || vm.selected.isEmpty())) {
                    TopAppBar(
                        title = {
                            Text(
                                when {
                                    settings -> "Setelan"
                                    vm.page == "Kamus" -> ""
                                    vm.page == "Memo" -> "Memo Anda"
                                    else -> vm.page
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        },
                        navigationIcon = {
                            if (settings) {
                                IconButton(onClick = { settings = false }) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Tutup setelan")
                                }
                            } else if (vm.page == "Kamus") {
                                Row(
                                    Modifier.padding(start = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    Image(painterResource(R.drawable.app_icon), null, Modifier.size(28.dp))
                                    Text("Kamus", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        },
                        actions = {
                            if (!settings) {
                                IconButton(onClick = { settings = true }) {
                                    Icon(Icons.Rounded.Settings, "Buka setelan")
                                }
                            }
                        },
                        expandedHeight = 52.dp,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            },
            bottomBar = {
                if (showNav && !wide && !keyboardVisible) VocaNavigation(vm.page, goToPage)
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                if (showNav && wide) VocaRail(vm.page, goToPage)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 740.dp).fillMaxSize()) {
                        when {
                            vm.practice != null -> PracticeScreen(vm, tts)
                            settings -> SettingsScreen(vm)
                            welcome -> WelcomeScreen(
                                firstRun = vm.showWelcome,
                                onProfile = {
                                    vm.setGoogleProfile(it)
                                    if (vm.showWelcome) vm.finishWelcome()
                                    account = false
                                    goToPage("Profil")
                                },
                                onGuest = {
                                    if (vm.showWelcome) vm.finishWelcome()
                                    account = false
                                },
                            )
                            vm.selected.isNotEmpty() -> WordScreen(vm, memos, tts)
                            vm.page == "Kamus" -> DictionaryScreen(vm, memos)
                            vm.page == "Memo" -> MemoScreen(vm, memos, tts)
                            vm.page == "Latihan" -> PracticeHome(vm, memos)
                            else -> ProfileScreen(
                                vm, memos, reviews, onLogin = { account = true },
                                onSignOut = {
                                    vm.setGoogleProfile(null)
                                    scope.launch { clearGoogleAccount(context) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(text: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        if (subtitle.isNotEmpty())
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun Hint(
    icon: ImageVector,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}

@Composable
private fun Pill(text: String, icon: ImageVector? = null) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

@Composable
private fun WordScreen(vm: VocaModel, memos: List<Memo>, tts: Pronouncer) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = vm::closeWord) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                Text("Kembali ke Kamus")
            }
            val colors = MaterialTheme.colorScheme
            val photo = wordPhotos[dictionaryKey(vm.selected)]?.takeIf { asset ->
                vm.senses.any { it.pos == "noun" && it.context == asset.context }
            }
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer)))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (photo != null) {
                    Image(painterResource(photo.drawable), photo.label,
                        Modifier.fillMaxWidth().height(152.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(vm.selected, style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { tts.speak(vm.selected) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Dengarkan pengucapan")
                    }
                }
            }
        }
        if (vm.senses.isNotEmpty()) item {
            Text("Arti & penggunaan", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }

        if (vm.detailLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(vm.senses, key = { it.id }) { sense ->
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill(posLabel(sense.pos))
                    if (sense.ipa.isNotBlank())
                        Text(
                            sense.ipa.replace(" | ", "  ·  "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    if (sense.context.isNotBlank())
                        Text(
                            "Konteks: ${sense.context}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    sense.translations.forEach { meaning ->
                        val exists =
                            memos.any {
                                it.word == sense.word &&
                                    it.meaning == meaning &&
                                    it.sourceSense == sense.source + "|" + sense.context
                            }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                meaning,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            FilledTonalButton(onClick = { vm.add(sense, meaning) }, enabled = !exists) {
                                Icon(
                                    if (exists) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd,
                                    null,
                                    Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (exists) "Tersimpan" else "Simpan")
                            }
                        }
                    }
                }
            }
        }
        if (vm.examples.isNotEmpty()) {
            item {
                Text(
                    "Contoh penggunaan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Contoh kata dari Tatoeba; konteksnya dapat berbeda dengan arti di atas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(vm.examples) { example ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        example.english,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(example.indonesian)
                    Text(
                        example.credit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                }
            }
        }
        item {
            Text(
                "WikDict • Wiktionary contributors via DBnary\nCC BY-SA 4.0 • snapshot Juni 2026\nArti dan kelas kata mengikuti sumber. Konteks tetap dalam bahasa sumber.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://en.wiktionary.org/wiki/${android.net.Uri.encode(vm.selected)}#English"
                                .toUri(),
                        )
                    )
                }
            ) {
                Text("Lihat sumber Wiktionary")
            }
        }
    }
}

private fun posLabel(pos: String) =
    when (pos.lowercase(Locale.ROOT)) {
        "noun" -> "kata benda"
        "verb" -> "kata kerja"
        "adjective" -> "kata sifat"
        "adverb" -> "kata keterangan"
        "pronoun" -> "kata ganti"
        "preposition" -> "kata depan"
        "interjection" -> "kata seru"
        "conjunction" -> "kata hubung"
        else -> pos
    }

@Composable
private fun MemoScreen(vm: VocaModel, memos: List<Memo>, tts: Pronouncer) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Semua") }
    var group by rememberSaveable { mutableStateOf("Semua") }
    var edit by remember { mutableStateOf<Memo?>(null) }
    val shown =
        memos.filter {
            (group == "Semua" || it.groupName == group) &&
                when (filter) {
                    "Favorit" -> it.favourite
                    "Dikuasai" -> it.mastered
                    "Belajar" -> !it.mastered
                    else -> true
                } &&
                (it.word.contains(search, true) ||
                    it.meaning.contains(search, true) ||
                    it.note.contains(search, true))
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            VocaSearch(search, { search = it }, "Cari di Memo", Modifier.fillMaxWidth())
        }
        item {
            FlowRowCompat {
                listOf("Semua", "Belajar", "Favorit", "Dikuasai").forEach { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) },
                    )
                }
            }
            GroupPicker(memos.map { it.groupName }.distinct().sorted(), group, { group = it })
        }
        if (shown.isEmpty())
            item {
                Hint(
                    Icons.Rounded.Bookmarks,
                    if (memos.isEmpty()) "Ruang untuk kata baru" else "Tidak ada memo yang cocok",
                    if (memos.isEmpty())
                        "Cari sebuah kata di Kamus, lalu simpan arti yang ingin Anda ingat."
                    else "Coba kata atau filter lainnya.",
                ) {
                    Button(onClick = { vm.page("Kamus") }) { Text("Buka Kamus") }
                }
            }
        items(shown, key = { it.id }) { memo ->
            Card(
                onClick = { edit = memo },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WordTile(memo.word, wordPhotos[dictionaryKey(memo.word)]?.takeIf {
                            memo.context == it.context && memo.sourceSense.isNotBlank()
                        }, Modifier.size(42.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                memo.word,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(memo.meaning, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { vm.save(memo.copy(favourite = !memo.favourite)) }) {
                            Icon(
                                if (memo.favourite) Icons.Rounded.Star
                                else Icons.Rounded.StarBorder,
                                if (memo.favourite) "Hapus favorit ${memo.word}"
                                else "Favoritkan ${memo.word}",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Pill(
                            if (memo.mastered) "Dikuasai" else memo.groupName,
                            if (memo.mastered) Icons.Rounded.CheckCircle else null,
                        )
                        IconButton(onClick = { tts.speak(memo.word) }) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Ucapkan ${memo.word}")
                        }
                    }
                    if (memo.context.isNotBlank())
                        Text(
                            memo.context,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    if (memo.note.isNotBlank())
                        Text(memo.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    edit?.let { m ->
        MemoEditor(
            m,
            onDismiss = { edit = null },
            onSave = {
                vm.save(it)
                edit = null
            },
            onDelete = {
                vm.delete(m)
                edit = null
            },
        )
    }
}

@Composable
private fun MemoEditor(
    m: Memo,
    onDismiss: () -> Unit,
    onSave: (Memo) -> Unit,
    onDelete: () -> Unit,
) {
    var meaning by remember { mutableStateOf(m.meaning) }
    var group by remember { mutableStateOf(m.groupName) }
    var note by remember { mutableStateOf(m.note) }
    var mastered by remember { mutableStateOf(m.mastered) }
    var confirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(m.word) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    meaning,
                    { meaning = it.take(1000) },
                    label = { Text("Arti yang dihafalkan") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Perubahan arti adalah catatan pribadi Anda.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    group,
                    { group = it.take(80) },
                    label = { Text("Koleksi / kelompok") },
                    singleLine = true,
                )
                OutlinedTextField(
                    note,
                    { note = it.take(5000) },
                    label = { Text("Catatan pribadi") },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(mastered, { mastered = it })
                    Text("Sudah dikuasai", Modifier.weight(1f))
                }
                TextButton(onClick = { confirm = true }) {
                    Icon(Icons.Rounded.DeleteOutline, null)
                    Text("Hapus memo")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = meaning.isNotBlank() && group.isNotBlank(),
                onClick = {
                    onSave(
                        m.copy(
                            meaning = meaning,
                            groupName = group,
                            note = note,
                            mastered = mastered,
                        )
                    )
                },
            ) {
                Text("Simpan perubahan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Hapus ${m.word}?") },
            text = { Text("Memo akan dihapus. Riwayat latihan tetap tersimpan untuk statistik.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } },
        )
}

@Composable
private fun GroupPicker(groups: List<String>, value: String, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Koleksi: $value")
            Icon(Icons.Rounded.ArrowDropDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            (listOf("Semua") + groups.filter { it != "Semua" }).forEach { g ->
                DropdownMenuItem(
                    text = { Text(g) },
                    onClick = {
                        change(g)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PracticeHome(vm: VocaModel, memos: List<Memo>) {
    var reverse by rememberSaveable { mutableStateOf(false) }
    var group by rememberSaveable { mutableStateOf("Semua") }
    val due = memos.count { !it.mastered && it.due <= System.currentTimeMillis() }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            val colors = MaterialTheme.colorScheme
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer)))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = MaterialTheme.shapes.medium, color = colors.surface) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.padding(12.dp), tint = colors.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Pengulangan terjadwal", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold)
                        Text("$due kata siap diulang", color = colors.onSurfaceVariant)
                    }
                }
                Button(onClick = { vm.start("Spaced repetition", reverse, group) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Mulai pengulangan adaptif")
                }
            }
        }
        item {
            Text(
                "Arah latihan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FlowRowCompat {
                FilterChip(!reverse, { reverse = false }, label = { Text("Inggris → Indonesia") })
                FilterChip(reverse, { reverse = true }, label = { Text("Indonesia → Inggris") })
            }
            GroupPicker(memos.map { it.groupName }.distinct().sorted(), group, { group = it })
        }
        item {
            Text(
                "Mode latihan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        items(
            listOf(
                Triple("Flashcard", "Balik kartu, lalu nilai ingatan Anda.", Icons.Rounded.Style),
                Triple("Ulang mekanis", "Putar berulang tanpa batas waktu.", Icons.Rounded.Repeat),
                Triple(
                    "Mengetik",
                    "Tulis arti yang Anda simpan secara tepat.",
                    Icons.Rounded.Keyboard,
                ),
                Triple(
                    "Tes kecepatan",
                    "Tantangan mengetik 60 detik; jeda saat keluar aplikasi.",
                    Icons.Rounded.Timer,
                ),
                Triple(
                    "Daftar",
                    "Baca kata dan arti berdampingan.",
                    Icons.AutoMirrored.Rounded.List,
                ),
            )
        ) { (mode, desc, icon) ->
            Card(
                onClick = { vm.start(mode, reverse, group) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Icon(icon, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            mode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
            }
        }
        item {
            Text(
                "Kata yang ditandai dikuasai tidak masuk latihan. Ubah tandanya di Memo untuk mengulanginya lagi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PracticeScreen(vm: VocaModel, tts: Pronouncer) {
    val p = vm.practice ?: return
    val lifecycle = LocalLifecycleOwner.current
    var active by remember {
        mutableStateOf(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var auto by rememberSaveable { mutableStateOf(false) }
    var exit by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val o = LifecycleEventObserver { _, _ ->
            active = lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.lifecycle.addObserver(o)
        onDispose { lifecycle.lifecycle.removeObserver(o) }
    }
    LaunchedEffect(active, p.finished) {
        if (active && !p.finished) {
            var last = android.os.SystemClock.elapsedRealtime()
            while (true) {
                delay(250)
                val now = android.os.SystemClock.elapsedRealtime()
                vm.tick(now - last)
                last = now
            }
        }
    }
    LaunchedEffect(auto, active, p.attempt, p.revealed) {
        if (auto && active && p.mode == "Ulang mekanis" && !p.finished) {
            delay(if (p.revealed) 2400 else 3000)
            if (p.revealed) vm.nextUnscored() else vm.reveal()
        }
    }
    BackHandler { exit = true }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { exit = true }) { Icon(Icons.Rounded.Close, "Akhiri latihan") }
            Text(
                p.mode,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (p.mode == "Tes kecepatan")
                Pill("${(p.remainingMs+999)/1000} dtk", Icons.Rounded.Timer)
        }
        if (p.finished) {
            Column(
                Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(20.dp))
                Title(
                    "Sesi selesai.",
                    "${p.reviewed} jawaban • ${if(p.reviewed==0)0 else p.right*100/p.reviewed}% akurasi",
                )
                Spacer(Modifier.height(24.dp))
                Text("Progres tersimpan di perangkat ini.")
                Button(onClick = vm::end, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text("Kembali ke Latihan")
                }
            }
        } else if (p.mode == "Daftar") {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(p.cards, key = { it.id }) { m ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(
                                if (p.reverse) m.meaning else m.word,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (p.reverse) m.word else m.meaning,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            IconButton(onClick = { tts.speak(m.word) }) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Ucapkan ${m.word}")
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (p.reverse) "Indonesia → Inggris" else "Inggris → Indonesia",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${p.reviewed} terjawab", style = MaterialTheme.typography.labelLarge)
                }
                if (p.mode == "Spaced repetition")
                    LinearProgressIndicator(
                        progress = { p.reviewed.toFloat() / p.cards.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                val typed = p.mode == "Mengetik" || p.mode == "Tes kecepatan"
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 230.dp)
                            .then(
                                if (!typed) Modifier.clickable(role = Role.Button) { vm.reveal() }
                                else Modifier
                            ),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp).animateContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Text(
                            p.prompt,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )
                        if (p.current.context.isNotBlank())
                            Text(
                                p.current.context,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        if (p.revealed) {
                            HorizontalDivider()
                            AnimatedContent(p.answer, label = "Jawaban kartu") { answer ->
                                Text(
                                    answer,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else
                            Text(
                                if (typed) "Ingat arti yang Anda pilih di Memo."
                                else "Ketuk kartu untuk melihat jawaban",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                    }
                }
                IconButton(onClick = { tts.speak(p.current.word) }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Dengarkan kata")
                }
                if (typed) {
                    OutlinedTextField(
                        p.typed,
                        vm::type,
                        label = { Text("Jawaban Anda") },
                        singleLine = true,
                        enabled = !p.checked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    )
                    if (!p.checked)
                        Button(
                            onClick = vm::checkAnswer,
                            enabled = p.typed.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Periksa jawaban")
                        }
                    else {
                        Text(
                            if (p.correct) "Tepat!" else "Belum tepat. Jawaban: ${p.answer}",
                            color =
                                if (p.correct) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Button(
                            onClick = { vm.grade(if (p.correct) 3 else 1) },
                            enabled = !vm.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Kata berikutnya")
                        }
                    }
                    Text(
                        "Dinilai sesuai arti tersimpan; huruf besar dan spasi ekstra diabaikan. Sinonim lain dapat benar secara bahasa, tetapi bukan jawaban kartu ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (p.mode == "Ulang mekanis") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(auto, { auto = it })
                        Spacer(Modifier.width(12.dp))
                        Text("Putar otomatis")
                    }
                    Text(
                        "Putaran otomatis tidak menambah statistik jawaban.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = vm::reveal,
                            enabled = !p.revealed,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Lihat arti")
                        }
                        Button(onClick = vm::nextUnscored, modifier = Modifier.weight(1f)) {
                            Text("Berikutnya")
                        }
                    }
                } else if (!p.revealed)
                    Button(onClick = vm::reveal, modifier = Modifier.fillMaxWidth()) {
                        Text("Tampilkan jawaban")
                    }
                else {
                    Text(
                        "Seberapa mudah Anda mengingatnya?",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    FlowRowCompat {
                        listOf(1 to "Ulang lagi", 2 to "Sulit", 3 to "Ingat", 4 to "Mudah")
                            .forEach { (r, label) ->
                                OutlinedButton(onClick = { vm.grade(r) }, enabled = !vm.busy) {
                                    Text(label)
                                }
                            }
                    }
                }
            }
        }
    }
    if (exit)
        AlertDialog(
            onDismissRequest = { exit = false },
            title = { Text("Akhiri sesi ini?") },
            text = {
                Text(
                    "Jawaban yang sudah dinilai tetap tersimpan. Sesi bisa dimulai lagi kapan saja."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.end()
                        exit = false
                    }
                ) {
                    Text("Akhiri sesi")
                }
            },
            dismissButton = { TextButton(onClick = { exit = false }) { Text("Lanjut belajar") } },
        )
}

@Composable
private fun SettingsScreen(vm: VocaModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var goal by rememberSaveable { mutableStateOf(vm.goal.toString()) }
    var licenses by remember { mutableStateOf(false) }
    var backupBusy by remember { mutableStateOf(false) }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                backupBusy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val text = vm.exportBackup()
                            context.contentResolver.openOutputStream(uri, "wt")?.use {
                                it.write(text.toByteArray())
                            } ?: error("Tidak dapat menulis cadangan")
                        }
                        vm.report("Cadangan disimpan. Simpan salinannya di tempat aman.")
                    } catch (e: Exception) {
                        vm.report(
                            e.message
                                ?: "Gagal menyimpan cadangan. Pilih lokasi lain dan coba lagi."
                        )
                    } finally {
                        backupBusy = false
                    }
                }
            }
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                backupBusy = true
                scope.launch {
                    try {
                        val text =
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    input.readLimited(BACKUP_LIMIT).toString(Charsets.UTF_8)
                                } ?: error("Berkas tidak dapat dibaca")
                            }
                        vm.imported(text)
                    } catch (_: Exception) {
                        vm.report(
                            "Cadangan tidak valid atau tidak dapat dibaca (maksimal 50 MB). Data Anda tetap aman."
                        )
                    } finally {
                        backupBusy = false
                    }
                }
            }
        }
    val notification =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.report(
                if (granted) "Notifikasi update diaktifkan"
                else "Notifikasi belum diizinkan. Update tetap dapat diperiksa di sini."
            )
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                "Target harian",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                goal,
                { goal = it.filter(Char::isDigit).take(3) },
                label = { Text("Jumlah jawaban per hari (1–500)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
            )
            TextButton(
                onClick = {
                    goal.toIntOrNull()?.let {
                        vm.goal(it)
                        goal = vm.goal.toString()
                        vm.report("Target harian diperbarui")
                    }
                },
                enabled = (goal.toIntOrNull() ?: 0) in 1..500,
            ) {
                Text("Simpan target")
            }
        }
        item {
            Text(
                "Tampilan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FlowRowCompat {
                listOf("Sistem", "Terang", "Gelap").forEach { t ->
                    FilterChip(vm.theme == t, { vm.theme(t) }, label = { Text(t) })
                }
            }
        }
        item {
            Text(
                "Cadangan pribadi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Simpan Memo dan riwayat latihan sebagai JSON. Pemulihan menggabungkan data tanpa menimpa memo yang sudah ada."
            )
            FlowRowCompat {
                OutlinedButton(
                    onClick = { export.launch("VocaMemo-${LocalDate.now()}.json") },
                    enabled = !backupBusy,
                ) {
                    Icon(Icons.Rounded.SaveAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ekspor cadangan")
                }
                OutlinedButton(
                    onClick = {
                        import.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream")
                        )
                    },
                    enabled = !backupBusy,
                ) {
                    Icon(Icons.Rounded.Restore, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pulihkan cadangan")
                }
            }
            if (backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                "Aplikasi tidak mengunggah data. Simpan cadangan sebelum menghapus aplikasi.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Text(
                "Suara TTS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pasang suara Inggris offline dari mesin TTS perangkat. Kecepatan pengucapan diatur lebih pelan untuk belajar."
            )
            OutlinedButton(
                onClick = {
                    runCatching {
                            context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                        }
                        .onFailure {
                            vm.report(
                                "Buka Pengaturan Android → Bahasa & masukan → Text-to-speech."
                            )
                        }
                }
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null)
                Spacer(Modifier.width(8.dp))
                Text("Pengaturan suara Android")
            }
        }
        item {
            Text(
                "Pembaruan aplikasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(vm.updateStatus, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (vm.updateBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            FlowRowCompat {
                OutlinedButton(onClick = { vm.checkUpdates() }, enabled = !vm.updateBusy) {
                    Text("Periksa update")
                }
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33)
                            notification.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else
                            context.startActivity(
                                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(
                                        android.provider.Settings.EXTRA_APP_PACKAGE,
                                        context.packageName,
                                    )
                            )
                    }
                ) {
                    Text("Notifikasi update")
                }
            }
            vm.release?.let { r ->
                Text("${r.versionName}\n${r.notes}")
                Button(onClick = vm::installUpdate, enabled = !vm.updateBusy) {
                    Text("Unduh dan pasang update")
                }
            }
        }
        item {
            Text(
                "Tentang data & privasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Kamus offline · ${java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(vm.count)} kata dari WikDict"
            )
            Text(
                "Memo dan progres tersimpan di perangkat. Login Google bersifat opsional dan hanya untuk identitas profil."
            )
            TextButton(onClick = { licenses = true }) { Text("Lisensi, sumber data, dan atribusi") }
            Text("VocaMemo ${BuildConfig.VERSION_NAME} · Wynn Dev",
                style = MaterialTheme.typography.bodySmall)

        }
    }
    if (licenses) {
        var text by remember { mutableStateOf("Memuat…") }
        LaunchedEffect(Unit) {
            text =
                withContext(Dispatchers.IO) {
                    context.assets.open("ATTRIBUTION.txt").bufferedReader().use { it.readText() }
                }
        }
        AlertDialog(
            onDismissRequest = { licenses = false },
            title = { Text("Sumber & lisensi") },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text,
                        Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { licenses = false }) { Text("Tutup") } },
        )
    }
}

internal fun java.io.InputStream.readLimited(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val b = ByteArray(8192)
    while (true) {
        val n = read(b)
        if (n < 0) break
        require(out.size() + n <= limit) { "Berkas terlalu besar" }
        out.write(b, 0, n)
    }
    return out.toByteArray()
}
