@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package id.wynn.arithmemo.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.wynn.arithmemo.BuildConfig
import id.wynn.arithmemo.R
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.data.MemoStore
import id.wynn.arithmemo.update.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private enum class Page(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Outlined.Home),
    LIBRARY("Hafalan", Icons.Outlined.Style),
    STATS("Statistik", Icons.Outlined.Insights),
    SETTINGS("Pengaturan", Icons.Outlined.Settings),
}

@Composable
fun MemoApp(vm: MemoViewModel) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val update by vm.updater.state.collectAsStateWithLifecycle()
    val automatic by vm.updater.automatic.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var practice by rememberSaveable { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<StudyMode?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let {
                vm.export { data ->
                    context.contentResolver.openOutputStream(it, "wt")?.use { output ->
                        output.write(data.toByteArray())
                    } ?: error("File tidak dapat ditulis")
                }
            }
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            importUri = uri
        }
    val notifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.message.value =
                if (granted) "Notifikasi pembaruan diaktifkan"
                else "Izin notifikasi belum diberikan. Update tetap dapat diperiksa di aplikasi."
        }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.dismissMessage()
        }
    }
    LaunchedEffect(loaded) { if (loaded && automatic) vm.checkUpdate() }
    DisposableEffect(lifecycle, practice) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.foreground(practice)
                Lifecycle.Event.ON_STOP -> vm.foreground(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        vm.foreground(practice && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            lifecycle.removeObserver(observer)
            vm.foreground(false)
        }
    }
    LaunchedEffect(practice, loaded) {
        while (practice && loaded) {
            delay(250)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) vm.tick()
        }
    }
    BackHandler(practice || page != Page.HOME) {
        if (practice) practice = false else page = Page.HOME
    }
    fun start(mode: StudyMode) {
        if (snapshot.session?.let { !it.finished } == true) pendingMode = mode
        else {
            vm.start(mode)
            practice = true
        }
    }
    ArithMemoTheme(snapshot.settings.theme) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (!loaded)
                Box(
                    Modifier.fillMaxSize().safeDrawingPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Menyiapkan hafalan…")
                        message?.let {
                            Text(it)
                            Button(
                                onClick = {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.provider.Settings
                                                .ACTION_INTERNAL_STORAGE_SETTINGS
                                        )
                                    )
                                }
                            ) {
                                Text("Periksa penyimpanan")
                            }
                        }
                    }
                }
            else if (practice && snapshot.session != null)
                SessionScreen(
                    snapshot.session!!,
                    busy,
                    vm,
                    { practice = false },
                    {
                        vm.abandon()
                        practice = false
                    },
                    snapshot.settings.haptics,
                    snackbar,
                )
            else
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 600.dp
                    Row {
                        if (wide)
                            NavigationRail(
                                Modifier.fillMaxHeight().safeDrawingPadding(),
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                Spacer(Modifier.height(24.dp))
                                Page.entries.forEach { p ->
                                    NavigationRailItem(
                                        selected = page == p,
                                        onClick = { page = p },
                                        icon = { Icon(p.icon, null) },
                                        label = { Text(p.label) },
                                    )
                                }
                            }
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            snackbarHost = { SnackbarHost(snackbar) },
                            bottomBar = {
                                if (!wide)
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp,
                                    ) {
                                        Page.entries.forEach { p ->
                                            NavigationBarItem(
                                                selected = page == p,
                                                onClick = { page = p },
                                                icon = { Icon(p.icon, null) },
                                                label = { Text(p.label, maxLines = 2) },
                                            )
                                        }
                                    }
                            },
                        ) { padding ->
                            Box(
                                Modifier.fillMaxSize()
                                    .padding(padding)
                                    .consumeWindowInsets(padding),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Crossfade(page, label = "Navigasi") { selected ->
                                    when (selected) {
                                        Page.HOME ->
                                            HomeScreen(
                                                snapshot,
                                                update.available != null,
                                                { start(it) },
                                                { practice = true },
                                                { page = Page.SETTINGS },
                                                { page = Page.LIBRARY },
                                            )
                                        Page.LIBRARY ->
                                            LibraryScreen(
                                                snapshot,
                                                vm::settings,
                                                { start(StudyMode.FLASHCARD) },
                                            )
                                        Page.STATS -> StatsScreen(snapshot)
                                        Page.SETTINGS ->
                                            SettingsScreen(
                                                snapshot.settings,
                                                vm::settings,
                                                update,
                                                automatic,
                                                { value ->
                                                    vm.updater.automatic(value)
                                                    UpdateWorker.schedule(context)
                                                },
                                                vm::checkUpdate,
                                                vm::downloadUpdate,
                                                { vm.install { context.startActivity(it) } },
                                                vm::cancelDownload,
                                                {
                                                    if (Build.VERSION.SDK_INT >= 33)
                                                        notifications.launch(
                                                            Manifest.permission.POST_NOTIFICATIONS
                                                        )
                                                    else
                                                        vm.message.value =
                                                            "Notifikasi mengikuti pengaturan Android"
                                                },
                                                {
                                                    export.launch(
                                                        "ArithMemo-${LocalDate.now()}.json"
                                                    )
                                                },
                                                {
                                                    import.launch(
                                                        arrayOf(
                                                            "application/json",
                                                            "text/plain",
                                                            "application/octet-stream",
                                                        )
                                                    )
                                                },
                                            )
                                    }
                                }
                                if (busy)
                                    LinearProgressIndicator(
                                        Modifier.fillMaxWidth().align(Alignment.TopCenter)
                                    )
                            }
                        }
                    }
                }
            pendingMode?.let { mode ->
                AlertDialog(
                    onDismissRequest = { pendingMode = null },
                    title = { Text("Mulai sesi baru?") },
                    text = {
                        Text(
                            "Jawaban yang selesai tetap tersimpan. Sisa sesi saat ini akan diganti."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingMode = null
                                vm.start(mode)
                                practice = true
                            }
                        ) {
                            Text("Mulai baru")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingMode = null
                                practice = true
                            }
                        ) {
                            Text("Lanjutkan sesi")
                        }
                    },
                )
            }
            importUri?.let { uri ->
                AlertDialog(
                    onDismissRequest = { importUri = null },
                    title = { Text("Pulihkan cadangan?") },
                    text = {
                        Text(
                            "Data hafalan saat ini akan diganti dengan isi cadangan. Ekspor data Anda terlebih dahulu bila ingin menyimpannya."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                importUri = null
                                vm.import {
                                    context.contentResolver.openInputStream(uri)?.use {
                                        MemoStore.readBackup(it)
                                    } ?: error("Cadangan tidak dapat dibaca")
                                }
                            }
                        ) {
                            Text("Pulihkan")
                        }
                    },
                    dismissButton = { TextButton(onClick = { importUri = null }) { Text("Batal") } },
                )
            }
        }
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.widthIn(max = 720.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
private fun Heading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = .10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint)
    }
}

