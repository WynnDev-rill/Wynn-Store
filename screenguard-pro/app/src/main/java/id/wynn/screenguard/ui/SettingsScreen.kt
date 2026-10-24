package id.wynn.screenguard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.BuildConfig
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuStatus

@Composable fun SettingsScreen(state: GuardState,status: ShizukuStatus,back: ()->Unit,secure: (()->Unit)->Unit,
    permissions: ()->Unit,change: ((GuardSettings)->GuardSettings)->Unit,pin: ()->Unit,removePin: ()->Unit,restore: ()->Unit,
    update: id.wynn.screenguard.update.AppUpdater.State = id.wynn.screenguard.update.AppUpdater.State(),
    autoUpdate: Boolean=true,checkUpdate: ()->Unit={},autoUpdateChange: (Boolean)->Unit={}) {
    var restorePrompt by remember {mutableStateOf(false)}
    var about by remember {mutableStateOf(false)}
    val context=LocalContext.current
    Column {
        PageHeader("Pengaturan",null)
        LazyColumn(contentPadding=PaddingValues(20.dp,8.dp,20.dp,24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {GuardCard {
                SettingsRow(Icons.Outlined.Bolt,"Shizuku",trailing=status.label,onClick=permissions)
                SettingsRow(Icons.Outlined.QueryStats,"Akses statistik",onClick=permissions)
                SettingsRow(Icons.Outlined.Palette,"Tema Tampilan",trailing=if(state.settings.darkMode) "Gelap" else "Terang",onClick={change {it.copy(darkMode=!it.darkMode)}})
            }}
            item {GuardCard {
                SettingsRow(Icons.Outlined.SystemUpdate,"Periksa update",update.message,if(update.checking) "Memeriksa…" else "v${BuildConfig.VERSION_NAME}",checkUpdate)
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Periksa saat dibuka",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                    Switch(autoUpdate,autoUpdateChange,Modifier.semantics {contentDescription="Periksa update otomatis"})
                }
                if(update.error) Text(update.message,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
            }}
            item {GuardCard {
                SettingsRow(Icons.Outlined.Restore,"Pulihkan semua aplikasi",onClick={restorePrompt=true})
                SettingsRow(Icons.Outlined.Info,"Tentang aplikasi",trailing="v${BuildConfig.VERSION_NAME}",onClick={about=true})
            }}
        }
    }
    if(restorePrompt) AlertDialog(onDismissRequest={restorePrompt=false},title={Text("Buka semua aplikasi?")},text={Text("Semua pembekuan milik ScreenGuard akan dilepas.")},confirmButton={TextButton({restorePrompt=false;restore()}) {Text("Buka semua")}},dismissButton={TextButton({restorePrompt=false}) {Text("Batal")}})
    if(about) AlertDialog(onDismissRequest={about=false},title={Text("ScreenGuard Pro")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Versi ${BuildConfig.VERSION_NAME}\nDibuat oleh Wynn")
        Text("Data aktivitas tersimpan di perangkat.",style=MaterialTheme.typography.bodyMedium)
        TextButton({context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/WynnDev-rill/Wynn-Store/tree/main/screenguard-pro")))}) {Text("Kode sumber & lisensi")}
    }},confirmButton={TextButton({about=false}) {Text("Tutup")}})
}
