package id.wynn.screenguard.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun FreezeScreen(
    state: GuardState, device: DeviceData, ready: Boolean, busy: Boolean,
    add: (InstalledApp) -> Unit, select: (InstalledApp) -> Unit,
    remove: (InstalledApp) -> Unit, saveOptions: (Boolean, AccessGate) -> Unit,
    permissions: () -> Unit = {}, close: (InstalledApp) -> Unit = {}, freezeAll: () -> Unit = {},
    addMany: ((List<InstalledApp>) -> Unit)? = null,
    editRule: ((InstalledApp, Int, TimeWindow?) -> Unit)? = null, initialTarget: String? = null
) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var options by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf<InstalledApp?>(null) }
    var removeApp by remember { mutableStateOf<InstalledApp?>(null) }
    var scheduleApp by remember { mutableStateOf<InstalledApp?>(null) }
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    val context = LocalContext.current
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val selected = remember(state.rules, device.apps) { FreezeCatalog.selected(state, device.apps) }
    fun isOpen(app: InstalledApp): Boolean = state.rules[app.packageName]?.let {
        app.enabled && !app.suspended && (it.managedFreeze && it.gate == null || it.overrideUntil > System.currentTimeMillis() && it.sessionUntilElapsed > now)
    } == true
    val visible = selected.filter { (it.label.contains(query, true) || it.packageName.contains(query, true)) &&
        (filter == 0 || filter == 1 && (!it.enabled || it.suspended) || filter == 2 && isOpen(it)) }
    LaunchedEffect(state.rules) { while (state.rules.values.any { it.overrideUntil > 0 && it.sessionUntilElapsed > now }) { now = android.os.SystemClock.elapsedRealtime(); delay(1_000) } }
    LaunchedEffect(initialTarget, device.loading) {
        if (initialTarget != null && !device.loading) {
            val app = device.apps.find { it.packageName == initialTarget }
            if (app != null) { if (selected.any { it.packageName == initialTarget }) menu = app else picker = true }
        }
    }
    Column {
        PageHeader("Bekukan", null, action = {
            Row {
                if (selected.any(::isOpen)) IconButton(freezeAll, enabled = ready && !busy) { Icon(Icons.Outlined.AcUnit, "Bekukan semua sekarang") }
                IconButton({ picker = true }, enabled = !busy) { Icon(Icons.Outlined.Add, "Tambah aplikasi") }
                IconButton({ options = true }, enabled = !busy) { Icon(Icons.Outlined.MoreVert, "Pengaturan Bekukan") }
            }
        })
        if (!ready) Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            InfoBanner("Aktifkan Shizuku agar pembekuan dan pembukaan aplikasi berfungsi.", Icons.Outlined.Bolt, action = permissions)
        }
        if (selected.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.AcUnit, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp)); Text("Belum ada aplikasi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp)); Text("Ketuk + untuk menambahkan aplikasi yang ingin dibekukan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button({ picker = true }, enabled = !busy) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Tambah aplikasi") }
            }
        } else {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 20.dp), label = { Text("Cari di Bekukan") },
                singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(18.dp))
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Semua", "Dibekukan", "Terbuka").forEachIndexed { index, label -> FilterChip(filter == index, { filter = index }, label = { Text(label) }) }
            }
            if (visible.isEmpty()) Box(Modifier.weight(1f)) { EmptyState("Tidak ditemukan", "Coba kata pencarian atau filter lain.") }
            else LazyVerticalGrid(columns = GridCells.Adaptive(minSize = if (largeText) 260.dp else 96.dp), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.packageName }) { app ->
                    val rule = state.rules[app.packageName]
                    val status = when {
                        rule?.lastError != null -> "Perlu diperiksa"
                        isOpen(app) && rule?.gate == null -> "Terbuka"
                        isOpen(app) -> {
                            val seconds = ((rule!!.sessionUntilElapsed-now).coerceAtLeast(0)+999)/1000
                            "${seconds/60}:${(seconds%60).toString().padStart(2,'0')} tersisa"
                        }
                        rule?.gate?.automaticOnly == true && app.enabled -> "Batas & jadwal"
                        !app.enabled || app.suspended -> "Dibekukan"
                        else -> "Menunggu penerapan"
                    }
                    Column(Modifier.fillMaxWidth().combinedClickable(enabled = !busy,
                        onClickLabel = "Buka sementara ${app.label}", onLongClickLabel = "Kelola ${app.label}",
                        onClick = { select(app) }, onLongClick = { menu = app }), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppIcon(app, 52.dp)
                        Text(app.label, style = MaterialTheme.typography.labelMedium, maxLines = if (largeText) 5 else 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, overflow = TextOverflow.Ellipsis)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = if (rule?.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton({ menu = app }) { Icon(Icons.Outlined.MoreHoriz, "Kelola ${app.label}") }
                    }
                }
            }
        }
    }
    if (picker) {
        var search by rememberSaveable { mutableStateOf(initialTarget?.let { pkg -> device.apps.find { it.packageName == pkg }?.label }.orEmpty()) }
        var chosen by rememberSaveable { mutableStateOf(listOf<String>()) }
        val addable = remember(device.apps, state.rules, search) { device.apps.filter {
            FreezeCatalog.canAdd(it, state.rules[it.packageName]) && (it.label.contains(search,true) || it.packageName.contains(search,true))
        }.sortedBy { it.label.lowercase() } }
        ModalBottomSheet(onDismissRequest = { picker = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).heightIn(max = 620.dp)) {
                Text("Tambah aplikasi", style = MaterialTheme.typography.titleLarge)
                Text("Pilih satu atau beberapa aplikasi. Data aplikasi tetap disimpan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cari aplikasi") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(18.dp))
                if (!ready) Text("Hubungkan Shizuku sebelum menambahkan aplikasi.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = PaddingValues(vertical = 8.dp)) {
                    if (addable.isEmpty()) item { EmptyState("Tidak ditemukan", "Aplikasi penting dan aplikasi yang dikelola alat lain tidak dapat ditambahkan.") }
                    items(addable, key = { it.packageName }) { app ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = ready && !busy) {
                            if (addMany == null) { add(app); picker = false }
                            else chosen = if (app.packageName in chosen) chosen - app.packageName else chosen + app.packageName
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppIcon(app, 42.dp)
                            Column(Modifier.weight(1f)) { Text(app.label, style = MaterialTheme.typography.bodyLarge); Text(app.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            if (addMany != null) Checkbox(app.packageName in chosen, null) else Icon(Icons.Outlined.AddCircleOutline, "Tambahkan ${app.label}")
                        }
                    }
                }
                if (addMany != null) Button({ addMany(device.apps.filter { it.packageName in chosen }); picker = false }, Modifier.fillMaxWidth().padding(vertical = 12.dp), enabled = ready && !busy && chosen.isNotEmpty()) {
                    Text("Bekukan ${chosen.size} aplikasi")
                }
            }
        }
    }
    if (options) FreezeOptionsSheet(state.settings, { options = false }) { enabled, gate -> saveOptions(enabled,gate); options=false }
    menu?.let { app ->
        ModalBottomSheet(onDismissRequest = { menu = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon(app, 48.dp); Column(Modifier.weight(1f)) { Text(app.label, style = MaterialTheme.typography.titleLarge); Text(app.packageName, style = MaterialTheme.typography.bodySmall) }
                }
                state.rules[app.packageName]?.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button({ menu = null; select(app) }, Modifier.fillMaxWidth(), enabled = ready && !busy) { Text("Buka sementara") }
                if (isOpen(app)) OutlinedButton({ menu = null; close(app) }, Modifier.fillMaxWidth(), enabled = ready && !busy) { Text("Bekukan sekarang") }
                if (editRule != null) TextButton({ menu = null; scheduleApp = app }, Modifier.fillMaxWidth(), enabled = ready && !busy) { Text("Batas & jadwal otomatis") }
                TextButton({ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))) }, Modifier.fillMaxWidth()) { Text("Info aplikasi") }
                TextButton({ menu = null; removeApp = app }, Modifier.fillMaxWidth(), enabled = ready && !busy) { Text("Keluarkan dari Bekukan", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    scheduleApp?.let { app -> RuleEditor(app, state.rules[app.packageName] ?: AppRule(app.packageName), { scheduleApp = null }) { minutes, window ->
        editRule?.invoke(app, minutes, window); scheduleApp = null
    } }
    removeApp?.let { app -> AlertDialog(onDismissRequest = { removeApp = null }, title = { Text("Keluarkan ${app.label}?") },
        text = { Text("Aplikasi akan diaktifkan kembali; batas dan jadwalnya dilepas. Data aplikasi tidak dihapus. Blokir internet dikelola terpisah.") },
        confirmButton = { TextButton({ remove(app); removeApp = null }, enabled = ready && !busy) { Text("Keluarkan") } }, dismissButton = { TextButton({ removeApp = null }) { Text("Batal") } }) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable private fun FreezeOptionsSheet(settings: GuardSettings, dismiss: () -> Unit,
    save: (Boolean, AccessGate) -> Unit) {
    var enabled by rememberSaveable { mutableStateOf(settings.freezeChallengeEnabled) }
    var mode by rememberSaveable { mutableStateOf(settings.freezeGate.mode) }
    var wait by rememberSaveable { mutableIntStateOf(settings.freezeGate.waitSeconds) }
    var count by rememberSaveable { mutableIntStateOf(settings.freezeGate.questions) }
    var level by rememberSaveable { mutableStateOf(settings.freezeGate.level) }
    var minutes by rememberSaveable { mutableIntStateOf(settings.freezeGate.accessMinutes) }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { Text("Pengaturan Bekukan", style = MaterialTheme.typography.headlineSmall) }
            item { Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Perlindungan tambahan", style = MaterialTheme.typography.titleMedium)
                    Text("Secara default aplikasi langsung dibuka sementara tanpa tantangan.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(enabled, { enabled = it }, modifier = Modifier.semantics { contentDescription = "Perlindungan tambahan" })
            } }
            if (enabled) {
                item {
                    Text("Tantangan saat membuka", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(GateMode.WAIT to "Jeda", GateMode.MATH to "Matematika", GateMode.BOTH to "Keduanya").forEach { (value, label) ->
                            FilterChip(mode == value, { mode = value }, label = { Text(label) })
                        }
                    }
                }
                if (mode != GateMode.MATH) item { ChoiceGroup("Waktu menunggu", listOf(5, 15, 30, 60, 120), wait, { "$it detik" }) { wait = it } }
                if (mode != GateMode.WAIT) {
                    item {
                        Text("Tingkat soal", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(MathLevel.EASY to "Mudah", MathLevel.MEDIUM to "Sedang", MathLevel.HARD to "Sulit").forEach { (value, label) ->
                                FilterChip(level == value, { level = value }, label = { Text(label) })
                            }
                        }
                    }
                    item { ChoiceGroup("Jumlah soal", (1..5).toList(), count, { "$it soal" }) { count = it } }
                }
            }
            item { ChoiceGroup("Durasi akses sementara", listOf(1, 5, 10, 15, 30), minutes, { "$it menit" }) { minutes = it } }
            item { Button({
                save(enabled, AccessGate(mode, wait, count, level, minutes))
            }, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Simpan pengaturan")
            } }
        }
    }
}

fun gateLabel(gate: AccessGate): String = when (gate.mode) {
    GateMode.WAIT -> "Jeda ${gate.waitSeconds} dtk"
    GateMode.MATH -> "${gate.questions} soal matematika"
    GateMode.BOTH -> "Jeda ${gate.waitSeconds} dtk + ${gate.questions} soal"
} + " · akses ${gate.accessMinutes} mnt"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun GateEditor(app: InstalledApp, rule: AppRule, ready: Boolean, dismiss: () -> Unit,
    save: (AccessGate) -> Unit, remove: () -> Unit, close: () -> Unit, advanced: () -> Unit) {
    val current = rule.gate ?: AccessGate()
    var automatic by rememberSaveable(app.packageName) { mutableStateOf(current.automaticOnly) }
    var mode by rememberSaveable(app.packageName) { mutableStateOf(current.mode) }
    var wait by rememberSaveable(app.packageName) { mutableIntStateOf(current.waitSeconds) }
    var count by rememberSaveable(app.packageName) { mutableIntStateOf(current.questions) }
    var level by rememberSaveable(app.packageName) { mutableStateOf(current.level) }
    var access by rememberSaveable(app.packageName) { mutableIntStateOf(current.accessMinutes) }
    var confirmRemove by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        LazyColumn(Modifier.fillMaxWidth().imePadding(), contentPadding = PaddingValues(24.dp, 4.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(app, 48.dp)
                Column(Modifier.weight(1f)) { Text(app.label, style = MaterialTheme.typography.titleLarge); Text("Pilih jeda yang cocok untukmu", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } }
            if (rule.dailyLimitMinutes > 0 || rule.windows.isNotEmpty()) item {
                Text("Kapan dibekukan", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!automatic, { automatic = false }, label = { Text("Selalu") })
                    FilterChip(automatic, { automatic = true }, label = { Text("Sesuai batas & jadwal") })
                }
            }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(GateMode.WAIT to "Tunggu", GateMode.MATH to "Matematika", GateMode.BOTH to "Kombinasi").forEach { (value, label) ->
                    FilterChip(mode == value, { mode = value }, label = { Text(label) })
                }
            } }
            if (mode != GateMode.MATH) item { ChoiceGroup("Waktu tunggu", listOf(5, 15, 30, 60, 120), wait, { "$it detik" }) { wait = it } }
            if (mode != GateMode.WAIT) {
                item { Text("Tingkat soal", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(MathLevel.EASY to "Ringan", MathLevel.MEDIUM to "Sedang", MathLevel.HARD to "Menantang").forEach { (value, label) -> FilterChip(level == value, { level = value }, label = { Text(label) }) }
                    }
                }
                item { ChoiceGroup("Jumlah soal", (1..5).toList(), count, { "$it soal" }) { count = it } }
            }
            item { ChoiceGroup("Buka sementara selama", listOf(1, 5, 10, 15, 30), access, { "$it menit" }) { access = it } }
            item { InfoBanner(if (automatic) "Pembekuan mengikuti batas dan jadwal. Tantangan memberikan akses sementara." else "Ikon hilang saat dibekukan. Setelah tantangan, aplikasi aktif sementara dan dibekukan kembali. Shizuku harus tetap berjalan.", Icons.Outlined.AcUnit) }
            item { Button({ save(AccessGate(mode, wait, count, level, access, automatic)) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = ready, shape = RoundedCornerShape(16.dp)) {
                Text(if (rule.gate == null) "Bekukan aplikasi" else "Simpan tantangan")
            } }
            if (!ready) item { Text("Hubungkan Shizuku di Pengaturan sebelum menyimpan.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (rule.gate != null && app.enabled && !automatic) item { OutlinedButton(close, Modifier.fillMaxWidth(), enabled = ready) { Text("Bekukan sekarang") } }
            item { TextButton(advanced, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Schedule, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Batas & jadwal otomatis") } }
            if (rule.gate != null) item { TextButton({ confirmRemove = true }, Modifier.fillMaxWidth(), enabled = ready) { Text("Hapus perlindungan", color = MaterialTheme.colorScheme.error) } }
        }
    }
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, title = { Text("Hapus perlindungan ${app.label}?") }, text = { Text("Tantangan dan pembekuan ScreenGuard akan dilepas. Data aplikasi tetap disimpan.") },
        confirmButton = { TextButton(remove) { Text("Hapus perlindungan") } }, dismissButton = { TextButton({ confirmRemove = false }) { Text("Batal") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ChoiceGroup(title: String, choices: List<Int>, selected: Int, label: (Int) -> String, select: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (choices + selected).distinct().sorted().forEach { value -> FilterChip(selected == value, { select(value) }, label = { Text(label(value)) }) }
        }
    }
}
