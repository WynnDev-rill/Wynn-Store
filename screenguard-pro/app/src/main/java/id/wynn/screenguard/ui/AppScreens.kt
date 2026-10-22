package id.wynn.screenguard.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuStatus

@Composable fun UsageScreen(device: DeviceData, back: () -> Unit, selected: (InstalledApp) -> Unit) {
    var range by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val days = device.days.takeLast(listOf(1, 7, 30)[range])
    val usage = days.flatMap { it.apps.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }
    val total = usage.values.sum()
    val apps = device.apps.filter { it.label.contains(query, true) }.filter {
        when (category) { 1 -> it.category == ApplicationInfo.CATEGORY_SOCIAL; 2 -> it.category == ApplicationInfo.CATEGORY_VIDEO; 3 -> it.category == ApplicationInfo.CATEGORY_GAME; else -> true }
    }.sortedByDescending { usage[it.packageName] ?: 0 }
    Column {
        PageHeader("Penggunaan Aplikasi", back)
        LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { PillTabs(listOf("Hari ini", "7 hari", "30 hari"), range) { range = it } }
            item { GuardCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Total waktu layar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (!device.usageAccess) "—" else if (total == 0L) "0 mnt" else duration(total), style = MaterialTheme.typography.headlineLarge)
                        Text(if (range == 0) "Penggunaan hari ini" else "${days.size} hari terakhir", style = MaterialTheme.typography.bodySmall, color = Green)
                    }
                    WeekChart(days.takeLast(7), Modifier.weight(1f), labels = false)
                }
            } }
            if (days.any { it.estimated }) item { Text("Riwayat lama memakai agregat Android dan dapat berupa perkiraan. Data harian tersimpan lokal hingga 90 hari.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Cari aplikasi") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(listOf("Semua", "Sosial", "Video", "Game")) { i, name -> FilterChip(category == i, { category = i }, label = { Text(name) }) }
            } }
            if (!device.usageAccess) item { InfoBanner("Buka Pengaturan → Akses penggunaan untuk memberikan izin statistik.") }
            if (apps.isEmpty()) item { EmptyState("Tidak ada aplikasi", "Coba kategori atau kata pencarian lain.") }
            items(apps, key = { it.packageName }) { app ->
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.padding(horizontal = 12.dp)) { UsageRow(app, usage[app.packageName] ?: 0, total) { selected(app) } }
                }
            }
        }
    }
}

