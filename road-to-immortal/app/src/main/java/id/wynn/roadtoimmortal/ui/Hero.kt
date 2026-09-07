package id.wynn.roadtoimmortal.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.roadtoimmortal.data.*

@Composable
fun HeroScreen(
    hero: Hero,
    catalog: Catalog,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onBack: () -> Unit,
    onHero: (Int) -> Unit,
    onGear: (Equipment) -> Unit,
    onSources: () -> Unit,
    onPool: () -> Unit,
    onParty: () -> Unit,
) {
    var section by rememberSaveable(hero.id) { mutableStateOf("Build") }
    var rank by rememberSaveable { mutableStateOf("mythic") }
    val meta = catalog.slice(rank)?.heroes?.firstOrNull { it.heroId == hero.id }
    val builds = catalog.builds.filter { it.heroId == hero.id }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                Artwork(
                    hero.portrait,
                    null,
                    Modifier.fillMaxSize(),
                    ContentScale.Crop,
                    0.dp,
                    hero.icon,
                )
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = .25f),
                                    Color.Transparent,
                                    Color(0xFF151729),
                                )
                            )
                        )
                )
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FilledIconButton(
                        onBack,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xCC151729),
                                contentColor = Color.White,
                            ),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali")
                    }
                    FilledIconToggleButton(
                        favorite,
                        { onFavorite() },
                        colors =
                            IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = Color(0xCC151729),
                                contentColor = Color.White,
                                checkedContainerColor = Color(0xFFE7BD70),
                                checkedContentColor = Color(0xFF151729),
                            ),
                    ) {
                        Icon(
                            if (favorite) Icons.Outlined.BookmarkAdded
                            else Icons.Outlined.BookmarkBorder,
                            if (favorite) "Hapus dari favorit" else "Simpan hero",
                        )
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomStart).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        hero.name,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Text(
                        (hero.roles + hero.lanes).distinct().joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD3D1EF),
                    )
                }
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onPool, Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Text("Rancang hero")
                }
                OutlinedButton(onParty, Modifier.weight(1f)) { Text("Cari partner") }
            }
        }
        item {
            ChoiceRow(
                listOf("Build", "Skill", "Matchup", "Profil"),
                section,
                { section = it },
                Modifier.padding(horizontal = 20.dp),
            )
        }
        when (section) {
            "Build" -> {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        SectionTitle("Siap masuk ranked")
                        Caption("Mythic · kombinasi item inti berdasarkan pertandingan")
                    }
                }
                if (builds.isEmpty())
                    item {
                        EmptyState(
                            "Build belum tersedia",
                            "Sumber belum menerbitkan build ranked hero ini. Detail item dan skill tetap tersedia.",
                            Icons.Outlined.Inventory2,
                        )
                    }
                items(builds) { build ->
                    Surface(
                        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        shape = Rounded,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    build.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Tag(build.lane)
                            }
                            GearRow(
                                build.items.mapNotNull(catalog.equipmentById::get),
                                onGear,
                                60.dp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Column {
                                    Caption("Win rate")
                                    Text(
                                        rate(build.winRate),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Column {
                                    Caption("Pick rate")
                                    Text(
                                        rate(build.pickRate),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                            }
                            val prep =
                                (listOfNotNull(build.spell) + build.emblems).mapNotNull(
                                    catalog.equipmentById::get
                                )
                            if (prep.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Caption("SPELL · EMBLEM · TALENT")
                                GearRow(prep, onGear)
                            }
                            SourceLine(build.source, onSources)
                        }
                    }
                }
                if (builds.isNotEmpty())
                    item {
                        Caption(
                            "Tiga item inti. Lengkapi sesuai lawan dan kebutuhan tim.",
                            Modifier.padding(horizontal = 20.dp),
                        )
                    }
            }
            "Skill" -> {
                items(hero.skills, key = { it.id }) { skill ->
                    var expanded by rememberSaveable(hero.id, skill.id) { mutableStateOf(false) }
                    Surface(
                        onClick = { expanded = !expanded },
                        shape = Rounded,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        Column(
                            Modifier.padding(16.dp).animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Artwork(skill.icon, null, Modifier.size(48.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(skill.name, style = MaterialTheme.typography.titleMedium)
                                    if (skill.tags.isNotEmpty())
                                        Caption(skill.tags.joinToString(" · "))
                                }
                                Icon(
                                    if (expanded) Icons.Outlined.ExpandLess
                                    else Icons.Outlined.ExpandMore,
                                    if (expanded) "Tutup skill" else "Baca skill",
                                )
                            }
                            if (expanded) {
                                Text(skill.description, style = MaterialTheme.typography.bodyMedium)
                                if (skill.cooldown.isNotBlank()) Caption(skill.cooldown)
                            }
                        }
                    }
                }
                if (hero.skills.isEmpty())
                    item {
                        EmptyState(
                            "Skill belum diberikan sumber",
                            "Periksa pembaruan setelah hero dirilis.",
                        )
                    }
            }
            "Matchup" -> {
                item {
                    Row(
                        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Baca lawanmu", style = MaterialTheme.typography.titleLarge)
                        ScopePicker(rank, { rank = it })
                    }
                }
                if (meta != null) {
                    listOf(
                            Triple(
                                "Waspadai",
                                "Win rate hero lawan saat menghadapi ${hero.name}",
                                meta.counters,
                            ),
                            Triple(
                                "Lebih menguntungkan",
                                "Win rate ${hero.name} menghadapi hero di bawah",
                                meta.strongAgainst,
                            ),
                            Triple("Sinergi", "Win rate pasangan dalam tim yang sama", meta.synergy),
                        )
                        .forEachIndexed { group, (title, caption, edges) ->
                            item {
                                Column(Modifier.padding(horizontal = 20.dp)) {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Caption(caption)
                                }
                            }
                            items(edges.take(5), key = { "$group-${it.heroId}" }) { edge ->
                                catalog.heroById[edge.heroId]?.let { h ->
                                    Box(Modifier.padding(horizontal = 20.dp)) {
                                        HeroRow(
                                            h,
                                            if (group == 2) "Bersama ${hero.name}"
                                            else "${hero.name} vs ${h.name}",
                                            rate(
                                                if (group == 1) edge.winRate?.let { 100 - it }
                                                else edge.winRate
                                            ),
                                        ) {
                                            onHero(h.id)
                                        }
                                    }
                                }
                            }
                        }
                    item {
                        Caption(
                            "Cakupan ${catalog.slice(rank)?.days} hari. Ukuran sampel pasangan tidak dipublikasikan. Matchup menggambarkan hasil pertandingan, bukan hasil duel lane.",
                            Modifier.padding(horizontal = 20.dp),
                        )
                    }
                } else
                    item {
                        EmptyState(
                            "Statistik matchup belum tersedia",
                            "Coba rank lain. Panduan hero tetap tersedia di bawah.",
                        )
                    }
                listOf(
                        "Counter dari panduan" to hero.counters,
                        "Sinergi dari panduan" to hero.synergy,
                    )
                    .forEach { (title, relation) ->
                        if (relation.ids.isNotEmpty()) {
                            item {
                                Column(
                                    Modifier.padding(horizontal = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    if (relation.description.isNotBlank())
                                        Text(
                                            relation.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                }
                            }
                            items(relation.ids, key = { "$title-$it" }) { id ->
                                catalog.heroById[id]?.let { h ->
                                    Box(Modifier.padding(horizontal = 20.dp)) {
                                        HeroRow(h, h.roles.joinToString(" · ")) { onHero(h.id) }
                                    }
                                }
                            }
                        }
                    }
            }
            else ->
                item {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SectionTitle("Tentang ${hero.name}")
                        if (hero.speciality.isNotEmpty())
                            Text(
                                hero.speciality.joinToString(" · "),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        Caption("Kesulitan mekanik dari sumber: ${hero.difficulty}/100")
                        LinearProgressIndicator(
                            progress = { hero.difficulty / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (hero.story.isNotBlank())
                            Text(hero.story, style = MaterialTheme.typography.bodyLarge)
                        Caption("Data hero · ${dateText(hero.updatedAt)}")
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentSheet(
    item: Equipment,
    catalog: Catalog,
    onDismiss: () -> Unit,
    onGear: (Equipment) -> Unit,
) {
    val uri = LocalUriHandler.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Artwork(item.icon, null, Modifier.size(76.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.headlineMedium)
                        Caption(categoryLabel(item.category))
                        item.price?.let { Tag("$it gold", MaterialTheme.colorScheme.secondary) }
                    }
                }
            }
            if (item.stats.isNotEmpty())
                item {
                    Surface(
                        shape = Rounded,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item.stats.forEach {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            if (item.description.isNotBlank())
                item { Text(item.description, style = MaterialTheme.typography.bodyLarge) }
            items(item.effects) { effect ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (effect.name.isNotBlank())
                        Text(effect.name, style = MaterialTheme.typography.titleMedium)
                    Text(effect.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (item.recipe.isNotEmpty())
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Komponen")
                        GearRow(item.recipe.mapNotNull(catalog.equipmentById::get), onGear)
                    }
                }
            if (item.stats.isEmpty() && item.description.isBlank() && item.effects.isEmpty())
                item {
                    Caption(
                        "Sumber baru menyediakan identitas. Atribut akan muncul saat katalog sumber melengkapinya."
                    )
                }
            item {
                HorizontalDivider()
                Caption(
                    "${item.source.name}\nDiperbarui ${dateText(item.source.updatedAt)}\nDiperiksa ${dateText(item.source.checkedAt)}"
                )
                if (item.source.note.isNotBlank()) Caption(item.source.note)
            }
            item {
                OutlinedButton({
                    if (item.source.url.startsWith("https://"))
                        runCatching { uri.openUri(item.source.url) }
                }) {
                    Text("Buka sumber")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
                }
            }
        }
    }
}
