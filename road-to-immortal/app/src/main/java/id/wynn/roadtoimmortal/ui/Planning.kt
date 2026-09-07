package id.wynn.roadtoimmortal.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*

@Composable
fun PoolPortrait(hero: Hero, added: Boolean, onHero: () -> Unit, onAdd: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Artwork(
                hero.icon,
                hero.name,
                Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onHero),
                round = 20.dp,
            )
            FilledTonalIconButton(onAdd, Modifier.align(Alignment.BottomEnd).size(48.dp)) {
                Icon(
                    if (added) Icons.Outlined.Check else Icons.Outlined.Add,
                    "Rancang ${hero.name}",
                )
            }
        }
        Text(
            hero.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PoolPreview(
    catalog: Catalog,
    pools: Map<String, List<Int>>,
    onManage: () -> Unit,
    onHero: (Int) -> Unit,
) {
    var lane by rememberSaveable { mutableStateOf("EXP") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Rancangan Hero", "Atur", onManage)
        ChoiceRow(listOf("EXP", "Jungle", "Roam", "Mid", "Gold"), lane, { lane = it })
        val ids = pools[lane].orEmpty()
        if (ids.isEmpty())
            Surface(shape = Rounded, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Siapa andalanmu di $lane?", style = MaterialTheme.typography.titleMedium)
                    Caption("Simpan hingga 10 hero untuk giliran ranked berikutnya.")
                    FilledTonalButton(onManage) { Text("Pilih hero") }
                }
            }
        else
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ids, key = { it }) { id ->
                    val hero = catalog.heroById[id]
                    Column(Modifier.width(76.dp).clickable { if (hero != null) onHero(id) }) {
                        Artwork(hero?.icon, hero?.name, Modifier.size(76.dp))
                        Text(
                            hero?.name ?: "Hero #$id",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolAddSheet(
    hero: Hero,
    pools: Map<String, List<Int>>,
    preferred: String?,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Artwork(hero.icon, null, Modifier.size(64.dp))
                Column {
                    Text(hero.name, style = MaterialTheme.typography.headlineSmall)
                    Caption("Tambahkan ke Rancangan Hero")
                }
            }
            (listOfNotNull(preferred) + hero.lanes + lanes)
                .distinct()
                .filter { it in lanes }
                .forEach { lane ->
                    val ids = pools[lane].orEmpty()
                    OutlinedButton(
                        {
                            onAdd(lane)
                            onDismiss()
                        },
                        enabled = hero.id !in ids && ids.size < 10,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(lane, Modifier.weight(1f))
                        Text(
                            if (hero.id in ids) "Sudah dipilih"
                            else if (ids.size == 10) "Penuh · 10/10" else "${ids.size}/10  +"
                        )
                    }
                }
        }
    }
}

@Composable
fun PoolScreen(
    catalog: Catalog,
    pools: Map<String, List<Int>>,
    onEdit: ((Map<String, List<Int>>) -> Map<String, List<Int>>) -> Unit,
    onHero: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var lane by rememberSaveable { mutableStateOf("EXP") }
    var query by rememberSaveable { mutableStateOf("") }
    var moving by rememberSaveable { mutableStateOf<Int?>(null) }
    val ids = pools[lane].orEmpty()
    val heroes =
        remember(catalog, query, lane, ids) {
            catalog.heroes.filter {
                it.id !in ids &&
                    (if (query.isBlank()) lane in it.lanes
                    else searchKey(query) in searchKey(it.name))
            }
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali") }
                Text("Rancangan Hero", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            ChoiceRow(
                listOf("EXP", "Jungle", "Roam", "Mid", "Gold"),
                lane,
                {
                    lane = it
                    query = ""
                },
            )
        }
        item { Caption("$lane · ${ids.size}/10 hero") }
        if (ids.isEmpty()) item { Caption("Belum ada pilihan. Tambahkan hero di bawah.") }
        itemsIndexed(ids, key = { _, id -> "pool-$id" }) { index, id ->
            val hero = catalog.heroById[id]
            Surface(
                Modifier.animateItem(),
                shape = Rounded,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.padding(12.dp).animateContentSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Artwork(
                            hero?.icon,
                            hero?.name,
                            Modifier.size(52.dp).clickable { if (hero != null) onHero(id) },
                        )
                        Text(
                            hero?.name ?: "Hero #$id",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton({ onEdit { HeroPool.remove(it, lane, id) } }) {
                            Icon(Icons.Outlined.Close, "Hapus ${hero?.name ?: id} dari $lane")
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton({ moving = id }) { Text("Pindah lane") }
                        IconButton(
                            { onEdit { HeroPool.reorder(it, lane, id, -1) } },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Outlined.KeyboardArrowUp, "Naikkan ${hero?.name ?: id}")
                        }
                        IconButton(
                            { onEdit { HeroPool.reorder(it, lane, id, 1) } },
                            enabled = index < ids.lastIndex,
                        ) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "Turunkan ${hero?.name ?: id}")
                        }
                    }
                }
            }
        }
        item {
            SectionTitle(if (ids.size >= 10) "Pool penuh" else "Tambah hero")
            SearchBox(query, { query = it }, "Cari hero untuk $lane")
        }
        if (ids.size >= 10)
            item { Caption("Hapus atau pindahkan satu hero untuk mengganti pilihan.") }
        if (heroes.isEmpty()) item { Caption("Hero tidak ditemukan.") }
        items(heroes, key = { "add-${it.id}" }) { hero ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Artwork(hero.icon, null, Modifier.size(52.dp).clickable { onHero(hero.id) })
                Column(Modifier.weight(1f).clickable { onHero(hero.id) }) {
                    Text(hero.name, style = MaterialTheme.typography.titleMedium)
                    Caption(hero.lanes.joinToString(" · "))
                }
                IconButton(
                    { onEdit { HeroPool.add(it, lane, hero.id) } },
                    enabled = ids.size < 10,
                ) {
                    Icon(Icons.Outlined.Add, "Tambahkan ${hero.name} ke $lane")
                }
            }
        }
    }
    moving?.let { id ->
        AlertDialog(
            onDismissRequest = { moving = null },
            title = { Text("Pindah ke lane") },
            text = {
                Column {
                    lanes
                        .filter { it != lane }
                        .forEach { target ->
                            val targetIds = pools[target].orEmpty()
                            TextButton(
                                {
                                    onEdit { HeroPool.move(it, lane, target, id) }
                                    moving = null
                                },
                                enabled = targetIds.size < 10 || id in targetIds,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("$target · ${targetIds.size}/10")
                            }
                        }
                }
            },
            confirmButton = { TextButton({ moving = null }) { Text("Batal") } },
        )
    }
}

