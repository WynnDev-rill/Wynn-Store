package id.wynn.screenguard.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.BuildConfig
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuStatus

@Composable fun SettingsScreen(state: GuardState, status: ShizukuStatus, back: () -> Unit, secure: (() -> Unit) -> Unit,
    permissions: () -> Unit, change: ((GuardSettings) -> GuardSettings) -> Unit, pin: () -> Unit, removePin: () -> Unit, restore: () -> Unit,
    update: id.wynn.screenguard.update.AppUpdater.State = id.wynn.screenguard.update.AppUpdater.State(),
    autoUpdate: Boolean = true, checkUpdate: () -> Unit = {}, autoUpdateChange: (Boolean) -> Unit = {}) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var goal by remember { mutableStateOf(state.settings.dailyGoalMinutes.toString()) }
    val context = LocalContext.current
    Column {
        PageHeader("Pengaturan", back)
        LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SectionTitle("Keamanan") }
            item { GuardCard {
                SettingsRow(Icons.Outlined.Lock, "PIN Orang Tua", "Lindungi perubahan dan pembukaan aplikasi", if (state.settings.hasPin) "Aktif" else "Atur", pin)
                SettingsRow(Icons.Outlined.Password, "Ubah PIN", "6 digit, disimpan sebagai hash", onClick = pin)
                if (state.settings.hasPin) SettingsRow(Icons.Outlined.LockOpen, "Hapus PIN", onClick = { secure { dialog = "removePin" } })
                SettingsRow(Icons.Outlined.Calculate, "Tantangan membuka", trailing = if (state.settings.unlockMode == UnlockMode.MATH) "Matematika" else "${state.settings.waitSeconds} detik", onClick = { secure { dialog = "unlock" } })
            } }
            item { SectionTitle("Akses & Integrasi") }
            item { GuardCard {
                SettingsRow(Icons.Outlined.Bolt, "Status Shizuku", status.label, if (status == ShizukuStatus.READY) "Terhubung" else "Periksa", permissions)
                SettingsRow(Icons.Outlined.MenuBook, "Panduan Shizuku & HyperOS", "Izin, restart, dan penghematan baterai", onClick = permissions)
                SettingsRow(Icons.Outlined.Restore, "Pulihkan semua aplikasi", "Lepas blokir dan kunci milik ScreenGuard", onClick = { secure { dialog = "restore" } })
            } }
            item { SectionTitle("Pembaruan aplikasi") }
            item { GuardCard {
                SettingsRow(Icons.Outlined.SystemUpdate, "Periksa update", update.message,
                    if (update.checking) "Memeriksa…" else "v${BuildConfig.VERSION_NAME}", onClick = checkUpdate)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Periksa saat dibuka", style = MaterialTheme.typography.titleMedium)
                        Text("Menghubungi GitHub untuk metadata versi. Data penggunaan tetap lokal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(autoUpdate, autoUpdateChange, modifier = Modifier.semantics { contentDescription = "Periksa update otomatis" })
                }
                if (update.error) Text(update.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } }
            item { SectionTitle("Umum") }
            item { GuardCard {
                SettingsRow(Icons.Outlined.Palette, "Tema Tampilan", trailing = if (state.settings.darkMode) "Gelap" else "Terang", onClick = { change { it.copy(darkMode = !it.darkMode) } })
                SettingsRow(Icons.Outlined.NotificationsNone, "Notifikasi", "Kelola notifikasi pemantauan", onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) })
                SettingsRow(Icons.Outlined.Flag, "Target waktu layar", trailing = duration(state.settings.dailyGoalMinutes * 60_000L), onClick = { secure { goal = state.settings.dailyGoalMinutes.toString(); dialog = "goal" } })
                SettingsRow(Icons.Outlined.Info, "Tentang Aplikasi", trailing = "v${BuildConfig.VERSION_NAME}", onClick = { dialog = "about" })
            } }
            item { Text("Bahasa Indonesia · Data hanya tersimpan di perangkat\nDibuat oleh Wynn", Modifier.fillMaxWidth().padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    when (dialog) {
        "removePin" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Hapus PIN orang tua?") }, text = { Text("Perubahan aturan tidak lagi memerlukan PIN.") }, confirmButton = { TextButton({ removePin(); dialog = null }) { Text("Hapus PIN") } }, dismissButton = { TextButton({ dialog = null }) { Text("Batal") } })
        "restore" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Pulihkan semua aplikasi?") }, text = { Text("Semua batas, jadwal, fokus, dan blokir internet ScreenGuard akan dilepas. Lakukan sebelum menghapus ScreenGuard.") }, confirmButton = { TextButton({ restore(); dialog = null }) { Text("Pulihkan") } }, dismissButton = { TextButton({ dialog = null }) { Text("Batal") } })
        "unlock" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Tantangan membuka") }, text = { Column {
            Text("Berlaku untuk pembukaan sementara selama 5 menit.")
            TextButton({ change { it.copy(unlockMode = UnlockMode.MATH) }; dialog = null }) { Text("Tantangan matematika") }
            listOf(30, 60, 120).forEach { seconds -> TextButton({ change { it.copy(unlockMode = UnlockMode.WAIT, waitSeconds = seconds) }; dialog = null }) { Text("Tunggu $seconds detik") } }
        } }, confirmButton = { TextButton({ dialog = null }) { Text("Tutup") } })
        "goal" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Target harian") }, text = { OutlinedTextField(goal, { goal = it.filter(Char::isDigit).take(4) }, label = { Text("Menit (1–1440)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }, confirmButton = { TextButton({ goal.toIntOrNull()?.takeIf { it in 1..1440 }?.let { minutes -> change { it.copy(dailyGoalMinutes = minutes) }; dialog = null } }, enabled = goal.toIntOrNull() in 1..1440) { Text("Simpan") } }, dismissButton = { TextButton({ dialog = null }) { Text("Batal") } })
        "about" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text("ScreenGuard Pro") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("v${BuildConfig.VERSION_NAME} · Android 11+\nAplikasi native Kotlin dan Jetpack Compose. Tanpa akun, server pribadi, analitik, iklan, atau VPN. Izin internet hanya dipakai untuk memeriksa dan mengunduh update resmi dari GitHub.")
            Text("Kode sumber: GPL-3.0. Referensi: ShizuWall (GPL-3.0) dan Hail (GPL-3.0). Dependensi AndroidX, Kotlin, dan Shizuku API: Apache-2.0.")
            Text("PIN melindungi pengaturan ScreenGuard. Pemilik perangkat masih dapat mencabut izin, menghentikan aplikasi, atau menghapusnya melalui Android.", style = MaterialTheme.typography.bodySmall)
            TextButton({ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/WynnDev-rill/Wynn-Store/tree/main/screenguard-pro"))) }) { Text("Kode sumber & lisensi") }
        } }, confirmButton = { TextButton({ dialog = null }) { Text("Tutup") } })
    }
}

@Composable fun PinCreateDialog(dismiss: () -> Unit, save: (String, String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Atur PIN orang tua") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Simpan PIN dengan aman. Tidak ada pemulihan melalui server atau email.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("PIN baru (6 digit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true)
        OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(6) }, label = { Text("Ulangi PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = error != null)
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
    } }, confirmButton = { TextButton({ error = when { !PinCrypto.valid(pin) -> "Gunakan 6 angka"; pin != confirm -> "Konfirmasi PIN berbeda"; else -> null }; if (error == null) { save(pin, confirm); dismiss() } }) { Text("Simpan PIN") } }, dismissButton = { TextButton(dismiss) { Text("Batal") } })
}
