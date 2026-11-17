@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package id.wynn.arithmemo.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.data.MemoStore
import id.wynn.arithmemo.update.*
import java.time.LocalDate
import kotlinx.coroutines.delay

private enum class Page(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Outlined.Home),
    LIBRARY("Hafalan", Icons.Outlined.Style),
    STATS("Statistik", Icons.Outlined.Insights),
    SETTINGS("Pengaturan", Icons.Outlined.Settings),
}

@Composable
fun MemoApp(vm: MemoViewModel, openUpdates: Boolean = false) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val update by vm.updater.state.collectAsStateWithLifecycle()
    val automatic by vm.updater.automatic.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(if (openUpdates) Page.SETTINGS else Page.HOME) }
    var practice by rememberSaveable { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<StudyMode?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let {
                vm.export { data ->
                    context.contentResolver.openOutputStream(it, "wt")?.use { output ->
                        output.write(data.toByteArray())
                    } ?: error("File tidak dapat ditulis")
                }
            }
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            importUri = uri
        }
    val notifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            vm.message.value =
                if (granted) "Notifikasi pembaruan diaktifkan"
                else "Izin notifikasi belum diberikan. Update tetap dapat diperiksa di aplikasi."
        }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.dismissMessage()
        }
    }
    LaunchedEffect(loaded) { if (loaded && (automatic || openUpdates)) vm.checkUpdate() }
    DisposableEffect(lifecycle, practice) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.foreground(practice)
                Lifecycle.Event.ON_STOP -> vm.foreground(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        vm.foreground(practice && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            lifecycle.removeObserver(observer)
            vm.foreground(false)
        }
    }
    LaunchedEffect(practice, loaded) {
        while (practice && loaded) {
            delay(250)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) vm.tick()
        }
    }
    BackHandler(practice || page != Page.HOME) {
        if (practice) practice = false else page = Page.HOME
    }
    fun start(mode: StudyMode) {
        if (snapshot.session?.let { !it.finished } == true) pendingMode = mode
        else {
            vm.start(mode)
            practice = true
        }
    }
    ArithMemoTheme(snapshot.settings.theme) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (!loaded)
                Box(
                    Modifier.fillMaxSize().safeDrawingPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Menyiapkan hafalan…")
                        message?.let {
                            Text(it)
                            Button(
                                onClick = {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.provider.Settings
                                                .ACTION_INTERNAL_STORAGE_SETTINGS
                                        )
                                    )
                                }
                            ) {
                                Text("Periksa penyimpanan")
                            }
                        }
                    }
                }
            else if (practice && snapshot.session != null)
                SessionScreen(
                    snapshot.session!!,
                    busy,
                    vm,
                    { practice = false },
                    {
                        vm.abandon()
                        practice = false
                    },
                    snapshot.settings.haptics,
                    snackbar,
                )
            else
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 600.dp
                    Row {
                        if (wide)
                            NavigationRail(
                                Modifier.fillMaxHeight().safeDrawingPadding(),
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                Spacer(Modifier.height(24.dp))
                                Page.entries.forEach { p ->
                                    NavigationRailItem(
                                        selected = page == p,
                                        onClick = { page = p },
                                        icon = { Icon(p.icon, null) },
                                        label = { Text(p.label) },
                                    )
                                }
                            }
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            snackbarHost = { SnackbarHost(snackbar) },
                            bottomBar = {
                                if (!wide)
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp,
                                    ) {
                                        Page.entries.forEach { p ->
                                            NavigationBarItem(
                                                selected = page == p,
                                                onClick = { page = p },
                                                icon = { Icon(p.icon, null) },
                                                label = { Text(p.label, maxLines = 2) },
                                            )
                                        }
                                    }
                            },
                        ) { padding ->
                            Box(
                                Modifier.fillMaxSize()
                                    .padding(padding)
                                    .consumeWindowInsets(padding),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Crossfade(page, label = "Navigasi") { selected ->
                                    when (selected) {
                                        Page.HOME ->
                                            HomeScreen(
                                                snapshot,
                                                update.available != null,
                                                { start(it) },
                                                { practice = true },
                                                { page = Page.SETTINGS },
                                                { page = Page.LIBRARY },
                                            )
                                        Page.LIBRARY ->
                                            LibraryScreen(
                                                snapshot,
                                                vm::settings,
                                                { start(StudyMode.FLASHCARD) },
                                            )
                                        Page.STATS -> StatsScreen(snapshot)
                                        Page.SETTINGS ->
                                            SettingsScreen(
                                                snapshot.settings,
                                                vm::settings,
                                                update,
                                                automatic,
                                                { value ->
                                                    vm.updater.automatic(value)
                                                    UpdateWorker.schedule(context)
                                                },
                                                vm::checkUpdate,
                                                vm::downloadUpdate,
                                                { vm.install { context.startActivity(it) } },
                                                vm::cancelDownload,
                                                {
                                                    if (Build.VERSION.SDK_INT >= 33)
                                                        notifications.launch(
                                                            Manifest.permission.POST_NOTIFICATIONS
                                                        )
                                                    else
                                                        vm.message.value =
                                                            "Notifikasi mengikuti pengaturan Android"
                                                },
                                                {
                                                    export.launch(
                                                        "ArithMemo-${LocalDate.now()}.json"
                                                    )
                                                },
                                                {
                                                    import.launch(
                                                        arrayOf(
                                                            "application/json",
                                                            "text/plain",
                                                            "application/octet-stream",
                                                        )
                                                    )
                                                },
                                            )
                                    }
                                }
                                if (busy)
                                    LinearProgressIndicator(
                                        Modifier.fillMaxWidth().align(Alignment.TopCenter)
                                    )
                            }
                        }
                    }
                }
            pendingMode?.let { mode ->
                AlertDialog(
                    onDismissRequest = { pendingMode = null },
                    title = { Text("Mulai sesi baru?") },
                    text = {
                        Text(
                            "Jawaban yang selesai tetap tersimpan. Sisa sesi saat ini akan diganti."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingMode = null
                                vm.start(mode)
                                practice = true
                            }
                        ) {
                            Text("Mulai baru")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingMode = null
                                practice = true
                            }
                        ) {
                            Text("Lanjutkan sesi")
                        }
                    },
                )
            }
            importUri?.let { uri ->
                AlertDialog(
                    onDismissRequest = { importUri = null },
                    title = { Text("Pulihkan cadangan?") },
                    text = {
                        Text(
                            "Data hafalan saat ini akan diganti dengan isi cadangan. Ekspor data Anda terlebih dahulu bila ingin menyimpannya."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                importUri = null
                                vm.import {
                                    context.contentResolver.openInputStream(uri)?.use {
                                        MemoStore.readBackup(it)
                                    } ?: error("Cadangan tidak dapat dibaca")
                                }
                            }
                        ) {
                            Text("Pulihkan")
                        }
                    },
                    dismissButton = { TextButton(onClick = { importUri = null }) { Text("Batal") } },
                )
            }
        }
    }
}