@Composable fun NetworkScreen(state: GuardState, device: DeviceData, status: ShizukuStatus, supported: Boolean, busy: Boolean,
    back: () -> Unit, toggle: (InstalledApp, Boolean) -> Unit, permissions: () -> Unit, fallback: (() -> Unit)? = null) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val apps = device.apps.filter { !it.protected && it.label.contains(query, true) && (tab == 0 || state.rules[it.packageName]?.networkBlocked == true) }
    Column {
        PageHeader("Kontrol Internet", back)
        LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Blokir akses internet untuk aplikasi tertentu.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { InfoBanner("Saat diblokir, seluruh koneksi Wi-Fi dan data seluler aplikasi dihentikan. Kontrol terpisah tidak disediakan API Android ini.", Icons.Outlined.Wifi) }
            if (status != ShizukuStatus.READY || !supported) item {
                InfoBanner(if (status != ShizukuStatus.READY) status.label else "API firewall tidak tersedia pada Android perangkat ini.", Icons.Outlined.Shield, action = permissions)
            }
            if (status == ShizukuStatus.READY && !supported && fallback != null) item {
                InfoBanner("Alternatif: bekukan aplikasi untuk menghentikan aplikasi beserta koneksinya.", Icons.Outlined.AcUnit, action = fallback)
            }
            item { PillTabs(listOf("Semua Aplikasi", "Diblokir (${state.rules.values.count { it.networkBlocked }})"), tab) { tab = it } }
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Cari aplikasi") }, singleLine = true, shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Outlined.Search, null) }) }
            if (apps.isEmpty()) item { EmptyState("Belum ada aplikasi", "Aplikasi yang sesuai akan ditampilkan di sini.") }
            items(apps, key = { it.packageName }) { app ->
                val rule = state.rules[app.packageName]
                val blocked = rule?.appliedNetwork == true
                GuardCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppIcon(app)
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.titleMedium)
                            Text(if (app.sharedUid) "UID bersama • kontrol dilindungi" else if (rule?.pendingNetwork == true) "Perubahan belum diterapkan" else if (blocked) "Internet diblokir" else "Internet diizinkan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Outlined.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Blokir Wi-Fi & data seluler", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(blocked, { toggle(app, it) }, enabled = status == ShizukuStatus.READY && supported && !busy && !app.sharedUid,
                            modifier = Modifier.semantics { contentDescription = "Blokir internet ${app.label}" })
                    }
                    if (rule?.lastError != null) Text(rule.lastError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    if (blocked && status != ShizukuStatus.READY) Text("Aturan tersimpan; status sistem belum terverifikasi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable fun FreezeScreen(state: GuardState, device: DeviceData, ready: Boolean, busy: Boolean, back: () -> Unit,
    freeze: (InstalledApp, FreezeMode) -> Unit, unlock: (InstalledApp) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var modeFor by remember { mutableStateOf<InstalledApp?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val lockedCount = state.rules.values.count { it.freeze != FreezeMode.NONE || it.ownedDisable || it.ownedSuspension }
    val apps = device.apps.filter { !it.protected && it.label.contains(query, true) && (tab == 0 || state.rules[it.packageName]?.let { r -> r.freeze != FreezeMode.NONE || r.ownedDisable || r.ownedSuspension } == true) }
    Column {
        PageHeader("Bekukan Aplikasi", back)
        LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Tangguhkan atau nonaktifkan aplikasi sementara.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { PillTabs(listOf("Terpasang", "Dibekukan ($lockedCount)"), tab) { tab = it } }
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Cari aplikasi") }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
            if (!ready) item { InfoBanner("Hubungkan Shizuku untuk membekukan dan memulihkan aplikasi.", Icons.Outlined.Shield) }
            item { InfoBanner("Buka aplikasi yang dibekukan melalui halaman ini untuk menjalani tantangan. Android tidak mengizinkan kami mengganti layar peluncur aplikasi lain.", Icons.Outlined.LockOpen) }
            if (apps.isEmpty()) item { EmptyState("Tidak ada aplikasi", "Daftar mengikuti filter yang dipilih.") }
            items(apps, key = { it.packageName }) { app ->
                val rule = state.rules[app.packageName]
                val locked = rule?.let { it.freeze != FreezeMode.NONE || it.ownedDisable || it.ownedSuspension } == true
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppIcon(app, 36.dp)
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.titleMedium)
                                Text(when { rule?.lastError != null -> "Perubahan belum berhasil"; rule?.ownedDisable == true && !app.enabled -> "Dinonaktifkan"; rule?.ownedSuspension == true && app.suspended -> "Ditangguhkan"; locked -> "Menunggu penerapan aturan"; !app.enabled || app.suspended -> "Dikunci alat lain"; else -> "Siap digunakan" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalButton(onClick = { if (locked) unlock(app) else modeFor = app }, enabled = ready && !busy && (locked || app.enabled && !app.suspended), contentPadding = PaddingValues(10.dp, 0.dp), shape = RoundedCornerShape(12.dp)) {
                                Icon(if (locked) Icons.Outlined.LockOpen else Icons.Outlined.AcUnit, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp)); Text(if (locked) "Buka" else "Bekukan")
                            }
                        }
                        if (rule?.lastError != null) Text(rule.lastError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    modeFor?.let { app -> AlertDialog(onDismissRequest = { modeFor = null }, title = { Text("Bekukan ${app.label}") },
        text = { Text("Tangguhkan: ikon tetap ada, aplikasi tidak bisa dibuka. Nonaktifkan: aplikasi juga dihilangkan dari peluncur. Data aplikasi tetap disimpan.") },
        confirmButton = { TextButton({ freeze(app, FreezeMode.SUSPEND); modeFor = null }) { Text("Tangguhkan") } },
        dismissButton = { TextButton({ freeze(app, FreezeMode.DISABLE); modeFor = null }) { Text("Nonaktifkan") } }) }
}