@Composable
fun TierScreen(
    catalog: Catalog,
    pools: Map<String, List<Int>>,
    onHero: (Int) -> Unit,
    onAdd: (Int, String?) -> Unit,
    onSources: () -> Unit,
) {
    var rank by rememberSaveable { mutableStateOf("mythic") }
    var lane by rememberSaveable { mutableStateOf("EXP") }
    var query by rememberSaveable { mutableStateOf("") }
    var info by rememberSaveable { mutableStateOf(false) }
    val entries = remember(catalog, rank, lane) { TierEngine.entries(catalog, rank, lane) }
    val filtered = entries.filter { searchKey(query) in searchKey(it.hero.name) }
    LazyVerticalGrid(
        GridCells.Adaptive(92.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tier List",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    IconButton({ info = true }) {
                        Icon(Icons.Outlined.Info, "Cara menghitung tier")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScopePicker(rank, { rank = it })
                    Caption("Tier olahan · 7 hari")
                }
                ChoiceRow(listOf("EXP", "Jungle", "Roam", "Mid", "Gold"), lane, { lane = it })
                SearchBox(query, { query = it }, "Cari hero di $lane")
                catalog.slice(rank)?.let { SourceLine(it.source, onSources) }
            }
        }
        if (filtered.isEmpty())
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    "Belum ada hasil",
                    if (entries.isEmpty()) "Statistik rank ini belum tersedia."
                    else "Coba hero lain atau ganti lane.",
                )
            }
        TierEngine.tiers.forEach { tier ->
            val rows = filtered.filter { it.tier == tier }
            if (rows.isNotEmpty()) {
                item(key = "tier-$tier", span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = Rounded,
                        color =
                            if (tier == "SS") MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                tier,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text("${rows.size} hero", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                items(rows, key = { it.hero.id }) { entry ->
                    Column {
                        PoolPortrait(
                            entry.hero,
                            entry.hero.id in pools[lane].orEmpty(),
                            { onHero(entry.hero.id) },
                            { onAdd(entry.hero.id, lane) },
                        )
                        Caption("WR ${rate(entry.meta.win)}")
                    }
                }
            }
        }
    }
    if (info)
        AlertDialog(
            onDismissRequest = { info = false },
            title = { Text("Tentang tier") },
            text = {
                Text(
                    "Urutan olahan dari win, pick, dan ban rate pada rank pilihan. Hero dengan pick rendah diberi bobot lebih kecil. SS adalah 10% teratas, lalu S 20%, A 30%, B 25%, dan C sisanya.\n\nLane menyaring hero; statistik sumber mencakup semua lane hero tersebut."
                )
            },
            confirmButton = { TextButton({ info = false }) { Text("Mengerti") } },
        )
}
