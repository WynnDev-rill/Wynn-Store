package id.wynn.screenguard.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*

@Composable fun ScheduleScreen(state: GuardState, apps: List<InstalledApp>, back: () -> Unit, edit: (InstalledApp) -> Unit,
    pick: () -> Unit, monitor: (Boolean) -> Unit, focus: (Int) -> Unit, initialTab: Int = 0) {
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    val listed = apps.filter { app -> state.rules[app.packageName]?.let { r ->
        when (tab) { 0 -> r.dailyLimitMinutes > 0; 1 -> r.windows.isNotEmpty(); else -> r.focus }
    } == true }
    Column {
        PageHeader("Jadwal & Otomatisasi", back)
        LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Atur batas penggunaan dan jadwal sesuai kebutuhanmu.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { PillTabs(listOf("Batas Aplikasi", "Jadwal", "Mode Fokus"), tab) { tab = it } }
            item { GuardCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pemantauan otomatis", style = MaterialTheme.typography.titleMedium)
                        Text(if (state.settings.monitorEnabled) "Diaktifkan • periksa status pada notifikasi" else "Aktifkan agar aturan diterapkan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(state.settings.monitorEnabled, monitor)
                }
            } }
            if (listed.isEmpty()) item { EmptyState("Belum ada aturan", "Tambahkan aplikasi untuk mengatur kebiasaanmu.") }
            items(listed, key = { it.packageName }) { app ->
                val rule = state.rules.getValue(app.packageName)
                GuardCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppIcon(app, 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.titleMedium)
                            Text(when (tab) { 0 -> "Batas harian"; 1 -> rule.windows.firstOrNull()?.name ?: "Jadwal"; else -> "Dikunci selama sesi fokus" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton({ edit(app) }) { Text(when (tab) { 0 -> duration(rule.dailyLimitMinutes * 60_000L); 1 -> rule.windows.firstOrNull()?.let { "${timeLabel(it.startMinute)}–${timeLabel(it.endMinute)}" } ?: "Atur"; else -> "Atur" }) }
                    }
                    if (rule.overrideUntil > System.currentTimeMillis()) Text("Jeda buka sementara aktif", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (rule.lastError != null) Text(rule.lastError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            item { FilledTonalButton(pick, Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Tambah Aturan Aplikasi") } }
            if (tab == 1) {
                item {
                    GuardCard {
                        Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Periode Kunci Harian", style = MaterialTheme.typography.titleMedium)
                        Text("Jadwal mendukung lintas tengah malam dan pilihan hari. Pada jadwal 22:00–06:00, hari yang dipilih adalah hari mulainya jadwal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (tab == 2) {
                item {
                    GuardCard {
                        Text("Sisihkan waktu untuk fokus", style = MaterialTheme.typography.titleMedium)
                        Text(if (state.settings.focusUntil > System.currentTimeMillis()) "Sesi berakhir pukul ${java.time.Instant.ofEpochMilli(state.settings.focusUntil).atZone(java.time.ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0)}" else "Pilih aplikasi di atas, lalu mulai sesi.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(25, 50).forEach { m -> FilledTonalButton({ focus(m) }, Modifier.weight(1f)) { Text("$m menit") } } }
                        if (state.settings.focusUntil > System.currentTimeMillis()) OutlinedButton({ focus(0) }, Modifier.fillMaxWidth()) { Text("Akhiri sesi fokus") }
                    }
                }
            }
            item { Text("Pemantauan memeriksa aturan setiap ±15 detik saat layar aktif. Mode hemat baterai/Doze dapat menunda jadwal; lihat panduan HyperOS di Pengaturan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable fun RuleEditor(app: InstalledApp, rule: AppRule, dismiss: () -> Unit, save: (Int, TimeWindow?, Boolean) -> Unit) {
    var minutes by rememberSaveable(app.packageName) { mutableStateOf(rule.dailyLimitMinutes.takeIf { it > 0 }?.toString() ?: "") }
    var scheduled by rememberSaveable(app.packageName) { mutableStateOf(rule.windows.isNotEmpty()) }
    var start by rememberSaveable(app.packageName) { mutableIntStateOf(rule.windows.firstOrNull()?.startMinute ?: 1320) }
    var end by rememberSaveable(app.packageName) { mutableIntStateOf(rule.windows.firstOrNull()?.endMinute ?: 360) }
    var days by remember(app.packageName) { mutableStateOf(rule.windows.firstOrNull()?.days ?: (1..7).toSet()) }
    var focused by rememberSaveable(app.packageName) { mutableStateOf(rule.focus) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    AlertDialog(onDismissRequest = dismiss, title = { Text("Atur ${app.label}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(4) }, label = { Text("Batas harian (menit)") }, supportingText = { Text("Kosong atau 0 untuk tanpa batas; maksimal 1440.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Jadwal kunci", Modifier.weight(1f)); Switch(scheduled, { scheduled = it }) }
            if (scheduled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ TimePickerDialog(context, { _, h, m -> start = h * 60 + m }, start / 60, start % 60, true).show() }, Modifier.weight(1f)) { Text(timeLabel(start)) }
                    OutlinedButton({ TimePickerDialog(context, { _, h, m -> end = h * 60 + m }, end / 60, end % 60, true).show() }, Modifier.weight(1f)) { Text(timeLabel(end)) }
                }
                Text("Hari mulai", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEachIndexed { i, name -> FilterChip(i + 1 in days, { days = if (i + 1 in days) days - (i + 1) else days + (i + 1) }, label = { Text(name) }) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Sertakan dalam Mode Fokus", Modifier.weight(1f)); Switch(focused, { focused = it }) }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton({
        val parsed = minutes.ifBlank { "0" }.toIntOrNull()
        error = when { parsed == null || parsed !in 0..1440 -> "Isi batas 0–1440 menit"; scheduled && start == end -> "Jam mulai dan selesai harus berbeda"; scheduled && days.isEmpty() -> "Pilih minimal satu hari"; else -> null }
        if (error == null) { save(parsed!!, if (scheduled) TimeWindow("Jadwal ${app.label}", start, end, days) else null, focused); dismiss() }
    }) { Text("Simpan aturan") } }, dismissButton = { TextButton(dismiss) { Text("Batal") } })
}
