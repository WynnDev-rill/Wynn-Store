package id.wynn.screenguard.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuStatus

enum class Page { HOME, USAGE, NETWORK, FREEZE, UNLOCK, SCHEDULE, SETTINGS, PERMISSIONS }

@Composable fun ScreenGuardApp(vm: GuardViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val device by vm.device.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val supported by vm.firewall.collectAsStateWithLifecycle()
    val connectionError by vm.graph.gateway.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val pinPrompt by vm.pinPrompt.collectAsStateWithLifecycle()
    val pinError by vm.pinError.collectAsStateWithLifecycle()
    val pinWorking by vm.pinWorking.collectAsStateWithLifecycle()
    val challenge by vm.challenge.collectAsStateWithLifecycle()
    val challengeError by vm.challengeError.collectAsStateWithLifecycle()
    val remaining by vm.remaining.collectAsStateWithLifecycle()
    val unlocking by vm.unlocking.collectAsStateWithLifecycle()
    var pageName by rememberSaveable { mutableStateOf(Page.HOME.name) }
    val page = Page.valueOf(pageName)
    var detail by remember { mutableStateOf<InstalledApp?>(null) }
    var editor by remember { mutableStateOf<InstalledApp?>(null) }
    var picker by remember { mutableStateOf(false) }
    var createPin by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun back() { pageName = if (page == Page.UNLOCK) Page.FREEZE.name else Page.HOME.name }
    fun navigate(next: Page) { if (next == Page.SETTINGS || next == Page.SCHEDULE) vm.secure { pageName = next.name } else pageName = next.name }
    fun permissions() { pageName = Page.PERMISSIONS.name }
    BackHandler(page != Page.HOME && state.settings.onboardingDone && !busy) { back() }
    GuardTheme(state.settings.darkMode) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
            if (page == Page.HOME && state.settings.onboardingDone) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                listOf(Triple(Page.HOME, "Beranda", Icons.Outlined.Home), Triple(Page.USAGE, "Aplikasi", Icons.Outlined.GridView),
                    Triple(Page.SCHEDULE, "Fokus", Icons.Outlined.TrackChanges), Triple(Page.SETTINGS, "Pengaturan", Icons.Outlined.Settings)).forEach { (target, label, icon) ->
                    NavigationBarItem(page == target, { navigate(target) }, icon = { Icon(icon, null) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer))
                }
            }
        }) { insets ->
            Box(Modifier.fillMaxSize().padding(insets).imePadding(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                    if (!state.settings.onboardingDone) PermissionsScreen(device.usageAccess, status, null, vm.graph.gateway::requestPermission, vm::onboarded, vm::refresh, connectionError)
                    else AnimatedContent(page, label = "page", transitionSpec = { fadeIn() togetherWith fadeOut() }) { current ->
                        when (current) {
                            Page.HOME -> DashboardScreen(state, device, status == ShizukuStatus.READY, ::navigate, { detail = it }, ::permissions)
                            Page.USAGE -> UsageScreen(device, ::back) { detail = it }
                            Page.NETWORK -> NetworkScreen(state, device, status, supported, busy, ::back, vm::setNetwork, ::permissions) { navigate(Page.FREEZE) }
                            Page.FREEZE -> FreezeScreen(state, device, status == ShizukuStatus.READY, busy, ::back, vm::freeze) { app -> vm.prepareUnlock(app) { pageName = Page.UNLOCK.name } }
                            Page.UNLOCK -> UnlockScreen(device.apps.find { it.packageName == unlocking }, state.settings, challenge, remaining, challengeError, busy, ::back, vm::newChallenge) { answer -> vm.unlock(answer) { pkg ->
                                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                if (intent == null) vm.message.value = "Aplikasi dipulihkan, tetapi tidak menyediakan ikon peluncur."
                                else runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { vm.message.value = it.message ?: "Tidak dapat membuka aplikasi" }
                            } }
                            Page.SCHEDULE -> ScheduleScreen(state, device.apps, ::back, { app -> vm.secure { editor = app } }, { vm.secure { picker = true } }, vm::monitor, vm::focus)
                            Page.SETTINGS -> SettingsScreen(state, status, ::back, vm::secure, ::permissions, vm::settings, { vm.secure { createPin = true } }, vm::removePin, vm::restore)
                            Page.PERMISSIONS -> PermissionsScreen(device.usageAccess, status, ::back, vm.graph.gateway::requestPermission, ::back, vm::refresh, connectionError)
                        }
                    }
                }
                if (device.loading && state.settings.onboardingDone) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
        detail?.let { app -> AlertDialog(onDismissRequest = { detail = null }, title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { AppIcon(app); Text(app.label) } }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Hari ini: ${device.days.lastOrNull()?.apps?.get(app.packageName)?.let(::duration) ?: "0 mnt"}")
                Text(vm.reason(app), color = MaterialTheme.colorScheme.primary)
                Button({ vm.secure { editor = app; detail = null } }, Modifier.fillMaxWidth(), enabled = !app.protected) { Text("Atur batas & jadwal") }
                FilledTonalButton({ detail = null; navigate(Page.NETWORK) }, Modifier.fillMaxWidth()) { Text("Kontrol internet") }
                FilledTonalButton({ detail = null; navigate(Page.FREEZE) }, Modifier.fillMaxWidth()) { Text("Bekukan / buka aplikasi") }
                if (app.protected) Text("Aplikasi penting dilindungi dari perubahan.", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton({ detail = null }) { Text("Tutup") } }) }
        if (picker) {
            var search by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { picker = false }, title = { Text("Pilih aplikasi") }, text = {
                Column {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Cari aplikasi") }, singleLine = true)
                    LazyColumn(Modifier.heightIn(max = 360.dp)) { items(device.apps.filter { !it.protected && it.label.contains(search, true) }, key = { it.packageName }) { app -> TextButton({ editor = app; picker = false }, Modifier.fillMaxWidth()) { AppIcon(app, 30.dp); Spacer(Modifier.width(12.dp)); Text(app.label, Modifier.weight(1f)) } } }
                }
            }, confirmButton = { TextButton({ picker = false }) { Text("Batal") } })
        }
        editor?.let { app -> RuleEditor(app, state.rules[app.packageName] ?: AppRule(app.packageName), { editor = null }) { limit, window, focus -> vm.saveRule(app, limit, window, focus) } }
        if (createPin) PinCreateDialog({ createPin = false }, vm::savePin)
        if (pinPrompt) {
            var input by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = vm::cancelPin, title = { Text("PIN Orang Tua") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Masukkan PIN untuk melanjutkan perubahan ini.")
                OutlinedTextField(input, { input = it.filter(Char::isDigit).take(6) }, label = { Text("PIN 6 digit") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, isError = pinError != null)
                if (pinError != null) Text(pinError!!, color = MaterialTheme.colorScheme.error)
            } }, confirmButton = { TextButton({ vm.verifyPin(input); input = "" }, enabled = !pinWorking && input.length == 6) { Text(if (pinWorking) "Memeriksa…" else "Verifikasi") } }, dismissButton = { TextButton(vm::cancelPin) { Text("Batal") } })
        }
        if (message != null && !pinPrompt) AlertDialog(onDismissRequest = { vm.message.value = null }, title = { Text("ScreenGuard Pro") }, text = { Text(message!!) }, confirmButton = { TextButton({ vm.message.value = null }) { Text("Mengerti") } })
        if (busy) Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp); Text("Menerapkan perubahan…") }
            }
        }
    }
}
