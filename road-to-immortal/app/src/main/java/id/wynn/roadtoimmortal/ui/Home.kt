package id.wynn.roadtoimmortal.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*
import java.time.Instant

@Composable
fun HomeScreen(
    catalog: Catalog,
    prefs: UserPreferences,
    now: Instant,
    onRank: () -> Unit,
    onHero: (Int) -> Unit,
    onMeta: () -> Unit,
    onSettings: () -> Unit,
    onSources: () -> Unit,
) {
    val position = prefs.position
    val road = Road.target(position, catalog.season, now, rules = catalog.rankRules)
    val rank =
        if (position.tier in listOf("epic", "legend")) position.tier
        else if (position.tier == "mythic" && position.stars >= 50) "glory"
        else if (position.tier == "mythic" && position.stars >= 25) "honor" else "mythic"
    val slice = catalog.slice(rank)
    val recommendations =
        remember(catalog, rank) {
            slice
                ?.heroes
                .orEmpty()
                .filter { it.win != null && (it.pick ?: 0.0) >= .5 }
                .sortedByDescending(DraftEngine::metaScore)
                .take(4)
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Caption("ROAD TO IMMORTAL")
                    Text("Setiap bintang berarti.", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onSettings) { Icon(Icons.Outlined.Tune, "Pengaturan") }
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF222440), Color(0xFF353164))))
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(
                        Color(0xFFB9B7FF).copy(alpha = .13f),
                        size.width * .43f,
                        Offset(size.width * .96f, size.height * .17f),
                        style = Stroke(1.dp.toPx()),
                    )
                    drawCircle(
                        Color(0xFFE7BD70).copy(alpha = .12f),
                        size.width * .33f,
                        Offset(size.width * .96f, size.height * .17f),
                        style = Stroke(1.dp.toPx()),
                    )
                }
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TARGET HARI INI",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD3D1EF),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFFE7BD70))
                    }
                    if (!prefs.hasRank) {
                        Text(
                            "Immortal dimulai\ndari bintangmu.",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                        )
                        Text(
                            "Pilih rank saat ini untuk menemukan ritme menuju 100★.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD3D1EF),
                        )
                        Button(
                            onRank,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE7BD70),
                                    contentColor = Color(0xFF292135),
                                ),
                        ) {
                            Text("Mulai perjalanan")
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                null,
                                Modifier.size(18.dp),
                            )
                        }
                    } else {
                        AnimatedContent(road.daily, label = "Target bintang") { daily ->
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier =
                                    Modifier.semantics(mergeDescendants = true) {
                                        contentDescription =
                                            if (daily != null) "$daily net bintang hari ini"
                                            else "Target menunggu jadwal season"
                                    },
                            ) {
                                Text(
                                    daily?.let { if (it == 0) "100" else "+$it" } ?: "—",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = Color.White,
                                )
                                Text(
                                    if (daily == 0) "★ tercapai" else "★ net",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFE7BD70),
                                    modifier = Modifier.padding(bottom = 9.dp),
                                )
                            }
                        }
                        Text(
                            when {
                                road.remaining == 0 -> "Kamu sudah sampai di Mythical Immortal."
                                road.expired ->
                                    "Season berakhir. Target baru tersedia setelah jadwal diperbarui."
                                road.daily == null ->
                                    "Menunggu jadwal reset yang dapat diverifikasi."
                                else ->
                                    "${road.remaining} bintang lagi · ${road.days} hari bermain tersisa"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD3D1EF),
                        )
                        if (road.daily != null && road.remaining > 0) {
                            val progress by
                                animateFloatAsState(road.progress, label = "Bintang Mythic")
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                color = Color(0xFFE7BD70),
                                trackColor = Color.White.copy(alpha = .12f),
                                drawStopIndicator = {},
                            )
                            Text(
                                "Net = bintang bertambah setelah menang & kalah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD3D1EF),
                            )
                        }
                    }
                }
            }
        }
        if (prefs.hasRank)
            item {
                Surface(
                    onClick = onRank,
                    shape = Rounded,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Artwork(
                            catalog.ranks.firstOrNull { it.key == Road.badge(position) }?.icon,
                            null,
                            Modifier.size(64.dp),
                            ContentScale.Fit,
                        )
                        Column(Modifier.weight(1f)) {
                            Caption("RANK SAAT INI")
                            Text(
                                Road.label(position, catalog.rankRules),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${position.stars} ★",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Icon(
                            Icons.Outlined.Edit,
                            "Ubah rank dan bintang",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        if (prefs.hasRank && prefs.lastSeason != null && prefs.lastSeason != catalog.season.number)
            item {
                Surface(
                    onClick = onRank,
                    shape = Rounded,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        "Season berganti. Sesuaikan rank setelah reset.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        item {
            Surface(
                onClick = onSources,
                shape = Rounded,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Season ${catalog.season.number?:"—"}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Caption(Road.countdown(catalog.season, now))
                    }
                    Tag("Komunitas", MaterialTheme.colorScheme.secondary)
                }
            }
        }
        if (recommendations.isNotEmpty()) {
            item {
                SectionTitle("Pilihan untuk ranked", "Lihat meta", onMeta)
                Caption("${scopes[rank]} · statistik ${slice?.days} hari")
            }
            items(recommendations, key = { it.heroId }) { m ->
                catalog.heroById[m.heroId]?.let { h ->
                    HeroRow(h, h.lanes.joinToString(" · "), rate(m.win)) { onHero(h.id) }
                }
            }
            item { slice?.let { SourceLine(it.source, onSources) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankPicker(
    catalog: Catalog,
    initial: RankPosition,
    onChange: (RankPosition) -> Unit,
    onDismiss: () -> Unit,
) {
    var position by remember { mutableStateOf(initial) }
    var editing by remember { mutableStateOf(false) }
    var digits by remember { mutableStateOf(initial.stars.toString()) }
    val haptic = LocalHapticFeedback.current
    val rule = catalog.rankRules.tiers.firstOrNull { it.key == position.tier }
    val max = rule?.starsPerDivision ?: 9999
    fun update(next: RankPosition) {
        val normalized = Road.normalize(next, catalog.rankRules)
        if (normalized != position) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        position = normalized
        digits = position.stars.toString()
        onChange(position)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Di mana posisimu?", style = MaterialTheme.typography.headlineMedium)
                Caption("Target langsung menyesuaikan bintangmu.")
            }
            item {
                val keys =
                    listOf(
                        "warrior",
                        "elite",
                        "master",
                        "grandmaster",
                        "epic",
                        "legend",
                        "mythic",
                        "honor",
                        "glory",
                        "immortal",
                    )
                val state =
                    rememberLazyListState(
                        initialFirstVisibleItemIndex =
                            (keys.indexOf(Road.badge(position)) - 1).coerceAtLeast(0)
                    )
                LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(keys) { key ->
                        val selected = Road.badge(position) == key
                        Surface(
                            onClick = {
                                val stars =
                                    when (key) {
                                        "honor" -> 25
                                        "glory" -> 50
                                        "immortal" -> 100
                                        else -> 0
                                    }
                                update(
                                    RankPosition(
                                        if (key in listOf("honor", "glory", "immortal")) "mythic"
                                        else key,
                                        1,
                                        stars,
                                    )
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            color =
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.width(96.dp).semantics { this.selected = selected },
                        ) {
                            Column(
                                Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Artwork(
                                    catalog.ranks.firstOrNull { it.key == key }?.icon,
                                    null,
                                    Modifier.size(72.dp),
                                    ContentScale.Fit,
                                )
                                Text(
                                    key.replaceFirstChar(Char::titlecase),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    Road.label(position, catalog.rankRules),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (rule != null)
                item {
                    ChoiceRow(
                        (rule.divisions downTo 1).map(Road::roman),
                        Road.roman(position.division),
                        { s ->
                            update(
                                position.copy(
                                    division = (1..rule.divisions).first { Road.roman(it) == s }
                                )
                            )
                        },
                    )
                }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FilledTonalIconButton(
                        { update(position.copy(stars = position.stars - 1)) },
                        enabled = position.stars > 0,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(Icons.Outlined.Remove, "Kurangi satu bintang")
                    }
                    if (editing)
                        OutlinedTextField(
                            digits,
                            { s ->
                                if (s.length <= 4 && s.all(Char::isDigit)) {
                                    if (s.isEmpty()) digits = s
                                    else s.toIntOrNull()?.let { update(position.copy(stars = it)) }
                                }
                            },
                            label = { Text("Bintang") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(140.dp),
                        )
                    else
                        TextButton({ editing = true }) {
                            Text(
                                "${position.stars} ★",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                        }
                    FilledTonalIconButton(
                        { update(position.copy(stars = position.stars + 1)) },
                        enabled = position.stars < max,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(Icons.Outlined.Add, "Tambah satu bintang")
                    }
                }
            }
            if (rule == null && position.stars <= 100)
                item {
                    Slider(
                        position.stars.toFloat(),
                        { update(position.copy(stars = it.toInt())) },
                        valueRange = 0f..100f,
                        steps = 99,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Bintang Mythic, 0 sampai 100"
                            },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Caption("Mythic · 0★")
                        Caption("Immortal · 100★")
                    }
                }
            if (rule != null)
                item {
                    Caption(
                        "Tangga bintang dasar hingga Mythic, lalu 100★. Bonus, proteksi, dan hasil placement dapat mempercepat perjalanan."
                    )
                }
            item {
                Button(onDismiss, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Selesai") }
            }
        }
    }
}
