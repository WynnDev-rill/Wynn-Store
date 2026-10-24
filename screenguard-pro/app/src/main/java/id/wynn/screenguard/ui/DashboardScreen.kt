package id.wynn.screenguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*

@Composable fun DashboardScreen(state: GuardState, device: DeviceData, connected: Boolean,
    navigate: (Page) -> Unit, selectApp: (InstalledApp) -> Unit, permissions: () -> Unit, selectDayApp: (InstalledApp, java.time.LocalDate) -> Unit = { app, _ -> selectApp(app) }, refresh: () -> Unit = {}) {
    var selectedDate by rememberSaveable { mutableStateOf(java.time.LocalDate.now().toString()) }
    val selected = device.days.find { it.date.toString() == selectedDate } ?: device.days.lastOrNull()
    val today = device.days.lastOrNull()
    val total = today?.totalMs ?: 0
    val yesterday = device.days.dropLast(1).lastOrNull()?.totalMs ?: 0
    val goal = state.settings.dailyGoalMinutes * 60_000L
    val byPackage = device.apps.associateBy { it.packageName }
    val top = selected?.apps.orEmpty().entries.sortedByDescending { it.value }.mapNotNull { (pkg, ms) -> byPackage[pkg]?.let { it to ms } }.take(5)
    val largeText = LocalDensity.current.fontScale >= 1.3f
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandMark(42.dp)
                Column(Modifier.weight(1f)) {
                    Text("ScreenGuard Pro", style = MaterialTheme.typography.titleLarge)
                }
                IconButton(refresh, enabled = !device.loading) { Icon(Icons.Outlined.Refresh, "Perbarui statistik") }
                IconButton(permissions) { Icon(Icons.Outlined.NotificationsNone, "Perizinan dan status") }
            }
        }
        if (!device.usageAccess || !connected) item {
            InfoBanner(if (!device.usageAccess) "Aktifkan akses penggunaan untuk melihat waktu layar." else "Shizuku belum terhubung. Kontrol aplikasi belum siap.", Icons.Outlined.Shield, action = permissions)
        }
        item {
            GuardCard(contentPadding = 16.dp, spacing = 12.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Total waktu layar hari ini", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (!device.usageAccess || today?.available != true) "—" else if (total == 0L) "0 mnt" else duration(total), style = MaterialTheme.typography.headlineLarge)
                        val delta = total - yesterday
                        if (today?.available == true && device.days.dropLast(1).lastOrNull()?.available == true && yesterday > 0L) Text("${if (delta <= 0) "↓" else "↑"} ${duration(kotlin.math.abs(delta))} dari kemarin",
                            style = MaterialTheme.typography.bodySmall, color = if (delta <= 0) Success else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Spa, null, Modifier.size(40.dp), tint = Success)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target harian: ${duration(goal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (device.usageAccess) "${if (goal > 0) total * 100 / goal else 0}%" else "—", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(progress = { (if (goal > 0) total.toFloat() / goal else 0f).coerceIn(0f, 1f) },
                    Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer, drawStopIndicator = {})
            }
        }
        item {
            GuardCard(contentPadding = 16.dp, spacing = 12.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text("Tren Mingguan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.padding(10.dp, 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rata-rata", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(if (device.days.none { it.available }) "—" else duration(device.days.takeLast(7).filter { it.available }.let { days -> days.sumOf { it.totalMs } / days.size.coerceAtLeast(1) }), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(if (!device.usageAccess) "Akses penggunaan diperlukan" else selected?.let { "${dateLabel(it.date)} · ${if (it.available) duration(it.totalMs) else "Data tidak tersedia"}" } ?: "Memuat riwayat…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (device.usageAccess) {
                    WeekChart(device.days.takeLast(7), chartHeight = 96.dp, selectedDate = selected?.date, onSelect = { selectedDate = it.date.toString() })
                    if (selected?.estimated == true) Text("Data sebagian", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            GuardCard(contentPadding = 16.dp, spacing = 12.dp) {
                SectionTitle(if (selected == null || selected.date == java.time.LocalDate.now()) "Paling banyak digunakan" else "Penggunaan ${selected?.date?.let(::dateLabel).orEmpty()}", "Semua") { navigate(Page.USAGE) }
                if (top.isEmpty()) Text(if (selected?.available == false) "Riwayat rinci hari ini tidak lagi tersedia dari Android." else "Belum ada penggunaan pada hari yang dipilih.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Column { top.forEach { (app, ms) -> UsageRow(app, ms, selected?.appTotalMs ?: 0, compact = true) { selectDayApp(app, selected?.date ?: java.time.LocalDate.now()) } } }
            }
        }
        item {
            SectionTitle("Aksi Cepat")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (largeText) {
                    QuickAction("Bekukan Aplikasi", Icons.Outlined.AcUnit, Modifier.fillMaxWidth()) { navigate(Page.FREEZE) }
                    QuickAction("Blokir Internet", Icons.Outlined.Wifi, Modifier.fillMaxWidth()) { navigate(Page.NETWORK) }
                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction("Bekukan Aplikasi", Icons.Outlined.AcUnit, Modifier.weight(1f)) { navigate(Page.FREEZE) }
                    QuickAction("Blokir Internet", Icons.Outlined.Wifi, Modifier.weight(1f)) { navigate(Page.NETWORK) }
                }
            }
        }
    }
}
@Composable private fun QuickAction(title: String, icon: ImageVector, modifier: Modifier, action: () -> Unit) {
    Surface(onClick = action, modifier = modifier.heightIn(min = 54.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
