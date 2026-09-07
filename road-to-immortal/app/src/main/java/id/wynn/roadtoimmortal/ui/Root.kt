package id.wynn.roadtoimmortal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.wynn.roadtoimmortal.MainViewModel
import id.wynn.roadtoimmortal.data.Equipment
import id.wynn.roadtoimmortal.domain.HeroPool
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmortalRoot(vm: MainViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var heroStack by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var gearId by rememberSaveable { mutableStateOf<String?>(null) }
    var rankSheet by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var sources by rememberSaveable { mutableStateOf(false) }
    var poolScreen by rememberSaveable { mutableStateOf(false) }
    var poolStartLane by rememberSaveable { mutableStateOf("EXP") }
    var poolHero by rememberSaveable { mutableStateOf<Int?>(null) }
    var poolLane by rememberSaveable { mutableStateOf<String?>(null) }
    var partySeed by rememberSaveable { mutableStateOf<Int?>(null) }
    var now by remember { mutableStateOf(Instant.now()) }
    val holder = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(30_000)
        }
    }
    val catalog = data.catalog
    val openHero: (Int) -> Unit = { heroStack = heroStack + it }
    val openGear: (Equipment) -> Unit = { gearId = it.id }
    val addPool: (Int, String?) -> Unit = { id, lane ->
        if (lane == null) {
            poolHero = id
            poolLane = null
        } else {
            val existing = prefs.pools[lane].orEmpty()
            val name = catalog?.heroById?.get(id)?.name ?: "Hero"
            val message =
                when {
                    id in existing -> "$name sudah ada di $lane"
                    existing.size >= 10 ->
                        "$lane penuh · atur Rancangan Hero untuk mengganti pilihan"
                    else -> {
                        vm.pool { HeroPool.add(it, lane, id) }
                        "$name ditambahkan ke $lane"
                    }
                }
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(message)
            }
        }
    }
    BackHandler(heroStack.isNotEmpty() || settings || sources || poolScreen || tab != 0) {
        when {
            sources -> sources = false
            settings -> settings = false
            heroStack.isNotEmpty() -> heroStack = heroStack.dropLast(1)
            poolScreen -> poolScreen = false
            else -> tab = 0
        }
    }
    ImmortalTheme(prefs.theme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (heroStack.isEmpty() && !settings && !sources && !poolScreen)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(
                                "Perjalanan" to Icons.Outlined.AutoAwesome,
                                "Jelajah" to Icons.Outlined.GridView,
                                "Meta" to Icons.Outlined.Leaderboard,
                                "Tier List" to Icons.Outlined.ViewAgenda,
                                "Tim" to Icons.Outlined.Groups,
                            )
                            .forEachIndexed { index, pair ->
                                NavigationBarItem(
                                    tab == index,
                                    { tab = index },
                                    icon = { Icon(pair.second, null) },
                                    label = { Text(pair.first) },
                                )
                            }
                    }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (data.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                AnimatedVisibility(data.error != null && catalog != null) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Mode tersimpan · pembaruan tertunda",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            TextButton(vm::refresh) { Text("Coba lagi") }
                        }
                    }
                }
                when {
                    catalog == null && data.loading ->
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(18.dp))
                            Text("Menyiapkan perjalananmu…")
                        }
                    catalog == null ->
                        EmptyState(
                            "Data belum bisa dibuka",
                            data.error ?: "Hubungkan internet untuk mengambil katalog MLBB.",
                            Icons.Outlined.CloudOff,
                            "Coba lagi",
                            vm::refresh,
                        )
                    sources ->
                        SourcesScreen(
                            catalog,
                            { sources = false },
                            vm::refresh,
                            data.refreshing,
                            data.error,
                        )
                    settings ->
                        SettingsScreen(prefs, vm::theme, { settings = false }, { sources = true })
                    heroStack.isNotEmpty() ->
                        catalog.heroById[heroStack.last()]?.let { h ->
                            holder.SaveableStateProvider("hero-${h.id}") {
                                HeroScreen(
                                    h,
                                    catalog,
                                    h.id in prefs.favorites,
                                    { vm.favorite(h.id) },
                                    { heroStack = heroStack.dropLast(1) },
                                    openHero,
                                    openGear,
                                    { sources = true },
                                    { addPool(h.id, null) },
                                    {
                                        partySeed = h.id
                                        heroStack = emptyList()
                                        poolScreen = false
                                        tab = 4
                                    },
                                )
                            }
                        }
                    poolScreen ->
                        PoolScreen(
                            catalog,
                            prefs.pools,
                            poolStartLane,
                            vm::pool,
                            openHero,
                            { poolScreen = false },
                        )
                    else ->
                        AnimatedContent(
                            tab,
                            label = "Navigasi",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                        ) { page ->
                            holder.SaveableStateProvider("tab-$page") {
                                when (page) {
                                    0 ->
                                        HomeScreen(
                                            catalog,
                                            prefs,
                                            now,
                                            { rankSheet = true },
                                            openHero,
                                            { tab = 2 },
                                            { settings = true },
                                            { sources = true },
                                            { lane ->
                                                poolStartLane = lane
                                                poolScreen = true
                                            },
                                        )
                                    1 ->
                                        AtlasScreen(
                                            catalog,
                                            prefs.favorites,
                                            openHero,
                                            openGear,
                                            addPool,
                                        )
                                    2 -> MetaScreen(catalog, openHero, { sources = true })
                                    3 ->
                                        TierScreen(
                                            catalog,
                                            prefs.pools,
                                            openHero,
                                            addPool,
                                            { sources = true },
                                        )
                                    else ->
                                        PartyScreen(
                                            catalog,
                                            partySeed,
                                            { partySeed = it },
                                            openHero,
                                            addPool,
                                            { sources = true },
                                        )
                                }
                            }
                        }
                }
            }
        }
        if (rankSheet && catalog != null)
            RankPicker(catalog, prefs.position, vm::rank, { rankSheet = false })
        poolHero?.let { id ->
            catalog?.heroById?.get(id)?.let { h ->
                PoolAddSheet(
                    h,
                    prefs.pools,
                    poolLane,
                    { lane -> vm.pool { HeroPool.add(it, lane, id) } },
                    { poolHero = null },
                )
            }
        }
        gearId?.let { id ->
            catalog?.equipmentById?.get(id)?.let { item ->
                key(id) { EquipmentSheet(item, catalog, { gearId = null }, openGear) }
            }
        }
    }
}
