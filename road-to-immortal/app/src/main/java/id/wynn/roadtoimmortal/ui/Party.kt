package id.wynn.roadtoimmortal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PartyScreen(
    catalog: Catalog,
    seed: Int?,
    onSeed: (Int?) -> Unit,
    onHero: (Int) -> Unit,
    onAdd: (Int, String?) -> Unit,
    onSources: () -> Unit,
) {
    var rank by rememberSaveable { mutableStateOf("mythic") }
    var mode by rememberSaveable { mutableStateOf("Duo") }
    var enemy by rememberSaveable { mutableStateOf<Int?>(null) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    var info by rememberSaveable { mutableStateOf(false) }
    val size =
        when (mode) {
            "Trio" -> 3
            "Squad" -> 5
            else -> 2
        }
    val result by
        produceState<List<Party>?>(null, catalog, rank, Triple(size, seed, enemy)) {
            value = null
            value =
                withContext(Dispatchers.Default) {
                    PartyEngine.recommend(catalog, rank, size, seed, enemy)
                }
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lebih kompak.", style = MaterialTheme.typography.headlineMedium)
                    Caption("Temukan partner untuk ranked bersama.")
                }
                IconButton({ info = true }) { Icon(Icons.Outlined.Info, "Tentang rekomendasi tim") }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoiceRow(listOf("Duo", "Trio", "Squad"), mode, { mode = it }, Modifier.weight(1f))
                ScopePicker(rank, { rank = it })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chosen = seed?.let(catalog.heroById::get)
                PartyChoice("Hero kamu", chosen, { picking = "seed" }, { onSeed(null) })
                PartyChoice(
                    "Lawan (opsional)",
                    enemy?.let(catalog.heroById::get),
                    { picking = "enemy" },
                    { enemy = null },
                )
            }
        }
        item { catalog.slice(rank)?.let { SourceLine(it.source, onSources) } }
        if (result == null)
            item {
                Row(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        else if (result!!.isEmpty())
            item { EmptyState("Kombinasi belum tersedia", "Coba rank atau hero lain.") }
        else
            itemsIndexed(result!!, key = { _, p -> p.heroes.joinToString { it.id.toString() } }) {
                index,
                party ->
                Surface(shape = Rounded, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Pilihan ${index + 1}",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Tag("Skor ${party.score}/100")
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            party.assignment.forEach { (lane, id) ->
                                catalog.heroById[id]?.let { hero ->
                                    Column(
                                        Modifier.width(if (size == 2) 116.dp else 88.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        PoolPortrait(
                                            hero,
                                            false,
                                            { onHero(id) },
                                            { onAdd(id, lane) },
                                        )
                                        Caption(lane)
                                    }
                                }
                            }
                        }
                        Caption(party.reasons.joinToString(" · "))
                        if (party.pairEvidence == 0) Caption("Sinergi pasangan belum tercatat")
                    }
                }
            }
    }
    if (picking != null)
        HeroSelectSheet(
            catalog,
            setOfNotNull(if (picking == "seed") enemy else seed),
            if (picking == "seed") "Pilih hero kamu" else "Pilih hero lawan",
            { id ->
                if (picking == "seed") onSeed(id) else enemy = id
                picking = null
            },
            { picking = null },
        )
    if (info)
        AlertDialog(
            onDismissRequest = { info = false },
            title = { Text("Skor rekomendasi") },
            text = {
                Text(
                    "Skor olahan dari meta rank, sinergi pasangan yang tersedia, lane, dan komposisi role. Pilih lawan untuk mempertimbangkan matchup.\n\nSkor bukan peluang menang. Kombinasi ini adalah saran, bukan statistik pertandingan party."
                )
            },
            confirmButton = { TextButton({ info = false }) { Text("Mengerti") } },
        )
}

@Composable
private fun PartyChoice(label: String, hero: Hero?, onChoose: () -> Unit, onClear: () -> Unit) {
    Surface(
        onClick = onChoose,
        shape = Rounded,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hero != null) Artwork(hero.icon, null, Modifier.size(48.dp))
            else Icon(Icons.Outlined.PersonAdd, null, Modifier.size(48.dp).padding(10.dp))
            Column(Modifier.weight(1f)) {
                Caption(label)
                Text(hero?.name ?: "Pilih hero", style = MaterialTheme.typography.titleMedium)
            }
            if (hero != null) IconButton(onClear) { Icon(Icons.Outlined.Close, "Hapus $label") }
            else Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroSelectSheet(
    catalog: Catalog,
    excluded: Set<Int>,
    title: String,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var lane by rememberSaveable { mutableStateOf("Semua") }
    val rows =
        remember(catalog, excluded, query, lane) {
            catalog.heroes.filter {
                it.id !in excluded &&
                    (lane == "Semua" || lane in it.lanes) &&
                    searchKey(query) in searchKey(it.name)
            }
        }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyVerticalGrid(
            GridCells.Adaptive(88.dp),
            modifier = Modifier.fillMaxHeight(.88f).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    SearchBox(query, { query = it }, "Cari hero")
                    ChoiceRow(listOf("Semua") + lanes, lane, { lane = it })
                }
            }
            if (rows.isEmpty())
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        "Hero tidak ditemukan",
                        "Coba nama lain. Hero yang sudah dipilih tidak ditampilkan.",
                    )
                }
            items(rows, key = { it.id }) { hero ->
                Column(
                    Modifier.clickable { onSelect(hero.id) },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Artwork(hero.icon, null, Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(
                        hero.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
