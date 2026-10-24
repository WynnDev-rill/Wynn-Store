package id.wynn.screenguard.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.privilege.ShizukuStatus

@Composable fun PermissionsScreen(usageAccess: Boolean,status: ShizukuStatus,back: (()->Unit)?,requestShizuku: ()->Unit,done: ()->Unit,refresh: ()->Unit,
    connectionError: String?=null,grantUsage: (()->Unit)?=null,busy: Boolean=false) {
    val context=LocalContext.current
    var help by remember {mutableStateOf(false)}
    fun open(intent: Intent) {runCatching {context.startActivity(intent)}.onFailure {context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}}
    Column {
        PageHeader(if(back==null) "ScreenGuard Pro" else "Akses aplikasi",back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            GuardCard {
                Text("Shizuku",style=MaterialTheme.typography.titleMedium)
                Text(status.label,style=MaterialTheme.typography.bodyMedium)
                if(connectionError!=null) Text(connectionError,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                Button(requestShizuku,Modifier.fillMaxWidth(),enabled=status!=ShizukuStatus.READY&&!busy) {Text("Hubungkan Shizuku")}
                TextButton({open(context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api") ?: Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/download/")))},Modifier.fillMaxWidth()) {Text("Buka Shizuku")}
            }
            GuardCard {
                Text("Statistik aktivitas",style=MaterialTheme.typography.titleMedium)
                Text(if(usageAccess) "Akses aktif" else "Opsional • akses penggunaan",style=MaterialTheme.typography.bodyMedium)
                FilledTonalButton({open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,Uri.parse("package:${context.packageName}")))},Modifier.fillMaxWidth()) {Text("Kelola akses statistik")}
                if(!usageAccess&&status==ShizukuStatus.READY&&grantUsage!=null) TextButton(grantUsage,Modifier.fillMaxWidth(),enabled=!busy) {Text("Aktifkan melalui Shizuku")}
                TextButton({help=true},Modifier.fillMaxWidth()) {Text("Bantuan izin")}
            }
            Button(done,Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(if(back==null) "Mulai ScreenGuard" else "Selesai")}
            TextButton(refresh,Modifier.fillMaxWidth()) {Text("Periksa status")}
        }
    }
    if(help) AlertDialog(onDismissRequest={help=false},title={Text("Akses statistik")},text={Text("Jika Android menolak akses, buka Info aplikasi → ⋮ → Izinkan setelan terbatas. Lalu aktifkan akses penggunaan.\n\nShizuku perlu dimulai kembali setelah perangkat restart.")},confirmButton={TextButton({open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}) {Text("Info aplikasi")}},dismissButton={TextButton({help=false}) {Text("Tutup")}})
}
