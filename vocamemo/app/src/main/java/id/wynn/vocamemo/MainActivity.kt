package id.wynn.vocamemo

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: VocaModel = viewModel()
            val dark = usesDarkTheme(vm.theme)
            SideEffect {
                window.setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(windowColor(dark))
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
    var settings by rememberSaveable { mutableStateOf(fromUpdate) }
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
    BackHandler(settings || vm.selected.isNotEmpty() || vm.practice != null || vm.page != "Kamus") {
        when {
            settings -> settings = false
            vm.selected.isNotEmpty() -> vm.closeWord()
            vm.practice != null -> vm.end()
            else -> vm.page("Kamus")
        }
    }
    val wordOpen = vm.selected.isNotEmpty()
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    ReadingBackdrop(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (vm.practice == null) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                if (!settings && !wordOpen && vm.page == "Kamus") {
                                    Image(painterResource(R.drawable.app_icon), null, Modifier.size(30.dp))
                                }
                                Text(
                                    when {
                                        settings -> "Setelan"
                                        wordOpen -> "Kamus"
                                        vm.page == "Kamus" -> "VocaMemo"
                                        else -> vm.page
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        },
                        navigationIcon = {
                            if (settings || wordOpen) {
                                IconButton(onClick = { if (settings) settings = false else vm.closeWord() }) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        if (settings) "Tutup setelan" else "Kembali ke Kamus",
                                    )
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            },
            bottomBar = {
                if (vm.practice == null && !settings && !keyboardOpen) {
                    VocaDock(vm.page) { label ->
                        vm.page(label)
                        vm.closeWord()
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                    when {
                        vm.practice != null -> PracticeScreen(vm, tts)
                        settings -> SettingsScreen(vm)
                        wordOpen -> WordScreen(vm, memos, tts)
                        vm.page == "Kamus" -> DictionaryScreen(vm, memos)
                        vm.page == "Memo" -> MemoScreen(vm, memos, tts)
                        vm.page == "Latihan" -> PracticeHome(vm, memos)
                        else -> StatisticsScreen(vm, memos, reviews)
                    }
                }
            }
        }
    }
}

private val destinations = listOf(
    "Kamus" to Icons.AutoMirrored.Rounded.MenuBook,
    "Memo" to Icons.Rounded.BookmarkBorder,
    "Latihan" to Icons.Rounded.Style,
    "Statistik" to Icons.Rounded.BarChart,
)

@Composable
private fun VocaDock(page: String, select: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        NavigationBar(
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()
                .shadow(7.dp, shape, clip = false).glass(shape),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            val itemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = colors.onPrimaryContainer,
                selectedTextColor = colors.onSurface,
                indicatorColor = colors.primaryContainer,
                unselectedIconColor = colors.onSurfaceVariant,
                unselectedTextColor = colors.onSurfaceVariant,
            )
            destinations.forEach { (label, icon) ->
                NavigationBarItem(
                    selected = page == label,
                    onClick = { select(label) },
                    icon = { Icon(icon, null) },
                    label = { Text(label) },
                    colors = itemColors,
                )
            }
        }
    }
}

@Composable
internal fun Title(text: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        if (subtitle.isNotEmpty())
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
internal fun Hint(
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
internal fun Pill(text: String, icon: ImageVector? = null) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50),
    ) {
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
internal fun FlowRowCompat(content: @Composable () -> Unit) {
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    vm.selected,
                    style = entryType(40, 50),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { tts.speak(vm.selected) },
                    modifier = Modifier.glass(RoundedCornerShape(18.dp)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.VolumeUp,
                        "Dengarkan pengucapan",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (vm.detailLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(vm.senses, key = { it.id }) { sense ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(posLabel(sense.pos))
                    if (sense.ipa.isNotBlank())
                        Text(
                            sense.ipa.replace(" | ", "  ·  "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    if (sense.context.isNotBlank())
                        Text(
                            sense.context,
                            style = MaterialTheme.typography.bodyMedium,
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
                            TextButton(onClick = { vm.add(sense, meaning) }, enabled = !exists) {
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
                    "Tatoeba · konteks contoh bisa berbeda",
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
                "WikDict • Wiktionary contributors via DBnary\nCC BY-SA 4.0 · Juni 2026",
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
