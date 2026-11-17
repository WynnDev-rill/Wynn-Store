@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package id.wynn.arithmemo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.arithmemo.BuildConfig
import id.wynn.arithmemo.R
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.update.*
import java.util.Locale

@Composable
internal fun SettingsScreen(
    settings: UserSettings,
    change: (UserSettings) -> Unit,
    update: AppUpdater.State,
    automatic: Boolean,
    auto: (Boolean) -> Unit,
    check: () -> Unit,
    download: () -> Unit,
    install: () -> Unit,
    cancel: () -> Unit,
    notifications: () -> Unit,
    export: () -> Unit,
    import: () -> Unit,
) {
    val context = LocalContext.current
    var licenses by remember { mutableStateOf(false) }
    if (licenses)
        AlertDialog(
            onDismissRequest = { licenses = false },
            title = { Text("Lisensi sumber terbuka") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        context.resources
                            .openRawResource(R.raw.plus_jakarta_sans_license)
                            .bufferedReader()
                            .use { it.readText() } +
                            "\n\n" +
                            context.resources
                                .openRawResource(R.raw.apache_license)
                                .bufferedReader()
                                .use { it.readText() },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { licenses = false }) { Text("Tutup") } },
        )
    PageColumn {
        Heading("Nyaman dengan cara Anda", "Atur rutinitas kecil yang ingin Anda jaga.")
        Text(
            "Target harian",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text("${settings.goal} jawaban per hari", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 20, 30, 50, 100).forEach { goal ->
                FilterChip(
                    selected = settings.goal == goal,
                    onClick = { change(settings.copy(goal = goal)) },
                    label = { Text("$goal") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(
            "Pilihan latihan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Filters(settings, change)
        Text("Tampilan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "Sistem", "light" to "Terang", "dark" to "Gelap").forEach {
                (key, label) ->
                FilterChip(
                    selected = settings.theme == key,
                    onClick = { change(settings.copy(theme = key)) },
                    label = { Text(label) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        ToggleRow(
            "Getaran jawaban",
            "Umpan balik halus setelah menjawab",
            settings.haptics,
            { change(settings.copy(haptics = it)) },
        )
        HorizontalDivider()
        Text(
            "Cadangan data",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Data disimpan lokal. Ekspor cadangan sebelum pindah perangkat atau menghapus aplikasi.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = export, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Outlined.FileUpload, null)
            Spacer(Modifier.width(8.dp))
            Text("Ekspor cadangan")
        }
        OutlinedButton(onClick = import, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Outlined.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("Pulihkan cadangan")
        }
        HorizontalDivider()
        Text(
            "Pembaruan aplikasi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        ToggleRow(
            "Periksa otomatis",
            "Sekitar sekali sehari saat internet tersedia",
            automatic,
            auto,
        )
        OutlinedButton(
            onClick = notifications,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Icon(Icons.Outlined.NotificationsActive, null)
            Spacer(Modifier.width(8.dp))
            Text("Izinkan notifikasi update")
        }
        Text(
            update.message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color =
                if (update.error) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = check,
            enabled = !update.checking && update.progress == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(if (update.checking) "Memeriksa…" else "Periksa pembaruan")
        }
        update.available?.let { release ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Versi ${release.versionName}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (release.notes.isNotBlank()) Text(release.notes)
                    Text(
                        "${String.format(Locale.forLanguageTag("id"),"%.1f",release.sizeBytes/1048576.0)} MB · rilis resmi GitHub",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    update.progress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Mengunduh ${(progress*100).toInt()}%")
                        TextButton(onClick = cancel) { Text("Batalkan unduhan") }
                    }
                    if (update.progress == null)
                        Button(
                            onClick = if (update.ready) install else download,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Text(if (update.ready) "Pasang update" else "Unduh update")
                        }
                }
            }
        }
        HorizontalDivider()
        Text(
            "ArithMemo ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Hafalan aritmatika pribadi. Tanpa akun, iklan, atau analitik. Internet hanya untuk pemeriksaan dan unduhan update GitHub. Pemasangan selalu meminta persetujuan Anda di Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { licenses = true }) { Text("Lihat lisensi sumber terbuka") }
        Text(
            "Plus Jakarta Sans · SIL Open Font License\nAndroidX Compose & Material Icons · Apache 2.0\nKode aplikasi · MIT · Wynn Dev",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
