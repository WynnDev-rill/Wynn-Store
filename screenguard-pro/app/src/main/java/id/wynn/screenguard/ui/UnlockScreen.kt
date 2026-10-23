package id.wynn.screenguard.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.domain.*

@Composable fun UnlockScreen(app: InstalledApp?, gate: AccessGate, challenge: MathChallenge, completed: Int, remaining: Int,
    error: String?, busy: Boolean, back: () -> Unit, change: () -> Unit, unlock: (String) -> Unit) {
    var answer by rememberSaveable(challenge) { mutableStateOf("") }
    val waiting = gate.hasWait && remaining > 0
    val math = !waiting && gate.hasMath && completed < gate.questions
    Column {
        PageHeader("Buka aplikasi", back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.height(8.dp))
            if (app != null) AppIcon(app, 72.dp) else BrandMark(72.dp)
            Text(app?.label ?: "Aplikasi", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(if (waiting) "Ambil jeda sebentar sebelum membuka." else if (math) "Selesaikan soal ${completed + 1} dari ${gate.questions}." else "Jeda selesai. Siap melanjutkan?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            GuardCard(spacing = 16.dp) {
                Text(if (waiting) "Tarik napas, nikmati jedanya" else if (math) "Berapa hasil dari" else "Tantangan selesai", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedContent(if (waiting) "$remaining detik" else if (math) challenge.question else "Siap dibuka", label = "challenge", transitionSpec = { fadeIn() togetherWith fadeOut() }) { label ->
                    Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                }
                if (math) OutlinedTextField(answer, { answer = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("Masukkan jawaban") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), isError = error != null)
                LinearProgressIndicator(progress = {
                    val waitSteps = if (gate.hasWait) 1 else 0
                    val mathSteps = if (gate.hasMath) gate.questions else 0
                    val waited = if (gate.hasWait) 1 - remaining.toFloat().coerceAtMost(gate.waitSeconds.toFloat()) / gate.waitSeconds else 0f
                    ((waited + completed) / (waitSteps + mathSteps).coerceAtLeast(1)).coerceIn(0f, 1f)
                }, modifier = Modifier.fillMaxWidth(), drawStopIndicator = {})
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button({ unlock(answer) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !busy && app != null && !waiting, shape = RoundedCornerShape(16.dp)) {
                Text(if (math && completed + 1 < gate.questions) "Periksa jawaban" else "Buka ${gate.accessMinutes} menit")
            }
            if (math) TextButton({ change(); answer = "" }) { Icon(Icons.Outlined.Refresh, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Ganti soal") }
            Text("Aplikasi akan dibekukan kembali setelah ${gate.accessMinutes} menit. Timer tetap berjalan saat ScreenGuard ditutup, selama Shizuku aktif. Blokir internet mengikuti pengaturannya sendiri.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
