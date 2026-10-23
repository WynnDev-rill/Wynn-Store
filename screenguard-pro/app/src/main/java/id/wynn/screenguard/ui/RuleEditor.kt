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

@Composable fun RuleEditor(app: InstalledApp, rule: AppRule, dismiss: () -> Unit, save: (Int, TimeWindow?) -> Unit) {
    var minutes by rememberSaveable(app.packageName) { mutableStateOf(rule.dailyLimitMinutes.takeIf { it > 0 }?.toString() ?: "") }
    var scheduled by rememberSaveable(app.packageName) { mutableStateOf(rule.windows.isNotEmpty()) }
    var start by rememberSaveable(app.packageName) { mutableIntStateOf(rule.windows.firstOrNull()?.startMinute ?: 1320) }
    var end by rememberSaveable(app.packageName) { mutableIntStateOf(rule.windows.firstOrNull()?.endMinute ?: 360) }
    var days by remember(app.packageName) { mutableStateOf(rule.windows.firstOrNull()?.days ?: (1..7).toSet()) }
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
            Text("Pembekuan otomatis menyembunyikan ikon saat batas atau jadwal berlaku. Saat dibuka lewat tantangan, akses sementara akan diberikan. Pemeriksaan ±15 detik; penghemat baterai dapat menundanya.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton({
        val parsed = minutes.ifBlank { "0" }.toIntOrNull()
        error = when { parsed == null || parsed !in 0..1440 -> "Isi batas 0–1440 menit"; scheduled && start == end -> "Jam mulai dan selesai harus berbeda"; scheduled && days.isEmpty() -> "Pilih minimal satu hari"; else -> null }
        if (error == null) { save(parsed!!, if (scheduled) TimeWindow("Jadwal ${app.label}", start, end, days) else null); dismiss() }
    }) { Text("Simpan aturan") } }, dismissButton = { TextButton(dismiss) { Text("Batal") } })
}
