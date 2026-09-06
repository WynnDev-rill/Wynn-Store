package id.wynn.roadtoimmortal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftScreen(catalog: Catalog, onHero: (Int) -> Unit, onGear: (Equipment) -> Unit) {
    var allies by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var enemies by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var bans by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var select by rememberSaveable { mutableStateOf<String?>(null) }
    var rank by rememberSaveable { mutableStateOf("mythic") }
    var lane by rememberSaveable { mutableStateOf("Semua") }
    var threat by rememberSaveable { mutableStateOf("Heal & shield") }
    var reset by remember { mutableStateOf(false) }
    val suggestions =
        remember(catalog, allies, enemies, bans, rank, lane) {
            DraftEngine.recommend(catalog, allies, enemies, bans.toSet(), rank, lane).take(10)
        }
    val assignment =
        remember(catalog, allies) {
            DraftEngine.laneAssignment(allies.mapNotNull(catalog.heroById::get))
        }
    val threats =
        linkedMapOf(
            "Heal & shield" to "Regen & shield",
            "Attack speed" to "Attack speed",
            "Physical burst" to "Physical burst",
            "Magic burst" to "Magic burst",
            "HP tinggi" to "HP tinggi",
            "Armor tinggi" to "Armor tinggi",
            "Magic defense" to "Magic defense",
        )
    val situational =
        remember(catalog, threat) {
            DraftEngine.itemCounters(catalog.items, threats[threat].orEmpty()).take(6)
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Menang dari draft.", style = MaterialTheme.typography.headlineMedium)
                    Caption("Susun tim. Temukan pilihan berikutnya.")
                }
                IconButton(
                    { reset = true },
                    enabled = allies.isNotEmpty() || enemies.isNotEmpty() || bans.isNotEmpty(),
                ) {
                    Icon(Icons.Outlined.RestartAlt, "Reset draft")
                }
            }
        }
        item {
            DraftTeam(
                "Tim kamu",
                allies,
                catalog,
                MaterialTheme.colorScheme.primary,
                { select = "ally" },
                { allies = allies - it },
            )
        }
        item {
            DraftTeam(
                "Tim lawan",
                enemies,
                catalog,
                MaterialTheme.colorScheme.error,
                { select = "enemy" },
                { enemies = enemies - it },
            )
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Ban",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton({ select = "ban" }, enabled = bans.size < 10) {
                    Icon(Icons.Outlined.Block, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah ban (${bans.size}/10)")
                }
            }
            if (bans.isNotEmpty())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bans) { id ->
                        catalog.heroById[id]?.let { h ->
                            InputChip(
                                selected = true,
                                onClick = { bans = bans - id },
                                label = { Text(h.name) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Outlined.Close,
                                        "Hapus ban ${h.name}",
                                        Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
        }
        item {
            Surface(shape = Rounded, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Cakupan lane · ${assignment.size}/5",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    lanes.forEach { l ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (l in assignment) Icons.Outlined.CheckCircle
                                else Icons.Outlined.RadioButtonUnchecked,
                                null,
                                Modifier.size(16.dp),
                                tint =
                                    if (l in assignment) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                l,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Caption(
                                assignment[l]?.let { catalog.heroById[it]?.name } ?: "Belum terisi"
                            )
                        }
                    }
                    if (allies.size == 5 && assignment.size < 5)
                        Caption("Beberapa hero berebut lane. Pertimbangkan mengganti satu pilihan.")
                }
            }
        }
        if (allies.size < 5) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pilihan berikutnya",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    ScopePicker(rank, { rank = it })
                }
                ChoiceRow(listOf("Semua") + lanes, lane, { lane = it })
            }
            items(suggestions, key = { it.hero.id }) { s ->
                Surface(shape = Rounded, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Artwork(
                                s.hero.icon,
                                null,
                                Modifier.size(52.dp).clickable { onHero(s.hero.id) },
                            )
                            Column(Modifier.weight(1f).clickable { onHero(s.hero.id) }) {
                                Text(s.hero.name, style = MaterialTheme.typography.titleMedium)
                                Caption(s.hero.lanes.joinToString(" · "))
                            }
                            FilledTonalIconButton({ allies = allies + s.hero.id }) {
                                Icon(Icons.Outlined.Add, "Pilih ${s.hero.name} untuk tim kamu")
                            }
                        }
                        Caption(s.reasons.joinToString("\n"))
                    }
                }
            }
            if (suggestions.isEmpty())
                item {
                    EmptyState(
                        "Tidak ada pilihan tersisa",
                        "Ubah filter lane atau ban untuk menemukan hero lain.",
                    )
                }
            item {
                Caption(
                    "Urutan menggabungkan meta ${scopes[rank]}, matchup, sinergi, dan cakupan lane. Ini bantuan memilih, bukan prediksi peluang menang."
                )
            }
        }
        item {
            SectionTitle("Jawab ancaman lawan")
            Caption("Item situasional berdasarkan efek pada katalog")
            ChoiceRow(threats.keys.toList(), threat, { threat = it })
        }
        items(situational, key = { "gear-${it.id}" }) { item ->
            Surface(
                onClick = { onGear(item) },
                shape = Rounded,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Artwork(item.icon, null, Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Caption("$threat · ${categoryLabel(item.category)}")
                    }
                    Icon(Icons.Outlined.ChevronRight, null)
                }
            }
        }
        if (situational.isEmpty())
            item {
                Caption(
                    "Efek item untuk ancaman ini belum teridentifikasi pada sumber. Buka katalog untuk membandingkan atributnya."
                )
            }
        item {
            Caption(
                "Pilihan situasional mengikuti aturan efek item, bukan statistik counter item. Cocokkan dengan tipe damage, role, dan kondisi pertandingan."
            )
        }
    }
    if (reset)
        AlertDialog(
            onDismissRequest = { reset = false },
            title = { Text("Mulai draft baru?") },
            text = { Text("Pilihan tim dan ban akan dikosongkan.") },
            confirmButton = {
                TextButton({
                    allies = emptyList()
                    enemies = emptyList()
                    bans = emptyList()
                    reset = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = { TextButton({ reset = false }) { Text("Batal") } },
        )
    if (select != null) {
        var query by rememberSaveable { mutableStateOf("") }
        var filter by rememberSaveable { mutableStateOf("Semua") }
        val taken = (allies + enemies + bans).toSet()
        val options =
            remember(catalog, query, filter, taken) {
                catalog.heroes.filter {
                    it.id !in taken &&
                        (filter == "Semua" || filter in it.lanes) &&
                        searchKey(query) in searchKey(it.name)
                }
            }
        ModalBottomSheet(
            onDismissRequest = { select = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier.fillMaxHeight(.88f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    when (select) {
                        "ally" -> "Pilih untuk tim kamu"
                        "enemy" -> "Pilih hero lawan"
                        else -> "Ban hero"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                SearchBox(query, { query = it }, "Cari hero")
                ChoiceRow(listOf("Semua") + lanes, filter, { filter = it })
                if (options.isEmpty())
                    EmptyState(
                        "Hero tidak ditemukan",
                        "Hero yang sudah dipilih atau di-ban tidak ditampilkan.",
                    )
                LazyVerticalGrid(
                    GridCells.Adaptive(88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(options, key = { it.id }) { h ->
                        Column(
                            Modifier.clickable {
                                when (select) {
                                    "ally" -> if (allies.size < 5) allies = allies + h.id
                                    "enemy" -> if (enemies.size < 5) enemies = enemies + h.id
                                    else -> if (bans.size < 10) bans = bans + h.id
                                }
                                select = null
                            },
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Artwork(h.icon, null, Modifier.fillMaxWidth().aspectRatio(1f))
                            Text(h.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftTeam(
    title: String,
    ids: List<Int>,
    catalog: Catalog,
    color: androidx.compose.ui.graphics.Color,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until 5).forEach { index ->
                val h = ids.getOrNull(index)?.let(catalog.heroById::get)
                Surface(
                    onClick = { if (h == null) onAdd() else onRemove(h.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier =
                        Modifier.weight(1f).aspectRatio(.78f).semantics {
                            contentDescription =
                                if (h == null) "$title, pilih hero slot ${index+1}"
                                else "$title, hapus ${h.name}"
                        },
                ) {
                    if (h == null)
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, null, tint = color)
                        }
                    else
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Artwork(h.icon, null, Modifier.fillMaxWidth().weight(1f), round = 14.dp)
                            Text(
                                h.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                }
            }
        }
        if (ids.isNotEmpty()) Caption("Ketuk hero untuk menghapus pilihan.")
    }
}
