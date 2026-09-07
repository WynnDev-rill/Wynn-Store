package id.wynn.roadtoimmortal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*

@Composable
fun AtlasScreen(
    catalog: Catalog,
    favorites: Set<Int>,
    onHero: (Int) -> Unit,
    onGear: (Equipment) -> Unit,
    onAdd: (Int, String?) -> Unit,
) {
    var section by rememberSaveable { mutableStateOf("Hero") }
    var query by rememberSaveable { mutableStateOf("") }
    var lane by rememberSaveable { mutableStateOf("Semua") }
    var category by rememberSaveable { mutableStateOf("Semua") }
    var saved by rememberSaveable { mutableStateOf(false) }
    var threat by rememberSaveable { mutableStateOf("Regen & shield") }
    val threats =
        listOf(
            "Regen & shield",
            "Attack speed",
            "Physical burst",
            "Magic burst",
            "HP tinggi",
            "Armor tinggi",
            "Magic defense",
        )
    val index =
        remember(catalog) {
            catalog.heroes.associate {
                it.id to searchKey(it.name + it.roles.joinToString() + it.lanes.joinToString())
            }
        }
    val heroes =
        remember(catalog, query, lane, saved, favorites) {
            catalog.heroes.filter {
                (lane == "Semua" || lane in it.lanes) &&
                    (!saved || it.id in favorites) &&
                    searchKey(query) in index[it.id].orEmpty()
            }
        }
    val allGear =
        when (section) {
            "Item",
            "Counter" -> catalog.items
            "Emblem" -> catalog.emblems
            else -> catalog.spells
        }
    val categories =
        listOf("Semua") + allGear.map { categoryLabel(it.category) }.distinct().sorted()
    val gear =
        remember(allGear, query, category, section, threat) {
            val q = searchKey(query)
            (if (section == "Counter") DraftEngine.itemCounters(allGear, threat) else allGear)
                .filter {
                    ((category == "Semua" && it.category != "Metadata") ||
                        categoryLabel(it.category) == category) &&
                        q in searchKey(it.name + it.category + it.stats.joinToString())
                }
                .sortedWith(compareBy<Equipment> { it.category }.thenBy { it.name })
        }
    val compactHeight = LocalConfiguration.current.screenHeightDp < 600
    val noResults = section == "Hero" && heroes.isEmpty() || section != "Hero" && gear.isEmpty()
    val header: @Composable () -> Unit = {
        Column(
            Modifier.padding(
                start = if (compactHeight) 0.dp else 20.dp,
                end = if (compactHeight) 0.dp else 20.dp,
                top = if (compactHeight) 0.dp else 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Kenali pilihanmu.",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                if (section == "Hero")
                    IconToggleButton(saved, { saved = it }) {
                        Icon(
                            if (saved) Icons.Outlined.BookmarkAdded
                            else Icons.Outlined.BookmarkBorder,
                            "Hero favorit",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
            }
            SearchBox(
                query,
                { query = it },
                if (section == "Hero") "Cari hero, role, atau lane"
                else "Cari ${section.lowercase()}",
            )
            ChoiceRow(
                listOf("Hero", "Item", "Counter", "Emblem", "Spell"),
                section,
                {
                    section = it
                    query = ""
                    category = "Semua"
                },
            )
            if (section == "Hero") ChoiceRow(listOf("Semua") + lanes, lane, { lane = it })
            else if (section == "Counter") {
                ChoiceRow(threats, threat, { threat = it })
                Caption("Item situasional berdasarkan efeknya")
            } else if (categories.size > 2) ChoiceRow(categories, category, { category = it })
            Caption(
                if (section == "Hero") "${heroes.size} hero${if(saved) " favorit" else ""}"
                else "${gear.size} ${section.lowercase()}"
            )
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (!compactHeight) header()
        LazyVerticalGrid(
            GridCells.Adaptive(if (section == "Hero") 100.dp else 96.dp),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (compactHeight) item(span = { GridItemSpan(maxLineSpan) }) { header() }
            if (noResults)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        "Belum ada hasil",
                        if (saved) "Simpan hero lewat ikon bookmark di halaman hero."
                        else "Coba nama lain atau hapus filter.",
                        action = "Hapus filter",
                        onAction = {
                            query = ""
                            lane = "Semua"
                            category = "Semua"
                            saved = false
                        },
                    )
                }
            else if (section == "Hero")
                items(heroes, key = { it.id }) { hero ->
                    Column(
                        Modifier.clickable { onHero(hero.id) },
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box {
                            Artwork(
                                hero.portrait.ifBlank { hero.icon },
                                null,
                                Modifier.fillMaxWidth().aspectRatio(.82f),
                                round = 18.dp,
                                fallbackUrl = hero.icon,
                            )
                            FilledTonalIconButton(
                                { onAdd(hero.id, null) },
                                Modifier.align(Alignment.BottomEnd).size(48.dp),
                            ) {
                                Icon(Icons.Outlined.Add, "Rancang ${hero.name}")
                            }
                        }
                        Text(
                            hero.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Caption(hero.lanes.joinToString(" · "))
                    }
                }
            else
                items(gear, key = { it.id }) { item ->
                    Surface(
                        onClick = { onGear(item) },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            Modifier.padding(12.dp).heightIn(min = 134.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Artwork(item.icon, null, Modifier.size(62.dp), round = 14.dp)
                            Text(
                                item.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            item.price?.let { Caption("$it gold") }
                        }
                    }
                }
        }
    }
}
