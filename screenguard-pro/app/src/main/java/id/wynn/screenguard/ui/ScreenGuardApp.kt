package id.wynn.screenguard.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.core.view.WindowCompat
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuStatus

enum class Page { HOME, FREEZE, NETWORK, SETTINGS, USAGE, APP_STATS, UNLOCK, PERMISSIONS }

@Composable fun ScreenGuardApp(vm: GuardViewModel) {
    val update by vm.updater.state.collectAsStateWithLifecycle()
    val autoUpdate by vm.updater.automatic.collectAsStateWithLifecycle()
    var updateSheet by rememberSaveable { mutableStateOf(false) }
    var offeredVersion by rememberSaveable { mutableLongStateOf(0) }
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
    val gate by vm.challengeGate.collectAsStateWithLifecycle()
    val completed by vm.completedQuestions.collectAsStateWithLifecycle()
    val challengeError by vm.challengeError.collectAsStateWithLifecycle()
    val remaining by vm.remaining.collectAsStateWithLifecycle()
    val unlocking by vm.unlocking.collectAsStateWithLifecycle()
    var pageName by rememberSaveable { mutableStateOf(Page.HOME.name) }
    val page = runCatching { Page.valueOf(pageName) }.getOrDefault(Page.HOME)
    var previousPage by rememberSaveable { mutableStateOf(Page.HOME.name) }
    val pageStateHolder = rememberSaveableStateHolder()
    var removalPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var detailPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var detailDate by rememberSaveable { mutableStateOf(java.time.LocalDate.now().toString()) }
    var detailReturn by rememberSaveable { mutableStateOf(Page.HOME.name) }
    var createPin by remember { mutableStateOf(false) }
    LaunchedEffect(update.available?.versionCode, state.settings.onboardingDone) {
        val version = update.available?.versionCode ?: return@LaunchedEffect
        if (state.settings.onboardingDone && version != offeredVersion) { offeredVersion = version; updateSheet = true }
    }
    LaunchedEffect(page, unlocking) { if (page == Page.UNLOCK && unlocking == null) pageName = Page.FREEZE.name }
    LaunchedEffect(state.rules) { vm.refresh() }
    val context = LocalContext.current
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !state.settings.darkMode
                isAppearanceLightNavigationBars = !state.settings.darkMode
            }
        }
    }
    fun back() {
        if (page == Page.UNLOCK) { vm.cancelChallenge(); removalPackage = null }
        pageName = when (page) { Page.UNLOCK -> Page.FREEZE.name; Page.APP_STATS -> detailReturn; Page.PERMISSIONS -> previousPage; else -> Page.HOME.name }
    }
    fun navigate(next: Page) { detailPackage = null; if (next == Page.SETTINGS) vm.secure { pageName = next.name } else pageName = next.name }
    fun permissions() { previousPage = pageName; pageName = Page.PERMISSIONS.name }
    fun details(app: InstalledApp) { detailDate = java.time.LocalDate.now().toString(); detailPackage = app.packageName; detailReturn = pageName; pageName = Page.APP_STATS.name }
    fun detailsAt(app: InstalledApp, date: java.time.LocalDate) { details(app); detailDate = date.toString() }
    fun launchApp(pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) vm.message.value = "Aplikasi aktif, tetapi tidak menyediakan peluncur Android."
        else runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { vm.message.value = "Tidak dapat membuka aplikasi: ${it.message}" }
    }
    fun select(app: InstalledApp) {
        val rule = state.rules[app.packageName] ?: return
        if (rule.gate == null) {
            vm.message.value = "Aplikasi ini sedang menunggu pemulihan. Tekan lama ikonnya dan pilih Keluarkan saat Shizuku siap."
            return
        }
        if (app.enabled && !app.suspended && (vm.sessionOpen(rule) || rule.gate.automaticOnly && vm.canLaunchWithoutChallenge(app))) launchApp(app.packageName)
        else if (status != ShizukuStatus.READY) permissions()
        else if (state.settings.freezeChallengeEnabled || rule.gate.automaticOnly) vm.prepareUnlock(app) { pageName = Page.UNLOCK.name }
        else vm.openTemporarily(app, ::launchApp)
    }
    BackHandler(page != Page.HOME && state.settings.onboardingDone && !busy) { back() }
    GuardTheme(state.settings.darkMode) {
        Scaffold(modifier = Modifier.imePadding(), containerColor = MaterialTheme.colorScheme.background, bottomBar = {
            if (page != Page.UNLOCK && page != Page.PERMISSIONS && state.settings.onboardingDone)
                FloatingDock(page, ::navigate)
        }) { insets ->
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                    if (!state.settings.onboardingDone) PermissionsScreen(device.usageAccess, status, null, vm.graph.gateway::requestPermission, vm::onboarded, vm::refresh, connectionError, vm::grantUsageThroughShizuku, busy)
                    else AnimatedContent(page, label = "page", transitionSpec = {
                        (fadeIn(tween(220, 40)) + slideInHorizontally(tween(280)) { width -> if (targetState.ordinal > initialState.ordinal) width / 16 else -width / 16 }) togetherWith
                            (fadeOut(tween(120)) + slideOutHorizontally(tween(180)) { width -> if (targetState.ordinal > initialState.ordinal) -width / 24 else width / 24 })
                    }) { current ->
                        pageStateHolder.SaveableStateProvider(if (current == Page.APP_STATS) "${current.name}:$detailPackage:$detailDate" else current.name) {
                        when (current) {
                            Page.HOME -> DashboardScreen(state, device, status == ShizukuStatus.READY, ::navigate, ::details, ::permissions, ::detailsAt, vm::refreshStatistics)
                            Page.USAGE -> UsageScreen(device, ::back, { navigate(Page.FREEZE) }, ::details)
                            Page.APP_STATS -> device.apps.find { it.packageName == detailPackage }?.let { app ->
                                AppStatsScreen(app, device, ::back, { pkg, date -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.graph.usage.details(pkg, date) } }, { detailPackage = app.packageName; pageName = Page.FREEZE.name }, detailDate)
                            } ?: Column {
                                PageHeader("Statistik aplikasi", ::back)
                                if (!device.loading) EmptyState("Aplikasi tidak ditemukan", "Aplikasi mungkin sudah dihapus dari perangkat.")
                            }
                            Page.NETWORK -> NetworkScreen(state, device, status, supported, busy, ::back, vm::setNetwork, ::permissions) { navigate(Page.FREEZE) }
                            Page.FREEZE -> FreezeScreen(state, device, status == ShizukuStatus.READY, busy,
                                vm::addFrozen, ::select,
                                { app ->
                                    if (state.settings.freezeChallengeEnabled) {
                                        if (status != ShizukuStatus.READY) permissions()
                                        else {
                                            removalPackage = app.packageName
                                            vm.prepareUnlock(app) { pageName = Page.UNLOCK.name }
                                        }
                                    } else vm.removeGate(app)
                                },
                                { enabled, config -> vm.settings { it.copy(freezeChallengeEnabled = enabled, freezeGate = config) } },
                                ::permissions, vm::closeAccess, vm::freezeAllNow, vm::addFrozenApps, vm::saveRule, detailPackage)
                            Page.UNLOCK -> UnlockScreen(device.apps.find { it.packageName == unlocking }, gate, challenge, completed, remaining, challengeError, busy, ::back, vm::newChallenge,
                                unlock = { answer ->
                                    if (removalPackage != null) vm.releaseAfterChallenge(answer) {
                                        removalPackage = null
                                        pageName = Page.FREEZE.name
                                    }
                                    else vm.unlock(answer) { pkg -> pageName = Page.FREEZE.name; launchApp(pkg) }
                                }, removing = removalPackage != null)
                            Page.SETTINGS -> SettingsScreen(state, status, ::back, vm::secure, ::permissions, vm::settings, { vm.secure { createPin = true } }, vm::removePin, vm::restore, update, autoUpdate, { vm.checkUpdates(true); if (update.available != null) updateSheet = true }, { enabled -> vm.secure { vm.updater.automatic(enabled) } })
                            Page.PERMISSIONS -> PermissionsScreen(device.usageAccess, status, ::back, vm.graph.gateway::requestPermission, ::back, vm::refresh, connectionError, vm::grantUsageThroughShizuku, busy)
                        }
                        }
                    }
                }
                if (device.loading && state.settings.onboardingDone) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
        if (updateSheet && update.available != null && !pinPrompt && !busy) UpdateSheet(update, { updateSheet = false }, vm::downloadUpdate, { vm.installUpdate { context.startActivity(it) } }, vm::cancelUpdate)
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
