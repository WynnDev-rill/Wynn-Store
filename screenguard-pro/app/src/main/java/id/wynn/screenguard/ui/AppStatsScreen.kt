package id.wynn.screenguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.data.*
import id.wynn.screenguard.domain.*
import java.time.LocalDate

@Composable fun AppStatsScreen(app: InstalledApp, device: DeviceData, back: () -> Unit,
    loadDetails: suspend (String, LocalDate) -> AppDayDetail, protect: () -> Unit, initialDate: String? = null) {
    var selectedDate by rememberSaveable(app.packageName) { mutableStateOf(initialDate ?: LocalDate.now().toString()) }
    val selected = device.days.find { it.date.toString() == selectedDate } ?: device.days.lastOrNull()
    val date = selected?.date ?: LocalDate.now()
    val week = device.days.takeLast(7).map { day -> day.copy(apps = mapOf(app.packageName to day.apps.getOrDefault(app.packageName, 0)), screenMs = day.apps.getOrDefault(app.packageName, 0)) }
    val total = selected?.apps?.get(app.packageName) ?: 0
    var detail by remember(app.packageName, date) { mutableStateOf<AppDayDetail?>(null) }
    var error by remember(app.packageName, date) { mutableStateOf<String?>(null) }
    LaunchedEffect(app.packageName, date, device.days) {
        try { detail = loadDetails(app.packageName, date) }
        catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; error = "Rincian belum tersedia. Buka kembali halaman ini untuk mencoba lagi." }
    }
    Column {
        PageHeader("Statistik aplikasi", back)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppIcon(app, 56.dp)
                Column(Modifier.weight(1f)) { Text(app.label, style = MaterialTheme.typography.headlineSmall); Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } }
            item { GuardCard(spacing = 16.dp) {
                Text("${dateLabel(date)} · waktu layar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (device.usageAccess && selected?.available == true) duration(total) else "—", style = MaterialTheme.typography.headlineLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ selectedDate = date.minusDays(1).toString() }, enabled = device.days.any { it.date == date.minusDays(1) }) { Text("Hari sebelumnya") }
                    TextButton({ selectedDate = date.plusDays(1).toString() }, enabled = date.isBefore(LocalDate.now())) { Text("Berikutnya") }
                }
                WeekChart(week, chartHeight = 100.dp, selectedDate = date, onSelect = { selectedDate = it.date.toString() })
                Text("Rata-rata 7 hari: ${duration(week.filter { it.available }.let { days -> days.sumOf { it.totalMs } / days.size.coerceAtLeast(1) })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            item { GuardCard {
                SectionTitle("Penggunaan per jam")
                when {
                    !device.usageAccess -> Text("Aktifkan akses penggunaan di Pengaturan untuk membaca statistik perangkat.", style = MaterialTheme.typography.bodyMedium)
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    detail == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    detail?.eventsAvailable != true -> Text("Android tidak lagi menyediakan kejadian rinci untuk hari ini. Total harian tetap ditampilkan; jumlah pembukaan dan grafik per jam tidak diperkirakan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {
                        HourChart(detail!!.hourly)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StatLine("Sesi di latar depan", "${detail!!.opens} kali")
                        StatLine("Sesi terlama", duration(detail!!.longestMs))
                    }
                }
            } }
            item { GuardCard {
                StatLine("Bagian penggunaan aplikasi", "${if ((selected?.appTotalMs ?: 0) > 0) total * 100 / selected!!.appTotalMs else 0}%")
                StatLine("Total 7 hari", duration(week.sumOf { it.totalMs }))
            } }
            if (!app.protected && app.launchable) item { FilledTonalButton(protect, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Outlined.AcUnit, null); Spacer(Modifier.width(8.dp)); Text("Bekukan aplikasi")
            } }
            item { Text(if (selected?.available != true) "Riwayat hari ini tidak tersedia. Android menyimpan kejadian rinci untuk waktu terbatas; hari kosong tidak dihitung sebagai nol." else if (selected.estimated) "Riwayat hari ini belum lengkap. Rincian yang masih tersedia ditampilkan tanpa menebak bagian yang hilang." else "Durasi dihitung dari sesi aplikasi di latar depan. Sesi berbeda dari jumlah ketukan ikon. Aktivitas bersamaan dalam aplikasi digabung; total layar dihitung terpisah.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun StatLine(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
    }
}

@Composable private fun HourChart(hours: List<Long>) {
    val color = MaterialTheme.colorScheme.primary
    val max = hours.maxOrNull()?.coerceAtLeast(60_000) ?: 60_000
    Column {
        Canvas(Modifier.fillMaxWidth().height(112.dp).semantics {
            contentDescription = "Penggunaan per jam: " + hours.mapIndexedNotNull { i, ms -> if (ms > 0) "$i:00 ${duration(ms)}" else null }.joinToString().ifBlank { "belum ada penggunaan" }
        }) {
            repeat(3) { y -> drawLine(color.copy(alpha = .1f), Offset(0f, size.height * y / 2), Offset(size.width, size.height * y / 2), 1.dp.toPx()) }
            hours.forEachIndexed { index, ms ->
                val slot = size.width / 24
                val height = (ms.toFloat() / max * size.height).coerceAtLeast(1.dp.toPx())
                drawRoundRect(color.copy(alpha = if (ms > 0) .8f else .15f), Offset(slot * index + slot * .2f, size.height - height), Size(slot * .6f, height), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
