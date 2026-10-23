package id.wynn.screenguard.ui

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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun FreezeScreen(
    state: GuardState, device: DeviceData, ready: Boolean, busy: Boolean,
    add: (InstalledApp) -> Unit, select: (InstalledApp) -> Unit,
    remove: (InstalledApp) -> Unit, saveOptions: (Boolean, AccessGate) -> Unit,
    permissions: () -> Unit = {}
) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var options by rememberSaveable { mutableStateOf(false) }
    var removeApp by remember { mutableStateOf<InstalledApp?>(null) }
    val selected = remember(state.rules, device.apps) { FreezeCatalog.selected(state, device.apps) }
    Column {
        PageHeader("Bekukan", null, action = {
            Row {
                IconButton({ picker = true }, enabled = !busy) {
                    Icon(Icons.Outlined.Add, contentDescription = "Tambah aplikasi")
                }
                IconButton({ options = true }, enabled = !busy) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Pengaturan Bekukan")
                }
            }
        })
        if (!ready) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                InfoBanner("Aktifkan Shizuku agar pembekuan dan pembukaan aplikasi berfungsi.", Icons.Outlined.Bolt, action = permissions)
            }
        }
        if (selected.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.AcUnit, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Belum ada aplikasi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Ketuk + untuk menambahkan aplikasi yang ingin dibekukan.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button({ picker = true }, enabled = !busy) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah aplikasi")
                }
            }
        } else {
            Text("${selected.size} aplikasi · ketuk untuk buka sementara",
                Modifier.padding(horizontal = 22.dp, vertical = 10.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 80.dp), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selected, key = { it.packageName }) { app ->
                    val rule = state.rules[app.packageName]
                    Column(Modifier.fillMaxWidth().combinedClickable(
                        enabled = !busy,
                        onClickLabel = "Buka sementara ${app.label}",
                        onLongClickLabel = "Keluarkan ${app.label} dari Bekukan",
                        onClick = { select(app) },
                        onLongClick = { removeApp = app }
                    ), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box {
                            AppIcon(app, 56.dp)
                            if (rule?.lastError != null) {
                                Icon(Icons.Outlined.ErrorOutline, "Pembekuan perlu diperiksa",
                                    Modifier.align(Alignment.BottomEnd).size(20.dp), tint = MaterialTheme.colorScheme.error)
                            } else if (!app.enabled || app.suspended) {
                                Icon(Icons.Outlined.AcUnit, null, Modifier.align(Alignment.BottomEnd).size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(app.label, style = MaterialTheme.typography.labelMedium, maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Text("Tekan lama ikon untuk mengeluarkan aplikasi.",
                Modifier.fillMaxWidth().padding(bottom = 12.dp), style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (picker) {
        var query by rememberSaveable { mutableStateOf("") }
        val addable = remember(device.apps, state.rules, query) {
            device.apps.filter { FreezeCatalog.canAdd(it, state.rules[it.packageName]) &&
                (it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)) }
                .sortedBy { it.label.lowercase() }
        }
        ModalBottomSheet(onDismissRequest = { picker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp)) {
                Text("Tambah aplikasi", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Cari aplikasi") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    shape = RoundedCornerShape(18.dp))
                if (!ready) Text("Hubungkan Shizuku sebelum menambahkan aplikasi.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 12.dp))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (addable.isEmpty()) item { EmptyState("Tidak ditemukan", "Tidak ada aplikasi yang aman untuk ditambahkan.") }
                    items(addable, key = { it.packageName }) { app ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = ready && !busy) {
                            add(app); picker = false
                        }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            AppIcon(app, 42.dp)
                            Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                            Icon(Icons.Outlined.AddCircleOutline, "Tambahkan ${app.label}", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    if (options) {
        FreezeOptionsSheet(state.settings, { options = false }) { enabled, gate ->
            saveOptions(enabled, gate)
            options = false
        }
    }
    removeApp?.let { app ->
        AlertDialog(onDismissRequest = { removeApp = null },
            title = { Text("Keluarkan ${app.label}?") },
            text = { Text("Aplikasi akan diaktifkan kembali dan muncul di launcher. Data aplikasinya tidak dihapus.") },
            confirmButton = {
                TextButton({ remove(app); removeApp = null }, enabled = ready && !busy) {
                    Text("Keluarkan")
                }
            },
            dismissButton = { TextButton({ removeApp = null }) { Text("Batal") } })
    }
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
                Switch(enabled, { enabled = it })
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