@Composable
private fun ModeRow(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ActionIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    s: Snapshot,
    hasUpdate: Boolean,
    start: (StudyMode) -> Unit,
    resume: () -> Unit,
    updates: () -> Unit,
    library: () -> Unit,
) {
    val stats = remember(s.attempts) { Stats.from(s.attempts) }
    val today = stats.today()
    val due =
        s.progress.count {
            s.settings.config.matches(Fact.parse(it.key)) &&
                it.value.due <= System.currentTimeMillis()
        }
    PageColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ArithMemo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    LocalDate.now()
                        .format(
                            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("id"))
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Image(
                painterResource(R.drawable.brand_icon),
                contentDescription = "Ikon ArithMemo",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
            )
        }
        Heading("Sedikit setiap hari.", "Buat angka terasa akrab, satu hafalan sekali waktu.")
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TrackChanges, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Target hari ini", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${stats.streak()} hari", style = MaterialTheme.typography.labelLarge)
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "$today",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "/ ${s.settings.goal} jawaban",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                LinearProgressIndicator(
                    progress = { (today.toFloat() / s.settings.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .25f),
                )
                Text(
                    if (today >= s.settings.goal) "Target tercapai. Kerja bagus hari ini!"
                    else
                        "${(s.settings.goal-today).coerceAtLeast(0)} jawaban lagi. Pelan-pelan juga berarti maju.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        s.session
            ?.takeIf { !it.finished }
            ?.let {
                ModeRow(
                    Icons.Outlined.PlayArrow,
                    "Lanjutkan sesi",
                    "${it.mode.label} · ${it.answered} jawaban tersimpan",
                    resume,
                )
            }
        if (hasUpdate)
            ModeRow(
                Icons.Outlined.SystemUpdate,
                "Pembaruan tersedia",
                "Lihat versi terbaru ArithMemo",
                updates,
            )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("${s.progress.count {it.value.mastered}}", "Dikuasai", Modifier.weight(1f))
            Metric(
                if (stats.total > 0) "${stats.accuracy}%" else "—",
                "Akurasi",
                Modifier.weight(1f),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Pilih ritme Anda",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                s.settings.config.operations.joinToString(" · ") { it.label } +
                    " · ${s.settings.config.digits} digit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeRow(
                Icons.Outlined.Psychology,
                "Latihan mengingat",
                "10 soal, tanpa tekanan waktu",
                { start(StudyMode.RECALL) },
            )
            ModeRow(
                Icons.Outlined.AutoAwesome,
                "Ulangi terjadwal",
                if (due > 0) "$due kartu jatuh tempo · lanjutkan hafalan"
                else "Kartu baru dan pengulangan adaptif",
                { start(StudyMode.ADAPTIVE) },
            )
            ModeRow(
                Icons.Outlined.Timer,
                "Tes kecepatan",
                "60 detik · seberapa lancar Anda?",
                { start(StudyMode.SPEED) },
            )
            ModeRow(
                Icons.Outlined.Shuffle,
                "Soal acak",
                "10 soal dari rentang pilihan Anda",
                { start(StudyMode.RANDOM) },
            )
        }
        OutlinedButton(
            onClick = library,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Icon(Icons.Outlined.Style, null)
            Spacer(Modifier.width(8.dp))
            Text("Buka ruang hafalan")
        }
        Text(
            "Tersimpan di perangkat Anda. Siap dipakai tanpa internet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Filters(settings: UserSettings, change: (UserSettings) -> Unit) {
    val cfg = settings.config
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Operasi", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Operation.entries.forEach { op ->
                FilterChip(
                    selected = op in cfg.operations,
                    onClick = {
                        val next =
                            if (op in cfg.operations) cfg.operations - op else cfg.operations + op
                        if (next.isNotEmpty())
                            change(settings.copy(config = cfg.copy(operations = next)))
                    },
                    label = { Text(op.symbol + " " + op.label) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text("Jumlah digit", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { digits ->
                FilterChip(
                    selected = cfg.digits == digits,
                    onClick = { change(settings.copy(config = cfg.copy(digits = digits))) },
                    label = { Text("$digits digit") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text("Kelompok hafalan", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FactGroup.entries.forEach { group ->
                FilterChip(
                    selected = cfg.group == group,
                    onClick = { change(settings.copy(config = cfg.copy(group = group))) },
                    label = { Text(group.label) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(
            "Bagian awal/akhir membagi rentang angka pertama menjadi dua. Kedua operand mengikuti jumlah digit yang dipilih.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryScreen(s: Snapshot, change: (UserSettings) -> Unit, start: () -> Unit) {
    val cfg = s.settings.config
    var cards by remember { mutableStateOf(emptyList<Fact>()) }
    LaunchedEffect(cfg) {
        cards =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                Facts.catalog(cfg)
            }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var index by rememberSaveable(cfg) { mutableIntStateOf(0) }
    var revealed by rememberSaveable(cfg, index) { mutableStateOf(false) }
    var page by rememberSaveable(cfg) { mutableIntStateOf(0) }
    LazyColumn(
        Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("factList"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Heading("Ruang hafalan", "Lihat, balik, lalu ingat kembali.") }
        item { Filters(s.settings, change) }
        item {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Daftar", "Flashcard").forEachIndexed { i, t ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(t) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        item {
            Text(
                "${cards.size} kartu pilihan · ${cfg.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (tab == 0) {
            items(cards.drop(page * 30).take(30), key = { it.key }) { f ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${f.prompt} = ${f.answer}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        val p = s.progress[f.key]
                        Text(
                            if (p?.mastered == true) "Dikuasai"
                            else if (p != null) "Dipelajari" else "Baru",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (p?.mastered == true) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { page-- }, enabled = page > 0) { Text("Sebelumnya") }
                    Text(
                        "${page+1} / ${((cards.size+29)/30).coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { page++ }, enabled = (page + 1) * 30 < cards.size) {
                        Text("Berikutnya")
                    }
                }
            }
        } else if (cards.isNotEmpty()) {
            val fact = cards[index.coerceAtMost(cards.lastIndex)]
            item {
                EquationCard(
                    fact,
                    if (revealed) fact.answer.toString() else "?",
                    if (revealed) "Baca dan ingat kembali" else "Pikirkan jawabannya",
                    revealed,
                )
            }
            item {
                Button(
                    onClick = { revealed = !revealed },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Outlined.FlipToFront, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (revealed) "Sembunyikan jawaban" else "Balik kartu")
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = {
                            index--
                            revealed = false
                        },
                        enabled = index > 0,
                    ) {
                        Text("Sebelumnya")
                    }
                    Text("${index+1} / ${cards.size}")
                    TextButton(
                        onClick = {
                            index++
                            revealed = false
                        },
                        enabled = index < cards.lastIndex,
                    ) {
                        Text("Berikutnya")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = start,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text("Latih dengan flashcard")
                }
            }
        }
        item {
            Text(
                "Daftar dan pratinjau tidak mengubah statistik. Mulai latihan untuk menjadwalkan hafalan. Pada 2–3 digit, katalog berisi hingga 240 pilihan per operasi; Soal acak memakai seluruh rentang.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EquationCard(fact: Fact, answer: String, hint: String, revealed: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(
            Modifier.padding(24.dp).heightIn(min = 160.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                fact.op.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                fact.prompt,
                modifier = Modifier.testTag("question"),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "= $answer",
                modifier = if (revealed) Modifier.testTag("revealedAnswer") else Modifier,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SessionScreen(
    s: StudySession,
    busy: Boolean,
    vm: MemoViewModel,
    pause: () -> Unit,
    finish: () -> Unit,
    haptics: Boolean,
    snackbar: SnackbarHostState,
) {
    val feedback = LocalHapticFeedback.current
    LaunchedEffect(s.feedback) {
        if (s.feedback != null && haptics)
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(s.mode.label, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = pause) { Icon(Icons.Outlined.Close, "Jeda sesi") }
                },
                actions = {
                    Text(
                        if (s.mode == StudyMode.SPEED) "${(s.remainingMs+999)/1000} dtk"
                        else "${(s.index+1).coerceAtMost(s.cards.size)} / ${s.cards.size}",
                        modifier = Modifier.padding(end = 20.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (s.finished) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Outlined.TaskAlt,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Heading(
                        "Satu langkah lebih lancar.",
                        "Semua jawaban sudah tersimpan di perangkat.",
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("${s.correct}/${s.answered}", "Benar", Modifier.weight(1f))
                        Metric(
                            if (s.answered > 0) "${s.correct*100/s.answered}%" else "—",
                            "Akurasi sesi",
                            Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Text(
                        if (s.answered == 0)
                            "Belum ada jawaban. Anda dapat mencoba lagi kapan saja."
                        else "Kartu yang sulit akan muncul lebih cepat pada pengulangan terjadwal.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Button(
                        onClick = finish,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        enabled = !busy,
                    ) {
                        Text("Kembali ke beranda")
                    }
                }
            } else {
                val f = s.current
                if (f != null) {
                    item {
                        LinearProgressIndicator(
                            progress = {
                                if (s.mode == StudyMode.SPEED) 1 - s.remainingMs / 60000f
                                else s.index.toFloat() / s.cards.size
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    }
                    item {
                        EquationCard(
                            f,
                            if (s.revealed) f.answer.toString() else "?",
                            if (s.mode == StudyMode.FLASHCARD)
                                "Ingat dahulu sebelum membuka jawaban"
                            else "Temukan jawabannya dari ingatan",
                            s.revealed,
                        )
                    }
                    if (s.feedback != null) {
                        item {
                            Surface(
                                color =
                                    if (s.feedback) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.large,
                                modifier =
                                    Modifier.fillMaxWidth().semantics {
                                        liveRegion = LiveRegionMode.Polite
                                    },
                            ) {
                                Column(
                                    Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        if (s.feedback) "Benar, sudah tersimpan."
                                        else "Belum tepat. Mari ingat lagi.",
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text("${f.prompt} = ${f.answer}")
                                    if (!s.feedback && s.mode != StudyMode.FLASHCARD)
                                        Text(
                                            "Jawaban Anda: ${s.typed}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = vm::next,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                enabled = !busy,
                            ) {
                                Text(if (s.index == s.cards.lastIndex) "Lihat hasil" else "Lanjut")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                            }
                        }
                    } else if (s.mode == StudyMode.FLASHCARD) {
                        if (!s.revealed)
                            item {
                                Button(
                                    onClick = vm::reveal,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    enabled = !busy,
                                ) {
                                    Text("Buka jawaban")
                                }
                            }
                        else {
                            item {
                                Text(
                                    "Seberapa mudah Anda mengingatnya?",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Rating.entries.chunked(2).forEach { ratings ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            ratings.forEach { rating ->
                                                OutlinedButton(
                                                    onClick = { vm.answer(rating) },
                                                    enabled = !busy,
                                                    modifier =
                                                        Modifier.weight(1f).heightIn(min = 56.dp),
                                                ) {
                                                    Text(rating.label)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = s.typed,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Jawaban Anda") },
                                placeholder = { Text("Gunakan papan angka") },
                                modifier = Modifier.fillMaxWidth().testTag("answerInput"),
                                textStyle = MaterialTheme.typography.headlineSmall,
                                singleLine = true,
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                        listOf("1", "2", "3"),
                                        listOf("4", "5", "6"),
                                        listOf("7", "8", "9"),
                                        listOf("hapus", "0", "periksa"),
                                    )
                                    .forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            row.forEach { key ->
                                                val label =
                                                    when (key) {
                                                        "hapus" -> "Hapus angka terakhir"
                                                        "periksa" -> "Periksa jawaban"
                                                        else -> "Angka $key"
                                                    }
                                                FilledTonalButton(
                                                    onClick = {
                                                        when (key) {
                                                            "hapus" -> vm.input(s.typed.dropLast(1))
                                                            "periksa" -> vm.answer()
                                                            else -> vm.input(s.typed + key)
                                                        }
                                                    },
                                                    enabled =
                                                        !busy &&
                                                            (key != "periksa" ||
                                                                s.typed.isNotEmpty()),
                                                    modifier =
                                                        Modifier.weight(1f)
                                                            .heightIn(min = 56.dp)
                                                            .semantics {
                                                                contentDescription = label
                                                            },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors =
                                                        if (key == "periksa")
                                                            ButtonDefaults.filledTonalButtonColors(
                                                                containerColor =
                                                                    MaterialTheme.colorScheme
                                                                        .primary,
                                                                contentColor =
                                                                    MaterialTheme.colorScheme
                                                                        .onPrimary,
                                                            )
                                                        else
                                                            ButtonDefaults.filledTonalButtonColors(),
                                                ) {
                                                    when (key) {
                                                        "hapus" ->
                                                            Icon(
                                                                Icons.AutoMirrored.Outlined
                                                                    .Backspace,
                                                                null,
                                                            )
                                                        "periksa" ->
                                                            Icon(Icons.Outlined.Check, null)
                                                        else ->
                                                            Text(
                                                                key,
                                                                style =
                                                                    MaterialTheme.typography
                                                                        .titleLarge,
                                                            )
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    if (s.mode == StudyMode.SPEED)
                        item {
                            Text(
                                "Waktu dijeda saat Anda meninggalkan sesi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(s: Snapshot) {
    val stats = remember(s.attempts) { Stats.from(s.attempts) }
    val now = System.currentTimeMillis()
    val due = s.progress.count { it.value.due <= now }
    val mastered = s.progress.count { it.value.mastered }
    PageColumn {
        Heading("Perkembangan Anda", "Kemajuan kecil tetap layak dicatat.")
        if (stats.total == 0)
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Outlined.Insights,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Perjalanan Anda dimulai di sini",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Selesaikan satu latihan. Akurasi, kecepatan, dan penguasaan Anda akan muncul di sini."
                    )
                }
            }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("${stats.total}", "Total jawaban", Modifier.weight(1f))
            Metric(
                if (stats.total == 0) "—" else "${stats.accuracy}%",
                "Akurasi",
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(
                if (stats.averageMs == 0L) "—"
                else
                    String.format(
                        Locale.forLanguageTag("id"),
                        "%.1f dtk",
                        stats.averageMs / 1000.0,
                    ),
                "Rata-rata benar",
                Modifier.weight(1f),
            )
            Metric("${stats.streak()}", "Hari beruntun", Modifier.weight(1f))
        }
        Text(
            "Penguasaan hafalan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatLine("Dikuasai", mastered.toString())
                StatLine("Sedang dipelajari", (s.progress.size - mastered).toString())
                StatLine("Jatuh tempo", due.toString())
                Text(
                    "Dikuasai = minimal 3 jawaban berhasil berturut-turut dan interval pengulangan minimal 7 hari. Penilaian flashcard dihitung sebagai laporan ingatan; waktu hanya dari jawaban ketik yang benar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "7 hari terakhir",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                val max =
                    days.maxOfOrNull { stats.byDay[it] ?: 0 }?.coerceAtLeast(s.settings.goal)
                        ?: s.settings.goal
                days.forEach { day ->
                    val count = stats.byDay[day] ?: 0
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                    ) {
                        StatLine(
                            if (day == LocalDate.now()) "Hari ini"
                            else
                                day.format(
                                    DateTimeFormatter.ofPattern(
                                        "EEE, d MMM",
                                        Locale.forLanguageTag("id"),
                                    )
                                ),
                            "$count jawaban",
                        )
                        LinearProgressIndicator(
                            progress = { count.toFloat() / max },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    }
                }
            }
        }
        Text(
            "Per operasi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Operation.entries.forEach { op ->
            val attempts = s.attempts.filter { it.fact.op == op }
            val a = Stats.from(attempts)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatLine(
                        op.label,
                        if (a.total == 0) "Belum dilatih" else "${a.accuracy}% benar",
                    )
                    Text(
                        "${a.total} jawaban · ${s.progress.count {Fact.parse(it.key).op==op && it.value.mastered}} kartu dikuasai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsScreen(
    settings: UserSettings,
    change: (UserSettings) -> Unit,
    update: AppUpdater.State,
    automatic: Boolean,
    auto: (Boolean) -> Unit,
    check: () -> Unit,
    download: () -> Unit,
    install: () -> Unit,
    cancel: () -> Unit,
    notifications: () -> Unit,
    export: () -> Unit,
    import: () -> Unit,
) {
    PageColumn {
        Heading("Nyaman dengan cara Anda", "Atur rutinitas kecil yang ingin Anda jaga.")
        Text(
            "Target harian",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text("${settings.goal} jawaban per hari", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 20, 30, 50, 100).forEach { goal ->
                FilterChip(
                    selected = settings.goal == goal,
                    onClick = { change(settings.copy(goal = goal)) },
                    label = { Text("$goal") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(
            "Pilihan latihan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Filters(settings, change)
        Text("Tampilan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Sistem", "light" to "Terang", "dark" to "Gelap").forEach {
                (key, label) ->
                FilterChip(
                    selected = settings.theme == key,
                    onClick = { change(settings.copy(theme = key)) },
                    label = { Text(label) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        ToggleRow(
            "Getaran jawaban",
            "Umpan balik halus setelah menjawab",
            settings.haptics,
            { change(settings.copy(haptics = it)) },
        )
        HorizontalDivider()
        Text(
            "Cadangan data",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Data disimpan lokal. Ekspor cadangan sebelum pindah perangkat atau menghapus aplikasi.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = export, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Outlined.FileUpload, null)
            Spacer(Modifier.width(8.dp))
            Text("Ekspor cadangan")
        }
        OutlinedButton(onClick = import, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Outlined.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("Pulihkan cadangan")
        }
        HorizontalDivider()
        Text(
            "Pembaruan aplikasi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        ToggleRow(
            "Periksa otomatis",
            "Sekitar sekali sehari saat internet tersedia",
            automatic,
            auto,
        )
        OutlinedButton(
            onClick = notifications,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Icon(Icons.Outlined.NotificationsActive, null)
            Spacer(Modifier.width(8.dp))
            Text("Izinkan notifikasi update")
        }
        Text(
            update.message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color =
                if (update.error) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = check,
            enabled = !update.checking && update.progress == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(if (update.checking) "Memeriksa…" else "Periksa pembaruan")
        }
        update.available?.let { release ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Versi ${release.versionName}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (release.notes.isNotBlank()) Text(release.notes)
                    Text(
                        "${String.format(Locale.forLanguageTag("id"),"%.1f",release.sizeBytes/1048576.0)} MB · rilis resmi GitHub",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    update.progress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Mengunduh ${(progress*100).toInt()}%")
                        TextButton(onClick = cancel) { Text("Batalkan unduhan") }
                    }
                    if (update.progress == null)
                        Button(
                            onClick = if (update.ready) install else download,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Text(if (update.ready) "Pasang update" else "Unduh update")
                        }
                }
            }
        }
        HorizontalDivider()
        Text(
            "ArithMemo ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Hafalan aritmatika pribadi. Tanpa akun, iklan, atau analitik. Internet hanya untuk pemeriksaan dan unduhan update GitHub. Pemasangan selalu meminta persetujuan Anda di Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Plus Jakarta Sans · SIL Open Font License\nAndroidX Compose & Material Icons · Apache 2.0\nKode aplikasi · MIT · Wynn Dev",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    value: Boolean,
    change: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = value,
            onCheckedChange = change,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}
