package id.wynn.screenguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*

@Composable fun DashboardScreen(state: GuardState, device: DeviceData, connected: Boolean,
    navigate: (Page) -> Unit, selectApp: (InstalledApp) -> Unit, permissions: () -> Unit,
    focusAction: (() -> Unit)? = null, scheduleAction: (() -> Unit)? = null) {
    val today = device.days.lastOrNull()
    val total = today?.totalMs ?: 0
    val yesterday = device.days.dropLast(1).lastOrNull()?.totalMs ?: 0
    val goal = state.settings.dailyGoalMinutes * 60_000L
    val byPackage = device.apps.associateBy { it.packageName }
    val top = today?.apps.orEmpty().entries.sortedByDescending { it.value }.mapNotNull { (pkg, ms) -> byPackage[pkg]?.let { it to ms } }.take(5)
    val largeText = LocalDensity.current.fontScale >= 1.3f
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrandMark(42.dp)
                Column(Modifier.weight(1f)) {
                    Text("ScreenGuard Pro", style = MaterialTheme.typography.titleLarge)
                    Text("Fokus hari ini, lebih baik esok hari.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(permissions) { Icon(Icons.Outlined.NotificationsNone, "Perizinan dan status") }
            }
        }
        if (!device.usageAccess || !connected) item {
            InfoBanner(if (!device.usageAccess) "Aktifkan akses penggunaan untuk melihat waktu layar." else "Shizuku belum terhubung. Kontrol aplikasi belum siap.", Icons.Outlined.Shield, action = permissions)
        }
        item {
            GuardCard(contentPadding = 12.dp, spacing = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Total waktu layar hari ini", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (!device.usageAccess) "—" else if (total == 0L) "0 mnt" else duration(total), style = MaterialTheme.typography.headlineLarge)
                        val delta = total - yesterday
                        Text(if (yesterday == 0L) "Mulai kenali kebiasaan digitalmu" else "${if (delta <= 0) "↓" else "↑"} ${duration(kotlin.math.abs(delta))} dari kemarin",
                            style = MaterialTheme.typography.bodySmall, color = if (delta <= 0) Success else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Spa, null, Modifier.size(40.dp), tint = Success)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target harian: ${duration(goal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${if (goal > 0) total * 100 / goal else 0}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(progress = { (if (goal > 0) total.toFloat() / goal else 0f).coerceIn(0f, 1f) },
                    Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer, drawStopIndicator = {})
            }
        }
        item {
            GuardCard(contentPadding = 12.dp, spacing = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text("Tren Mingguan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.padding(10.dp, 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rata-rata", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(if (device.days.isEmpty()) "—" else duration(device.days.takeLast(7).sumOf { it.totalMs } / 7), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                WeekChart(device.days.takeLast(7), chartHeight = 68.dp)
            }
        }
        item {
            GuardCard(contentPadding = 12.dp, spacing = 8.dp) {
                SectionTitle("Paling banyak digunakan", "Semua") { navigate(Page.USAGE) }
                if (top.isEmpty()) Text("Belum ada data penggunaan hari ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Column { top.forEach { (app, ms) -> UsageRow(app, ms, total, compact = true) { selectApp(app) } } }
            }
        }
        item {
            SectionTitle("Aksi Cepat")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (largeText) {
                    QuickAction("Blokir Internet", Icons.Outlined.Lock, Modifier.fillMaxWidth()) { navigate(Page.NETWORK) }
                    QuickAction("Mode Fokus", Icons.Outlined.TrackChanges, Modifier.fillMaxWidth()) { focusAction?.invoke() ?: navigate(Page.SCHEDULE) }
                    QuickAction("Jadwal", Icons.Outlined.CalendarMonth, Modifier.fillMaxWidth()) { scheduleAction?.invoke() ?: navigate(Page.SCHEDULE) }
                    QuickAction("Bekukan Aplikasi", Icons.Outlined.AcUnit, Modifier.fillMaxWidth()) { navigate(Page.FREEZE) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction("Blokir Internet", Icons.Outlined.Lock, Modifier.weight(1f)) { navigate(Page.NETWORK) }
                        QuickAction("Mode Fokus", Icons.Outlined.TrackChanges, Modifier.weight(1f)) { focusAction?.invoke() ?: navigate(Page.SCHEDULE) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction("Jadwal", Icons.Outlined.CalendarMonth, Modifier.weight(1f)) { scheduleAction?.invoke() ?: navigate(Page.SCHEDULE) }
                        QuickAction("Bekukan Aplikasi", Icons.Outlined.AcUnit, Modifier.weight(1f)) { navigate(Page.FREEZE) }
                    }
                }
            }
        }
    }
}
@Composable private fun QuickAction(title: String, icon: ImageVector, modifier: Modifier, action: () -> Unit) {
    Surface(onClick = action, modifier = modifier.heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
