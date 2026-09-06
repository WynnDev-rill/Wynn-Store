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
) {
    var section by rememberSaveable { mutableStateOf("Hero") }
    var query by rememberSaveable { mutableStateOf("") }
    var lane by rememberSaveable { mutableStateOf("Semua") }
    var category by rememberSaveable { mutableStateOf("Semua") }
    var saved by rememberSaveable { mutableStateOf(false) }
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
            "Item" -> catalog.items
            "Emblem" -> catalog.emblems
            else -> catalog.spells
        }
    val categories =
        listOf("Semua") + allGear.map { categoryLabel(it.category) }.distinct().sorted()
    val gear =
        remember(allGear, query, category) {
            val q = searchKey(query)
            allGear
                .filter {
                    (category == "Semua" || categoryLabel(it.category) == category) &&
                        q in searchKey(it.name + it.category + it.stats.joinToString())
                }
                .sortedWith(compareBy<Equipment> { it.category }.thenBy { it.name })
        }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
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
                listOf("Hero", "Item", "Emblem", "Spell"),
                section,
                {
                    section = it
                    query = ""
                    category = "Semua"
                },
            )
            if (section == "Hero") ChoiceRow(listOf("Semua") + lanes, lane, { lane = it })
            else if (categories.size > 2) ChoiceRow(categories, category, { category = it })
            Caption(
                if (section == "Hero") "${heroes.size} hero${if(saved) " favorit" else ""}"
                else "${gear.size} ${section.lowercase()}"
            )
        }
        if (section == "Hero" && heroes.isEmpty() || section != "Hero" && gear.isEmpty())
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
        else
            LazyVerticalGrid(
                GridCells.Adaptive(if (section == "Hero") 100.dp else 96.dp),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (section == "Hero")
                    items(heroes, key = { it.id }) { hero ->
                        Column(
                            Modifier.clickable { onHero(hero.id) },
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Artwork(
                                hero.portrait.ifBlank { hero.icon },
                                null,
                                Modifier.fillMaxWidth().aspectRatio(.82f),
                                round = 18.dp,
                            )
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
