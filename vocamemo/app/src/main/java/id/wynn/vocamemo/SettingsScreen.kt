package id.wynn.vocamemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsScreen(vm: VocaModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var licenses by remember { mutableStateOf(false) }
    var backupBusy by remember { mutableStateOf(false) }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                backupBusy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val text = vm.exportBackup()
                            context.contentResolver.openOutputStream(uri, "wt")?.use {
                                it.write(text.toByteArray())
                            } ?: error("Tidak dapat menulis cadangan")
                        }
                        vm.report("Cadangan disimpan.")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        vm.report(
                            e.message
                                ?: "Gagal menyimpan cadangan. Pilih lokasi lain dan coba lagi."
                        )
                    } finally {
                        backupBusy = false
                    }
                }
            }
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                backupBusy = true
                scope.launch {
                    try {
                        val text =
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    input.readLimited(BACKUP_LIMIT).toString(Charsets.UTF_8)
                                } ?: error("Berkas tidak dapat dibaca")
                            }
                        vm.imported(text)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        vm.report(
                            "Cadangan gagal dibaca. Pilih JSON yang valid (maksimal 50 MB)."
                        )
                    } finally {
                        backupBusy = false
                    }
                }
            }
        }
    val notification =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.report(
                if (granted) "Notifikasi update diaktifkan"
                else "Notifikasi belum diizinkan. Update tetap dapat diperiksa di sini."
            )
        }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            SettingsGroup("Tampilan") {
                Box(Modifier.fillMaxWidth().glass(MaterialTheme.shapes.large).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    FlowRowCompat {
                        listOf(
                            "Sistem" to Icons.Rounded.BrightnessAuto,
                            "Terang" to Icons.Rounded.LightMode,
                            "Gelap" to Icons.Rounded.DarkMode,
                        ).forEach { (theme, icon) ->
                            FilterChip(
                                selected = vm.theme == theme,
                                onClick = { vm.theme(theme) },
                                label = { Text(theme) },
                                leadingIcon = { Icon(icon, null, Modifier.size(17.dp)) },
                            )
                        }
                    }
                }
            }
        }
        item {
            SettingsGroup("Belajar") {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column {
                        SettingRow("Target harian", "${vm.goal} jawaban", Icons.Rounded.Flag) {
                            editGoal = true
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingRow("Suara TTS", "Pengaturan suara Android", Icons.AutoMirrored.Rounded.VolumeUp) {
                            runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                                .onFailure { vm.report("Buka Pengaturan Android → Bahasa & masukan → Text-to-speech.") }
                        }
                    }
                }
            }
        }
        item {
            SettingsGroup("Cadangan") {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column {
                        SettingRow("Ekspor cadangan", "Memo dan riwayat latihan", Icons.Rounded.SaveAlt, !backupBusy) {
                            export.launch("VocaMemo-${LocalDate.now()}.json")
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingRow("Pulihkan cadangan", "Gabungkan dari berkas JSON", Icons.Rounded.Restore, !backupBusy) {
                            import.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        }
                        if (backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            SettingsGroup("Pembaruan") {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column {
                        SettingRow("Periksa pembaruan", vm.updateStatus, Icons.Rounded.SystemUpdate, !vm.updateBusy) {
                            vm.checkUpdates()
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        SettingRow("Notifikasi pembaruan", null, Icons.Rounded.NotificationsNone) {
                            val permission = Manifest.permission.POST_NOTIFICATIONS
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notification.launch(permission)
                            } else {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        }
                        if (vm.updateBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        vm.release?.let { release ->
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(release.notes, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = vm::installUpdate, enabled = !vm.updateBusy) {
                                    Text("Unduh dan pasang")
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingsGroup("Tentang") {
                val count = remember(vm.count) {
                    NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(vm.count)
                }
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    SettingRow(
                        "Sumber dan lisensi",
                        if (vm.count > 0) "$count kata offline · WikDict" else "WikDict",
                        Icons.Rounded.Info,
                    ) { licenses = true }
                }
                Text(
                    "VocaMemo ${BuildConfig.VERSION_NAME} · Wynn Dev",
                    Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Memo dan riwayat tersimpan di perangkat.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (editGoal) {
        GoalDialog(
            goal = vm.goal,
            dismiss = { editGoal = false },
            save = {
                vm.goal(it)
                vm.report("Target harian diperbarui")
                editGoal = false
            },
        )
    }
    if (licenses) {
        var text by remember { mutableStateOf("Memuat…") }
        LaunchedEffect(Unit) {
            text =
                withContext(Dispatchers.IO) {
                    context.assets.open("ATTRIBUTION.txt").bufferedReader().use { it.readText() }
                }
        }
        AlertDialog(
            onDismissRequest = { licenses = false },
            title = { Text("Sumber & lisensi") },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text,
                        Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { licenses = false }) { Text("Tutup") } },
        )
    }
}

internal fun java.io.InputStream.readLimited(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val b = ByteArray(8192)
    while (true) {
        val n = read(b)
        if (n < 0) break
        require(out.size() + n <= limit) { "Berkas terlalu besar" }
        out.write(b, 0, n)
    }
    return out.toByteArray()
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        content()
    }
}

@Composable
private fun SettingRow(
    title: String,
    detail: String?,
    icon: ImageVector,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = detail?.let { text ->
            {
                Text(
                    text,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.onSurfaceVariant,
                )
            }
        },
        leadingContent = { Icon(icon, null, tint = colors.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = action),
    )
}

@Composable
private fun GoalDialog(goal: Int, dismiss: () -> Unit, save: (Int) -> Unit) {
    var input by rememberSaveable { mutableStateOf(goal.toString()) }
    val valid = input.toIntOrNull()?.takeIf { it in 1..500 }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Target harian") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit).take(3) },
                label = { Text("Jawaban per hari") },
                supportingText = { Text("1–500 jawaban") },
                isError = input.isNotEmpty() && valid == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Batal") } },
        confirmButton = {
            TextButton(enabled = valid != null, onClick = { valid?.let(save) }) { Text("Simpan target") }
        },
    )
}
