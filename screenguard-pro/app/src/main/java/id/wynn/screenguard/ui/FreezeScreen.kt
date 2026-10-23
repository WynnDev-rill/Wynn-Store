package id.wynn.screenguard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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

@Composable fun FreezeScreen(state: GuardState, device: DeviceData, ready: Boolean, busy: Boolean, back: () -> Unit,
    edit: (InstalledApp) -> Unit, select: (InstalledApp) -> Unit, permissions: () -> Unit = {}) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var guarded by rememberSaveable { mutableStateOf(false) }
    val apps = remember(device.apps, query, filter, guarded, state.rules) {
        device.apps.filter { app ->
            (app.label.contains(query, true) || app.packageName.contains(query, true)) &&
                (filter == 0 || if (filter == 1) !app.system else app.system) &&
                (!guarded || state.rules[app.packageName]?.gate != null)
        }.sortedWith(compareBy<InstalledApp> { it.protected || !it.launchable }.thenBy { it.label.lowercase() })
    }
    Column {
        PageHeader("Bekukan", back)
        LazyColumn(contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Cari aplikasi") }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(20.dp)) }
            item { PillTabs(listOf("Semua", "Unduhan", "Sistem"), filter) { filter = it } }
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(guarded, { guarded = !guarded }, label = { Text("Dilindungi (${state.rules.values.count { it.gate != null }})") },
                    leadingIcon = { Icon(Icons.Outlined.Shield, null, Modifier.size(18.dp)) })
                Text("${apps.size} aplikasi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            if (!ready) item { InfoBanner("Hubungkan Shizuku untuk menerapkan pembekuan.", Icons.Outlined.Bolt, action = permissions) }
            if (apps.isEmpty()) item { EmptyState("Tidak ada aplikasi", "Coba kata pencarian atau filter lain.") }
            items(apps, key = { it.packageName }) { app ->
                val rule = state.rules[app.packageName]
                val safe = !app.protected && app.launchable
                val owned = rule?.ownedDisable == true || rule?.ownedSuspension == true
                val external = (!app.enabled || app.suspended) && !owned
                val closed = !app.enabled || app.suspended
                Surface(modifier = Modifier.animateItem(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))) {
                    Column {
                        Row(Modifier.fillMaxWidth().clickable(enabled = safe && !external && !busy, role = Role.Button,
                            onClickLabel = if (rule?.gate != null && closed) "Selesaikan tantangan" else "Atur perlindungan", onClick = { select(app) }).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppIcon(app)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(when {
                                    !safe -> "Komponen sistem • dilindungi"
                                    external -> "Dikunci oleh alat lain"
                                    rule?.lastError != null -> "Perlu diperiksa"
                                    closed && owned -> "Dibekukan • ikon disembunyikan"
                                    rule?.gate?.automaticOnly == true -> if (state.settings.monitorEnabled) "Batas & jadwal aktif" else "Aturan otomatis dijeda"
                                    rule?.gate != null && rule.overrideUntil > System.currentTimeMillis() -> "Akses sementara aktif"
                                    rule?.gate != null -> "Menunggu pembekuan"
                                    else -> "Ketuk untuk memilih tantangan"
                                }, style = MaterialTheme.typography.bodySmall, color = if (closed && owned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (safe && !external) Icon(if (closed && owned) Icons.Outlined.LockOpen else Icons.Outlined.ChevronRight, null,
                                Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        if (rule?.gate != null && safe && !external) {
                            HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(gateLabel(rule.gate), Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton({ edit(app) }, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Atur tantangan ${app.label}" }) {
                                    Icon(Icons.Outlined.Tune, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (rule?.lastError != null) Text(rule.lastError, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Text("Data aplikasi tetap disimpan. Komponen penting dan aplikasi tanpa peluncur ditampilkan hanya untuk informasi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
