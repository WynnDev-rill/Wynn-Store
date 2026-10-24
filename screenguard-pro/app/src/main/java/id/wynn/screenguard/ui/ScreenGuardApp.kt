package id.wynn.screenguard.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
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
    val state by vm.state.collectAsStateWithLifecycle()
    val device by vm.device.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val connectionError by vm.graph.gateway.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var pageName by rememberSaveable { mutableStateOf(Page.FREEZE.name) }
    val page = runCatching { Page.valueOf(pageName) }.getOrDefault(Page.FREEZE)
    var previousPage by rememberSaveable { mutableStateOf(Page.FREEZE.name) }
    var detailPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var detailDays by rememberSaveable { mutableIntStateOf(1) }
    var detailDate by rememberSaveable { mutableStateOf(java.time.LocalDate.now().toString()) }
    var detailReturn by rememberSaveable { mutableStateOf(Page.HOME.name) }
    var updateSheet by rememberSaveable { mutableStateOf(false) }
    var offeredVersion by rememberSaveable { mutableLongStateOf(0) }
    val holder = rememberSaveableStateHolder()
    val context = LocalContext.current
    LaunchedEffect(update.available?.versionCode, state.settings.onboardingDone) {
        val version = update.available?.versionCode ?: return@LaunchedEffect
        if (state.settings.onboardingDone && version != offeredVersion) { offeredVersion = version; updateSheet = true }
    }
    LaunchedEffect(state.rules) { vm.refresh() }
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window,window.decorView).apply {
                isAppearanceLightStatusBars = !state.settings.darkMode
                isAppearanceLightNavigationBars = !state.settings.darkMode
            }
        }
    }
    fun back() { pageName = when(page) { Page.APP_STATS -> detailReturn; Page.USAGE -> Page.HOME.name; Page.PERMISSIONS -> previousPage; else -> Page.FREEZE.name } }
    fun navigate(next: Page) { pageName = next.name }
    fun permissions() { previousPage = pageName; pageName = Page.PERMISSIONS.name }
    fun details(app: InstalledApp, date: java.time.LocalDate = java.time.LocalDate.now(), days: Int = 1) {
        detailDays = days
        detailPackage = app.packageName; detailDate = date.toString(); detailReturn = pageName; pageName = Page.APP_STATS.name
    }
    fun launch(app: InstalledApp) {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent == null) vm.message.value = "Aplikasi tidak dapat dibuka"
        else runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { vm.message.value = "Aplikasi tidak dapat dibuka" }
    }
    fun select(app: InstalledApp) {
        if (!app.enabled || app.suspended) {
            if (status != ShizukuStatus.READY) permissions()
            else vm.setManualFreeze(app,false) { launch(app) }
        } else launch(app)
    }
    BackHandler(page != Page.FREEZE && state.settings.onboardingDone && !busy) { back() }
    GuardTheme(state.settings.darkMode) {
        Scaffold(Modifier.imePadding(), containerColor = MaterialTheme.colorScheme.background, bottomBar = {
            if (state.settings.onboardingDone && page != Page.PERMISSIONS) FloatingDock(page,::navigate)
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                    if (!state.settings.onboardingDone) PermissionsScreen(device.usageAccess,status,null,vm.graph.gateway::requestPermission,vm::onboarded,vm::refresh,connectionError,vm::grantUsageThroughShizuku,busy)
                    else AnimatedContent(page, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "page") { current ->
                        holder.SaveableStateProvider(current.name + if(current == Page.APP_STATS) ":$detailPackage:$detailDate:$detailDays" else "") {
                            when(current) {
                                Page.FREEZE -> FreezeScreen(state,device,status == ShizukuStatus.READY,busy,
                                    { vm.addManagedApps(listOf(it)) },::select,vm::removeManagedApp,{ _,_ -> },
                                    ::permissions,{ vm.setManualFreeze(it,true) },vm::freezeManagedApps,vm::addManagedApps)
                                Page.HOME -> DashboardScreen(state,device,status == ShizukuStatus.READY,::navigate,{ details(it) },::permissions,{ app,date -> details(app,date) },vm::refreshStatistics,{app,date,count -> details(app,date,count)})
                                Page.USAGE -> UsageScreen(device,::back,null) { details(it) }
                                Page.APP_STATS -> {
                                    val app = device.apps.find { it.packageName == detailPackage } ?: InstalledApp(detailPackage.orEmpty(),detailPackage?.substringAfterLast('.').orEmpty(),0,-1,true,false,true,false,launchable=false)
                                    AppStatsScreen(app,device,::back,{ pkg,date -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { vm.graph.usage.details(pkg,date) } },{
                                        pageName = Page.FREEZE.name
                                    },detailDate,detailDays)
                                }
                                Page.SETTINGS -> SettingsScreen(state,status,::back,vm::secure,::permissions,vm::settings,{}, {},vm::restore,
                                    update,autoUpdate,{ vm.checkUpdates(true); if(update.available != null) updateSheet = true },vm.updater::automatic)
                                Page.PERMISSIONS -> PermissionsScreen(device.usageAccess,status,::back,vm.graph.gateway::requestPermission,::back,vm::refresh,connectionError,vm::grantUsageThroughShizuku,busy)
                                else -> LaunchedEffect(Unit) { pageName = Page.FREEZE.name }
                            }
                        }
                    }
                }
                if(device.loading && state.settings.onboardingDone) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
        if(updateSheet && update.available != null && !busy) UpdateSheet(update,{ updateSheet=false },vm::downloadUpdate,{ vm.installUpdate { context.startActivity(it) } },vm::cancelUpdate)
        if(message != null) AlertDialog(onDismissRequest = { vm.message.value=null },title = { Text("ScreenGuard Pro") },text = { Text(message!!) },confirmButton = { TextButton({vm.message.value=null}) {Text("Tutup")} })
        if(busy) Dialog(onDismissRequest = {}) { Surface(shape=RoundedCornerShape(20.dp)) {
            Row(Modifier.padding(24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=3.dp);Text("Memproses…") }
        } }
    }
}
