package id.wynn.screenguard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
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

@Composable fun UnlockScreen(app: InstalledApp?, settings: GuardSettings, challenge: MathChallenge, remaining: Int,
    error: String?, busy: Boolean, back: () -> Unit, change: () -> Unit, unlock: (String) -> Unit) {
    var answer by rememberSaveable(challenge) { mutableStateOf("") }
    Column {
        PageHeader("Buka Aplikasi", back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(22.dp))
            if (app != null) AppIcon(app, 72.dp) else BrandMark(72.dp)
            Text("${app?.label ?: "Aplikasi"} terkunci", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("Ambil jeda sebelum kembali membuka aplikasi.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            GuardCard {
                Text(if (settings.unlockMode == UnlockMode.MATH) "Berapa hasil dari" else "Tarik napas. Tunggu sebentar.", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedContent(if (settings.unlockMode == UnlockMode.MATH) challenge.question else "$remaining detik", label = "challenge", transitionSpec = { fadeIn() togetherWith fadeOut() }) { label ->
                    Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                }
                if (settings.unlockMode == UnlockMode.MATH) OutlinedTextField(answer, { answer = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("Masukkan jawaban") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), isError = error != null)
                else LinearProgressIndicator(progress = { 1 - remaining.toFloat() / settings.waitSeconds.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button({ unlock(answer) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !busy && app != null && (settings.unlockMode == UnlockMode.MATH || remaining == 0), shape = RoundedCornerShape(12.dp)) { Text("Buka Aplikasi") }
            if (settings.unlockMode == UnlockMode.MATH) TextButton({ change(); answer = "" }) { Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Ganti soal") }
            Text("Pembekuan manual dilepas. Batas dan jadwal mendapat jeda 5 menit sebelum berlaku kembali. Blokir internet mengikuti pengaturannya sendiri.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
