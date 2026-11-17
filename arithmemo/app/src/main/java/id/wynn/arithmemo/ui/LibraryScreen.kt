@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package id.wynn.arithmemo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.update.*

@Composable
internal fun LibraryScreen(s: Snapshot, change: (UserSettings) -> Unit, start: () -> Unit) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val cfg = s.settings.config
    var cards by remember { mutableStateOf(emptyList<Fact>()) }
    LaunchedEffect(cfg) {
        cards =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                Facts.catalog(cfg)
            }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var index by rememberSaveable(cfg) { mutableIntStateOf(0) }
    var revealed by rememberSaveable(cfg, index) { mutableStateOf(false) }
    var page by rememberSaveable(cfg) { mutableIntStateOf(0) }
    LazyColumn(
        Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("factList"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Heading("Ruang hafalan", "Lihat, balik, lalu ingat kembali.") }
        item {
            OutlinedButton(
                onClick = { filtersOpen = !filtersOpen },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Outlined.Tune, null)
                Spacer(Modifier.width(8.dp))
                Text(if (filtersOpen) "Tutup pilihan" else "Atur pilihan · ${cfg.digits} digit")
            }
        }
        if (filtersOpen) item { Filters(s.settings, change) }
        item {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Daftar", "Flashcard").forEachIndexed { i, t ->
                    Tab(
                        selected = tab == i,
                        onClick = {
                            tab = i
                            filtersOpen = false
                        },
                        text = { Text(t) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        item {
            Text(
                "${cards.size} kartu pilihan · ${cfg.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (tab == 0) {
            items(cards.drop(page * 30).take(30), key = { it.key }) { f ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${f.prompt} = ${f.answer}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        val p = s.progress[f.key]
                        Text(
                            if (p?.mastered == true) "Dikuasai"
                            else if (p != null) "Dipelajari" else "Baru",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (p?.mastered == true) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { page-- }, enabled = page > 0) { Text("Sebelumnya") }
                    Text(
                        "${page+1} / ${((cards.size+29)/30).coerceAtLeast(1)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { page++ }, enabled = (page + 1) * 30 < cards.size) {
                        Text("Berikutnya")
                    }
                }
            }
        } else if (cards.isNotEmpty()) {
            val fact = cards[index.coerceAtMost(cards.lastIndex)]
            item {
                EquationCard(
                    fact,
                    if (revealed) fact.answer.toString() else "?",
                    if (revealed) "Baca dan ingat kembali" else "Pikirkan jawabannya",
                    revealed,
                )
            }
            item {
                Button(
                    onClick = { revealed = !revealed },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Outlined.FlipToFront, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (revealed) "Sembunyikan jawaban" else "Balik kartu")
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = {
                            index--
                            revealed = false
                        },
                        enabled = index > 0,
                    ) {
                        Text("Sebelumnya")
                    }
                    Text("${index+1} / ${cards.size}")
                    TextButton(
                        onClick = {
                            index++
                            revealed = false
                        },
                        enabled = index < cards.lastIndex,
                    ) {
                        Text("Berikutnya")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = start,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text("Latih dengan flashcard")
                }
            }
        }
        item {
            Text(
                "Daftar dan pratinjau tidak mengubah statistik. Mulai latihan untuk menjadwalkan hafalan. Pada 2–3 digit, katalog berisi hingga 240 pilihan per operasi; Soal acak memakai seluruh rentang.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
