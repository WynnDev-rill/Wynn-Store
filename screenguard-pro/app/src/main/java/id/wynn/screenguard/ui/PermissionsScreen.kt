package id.wynn.screenguard.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.privilege.ShizukuStatus

@Composable fun PermissionsScreen(usageAccess: Boolean, status: ShizukuStatus, back: (() -> Unit)?, requestShizuku: () -> Unit, done: () -> Unit, refresh: () -> Unit, connectionError: String? = null) {
    val context = LocalContext.current
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    fun open(intent: Intent) { runCatching { context.startActivity(intent) }.onFailure { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) } }
    Column {
        if (back != null) PageHeader("Akses & Perizinan", back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (back == null) { BrandMark(68.dp); Text("Ruang untuk lebih fokus.", style = MaterialTheme.typography.headlineLarge); Text("Selamat datang di ScreenGuard Pro. Atur kebiasaan digital dengan kendali yang sederhana.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            GuardCard {
                Text("1. Akses penggunaan", style = MaterialTheme.typography.titleMedium)
                Text("${if (usageAccess) "✓ Aktif. " else ""}Membaca durasi aplikasi untuk grafik dan batas harian. Riwayat tetap di perangkat.", style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton({ open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, Modifier.fillMaxWidth()) { Text(if (usageAccess) "Kelola akses penggunaan" else "Berikan akses penggunaan") }
            }
            GuardCard {
                Text("2. Hubungkan Shizuku", style = MaterialTheme.typography.titleMedium)
                Text(status.label, color = MaterialTheme.colorScheme.primary)
                if (connectionError != null) Text(connectionError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Instal Shizuku, aktifkan Opsi Pengembang → Debugging Nirkabel, lalu lakukan pairing dan mulai Shizuku. Setelah itu berikan izin ScreenGuard.", style = MaterialTheme.typography.bodyMedium)
                Button(requestShizuku, Modifier.fillMaxWidth(), enabled = status != ShizukuStatus.READY) { Text("Berikan izin Shizuku") }
                TextButton({ open(context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) }, Modifier.fillMaxWidth()) { Text("Buka / unduh Shizuku") }
                Text("Pada perangkat tanpa root, Shizuku perlu dimulai lagi setiap restart. ScreenGuard akan menampilkan statusnya dan memulihkan aturan saat tersambung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GuardCard {
                Text("3. Notifikasi & baterai", style = MaterialTheme.typography.titleMedium)
                Text("Pemantauan otomatis menampilkan notifikasi. Pada HyperOS, izinkan autostart dan pilih penghemat baterai ‘Tanpa batasan’ untuk ScreenGuard dan Shizuku.", style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton({ if (Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }, Modifier.fillMaxWidth()) { Text("Kelola notifikasi") }
                OutlinedButton({ open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Pengaturan baterai") }
                TextButton({ open(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))) }, Modifier.fillMaxWidth()) { Text("Autostart HyperOS / info aplikasi") }
            }
            InfoBanner("Sebelum menghapus ScreenGuard, gunakan ‘Pulihkan semua aplikasi’. Penonaktifan aplikasi dapat tetap berlaku meski ScreenGuard dihapus.", Icons.Outlined.Restore)
            Button(done, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) { Text(if (back == null) "Mulai ScreenGuard" else "Selesai") }
            TextButton(refresh, Modifier.fillMaxWidth()) { Text("Periksa status kembali") }
        }
    }
}
