package id.wynn.roadtoimmortal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import id.wynn.roadtoimmortal.data.*

@Composable
private fun BackTitle(title: String, back: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali") }
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    onTheme: (String) -> Unit,
    onBack: () -> Unit,
    onSources: () -> Unit,
) {
    val uri = LocalUriHandler.current
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { BackTitle("Ruangmu", onBack) }
        item {
            SectionTitle("Tampilan")
            Caption("Pilih suasana untuk perjalananmu.")
        }
        item {
            ChoiceRow(
                listOf("Sistem", "Terang", "Gelap"),
                when (prefs.theme) {
                    "light" -> "Terang"
                    "dark" -> "Gelap"
                    else -> "Sistem"
                },
                {
                    onTheme(
                        when (it) {
                            "Terang" -> "light"
                            "Gelap" -> "dark"
                            else -> "system"
                        }
                    )
                },
            )
        }
        item {
            Surface(
                onClick = onSources,
                shape = Rounded,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CloudSync, null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Data & sumber", style = MaterialTheme.typography.titleMedium)
                        Caption("Kesegaran data, jadwal season, dan pembaruan")
                    }
                    Icon(Icons.Outlined.ChevronRight, null)
                }
            }
        }
        item {
            HorizontalDivider()
            SectionTitle("Road to Immortal")
            Text(
                "Companion ranked MLBB, dibuat untuk membantu setiap pilihan terasa lebih jelas.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item {
            Caption(
                "Versi ${id.wynn.roadtoimmortal.BuildConfig.VERSION_NAME}\nAplikasi komunitas independen oleh Wynn. Tidak berafiliasi dengan atau didukung Moonton. Nama dan artwork MLBB milik pemegang haknya."
            )
        }
        item {
            Text("Privasi", style = MaterialTheme.typography.titleMedium)
            Caption(
                "Tanpa akun, iklan, atau analitik. Rank dan favorit disimpan di perangkat. Permintaan data dan gambar dikirim ke penyedia dan CDN; kebijakan mereka berlaku. Pencadangan Android mengikuti pengaturan perangkatmu."
            )
        }
        item {
            Text("Kredit", style = MaterialTheme.typography.titleMedium)
            Caption(
                "Hero dan statistik: MLBB / Moonton GMS. Adapter Academy: Rone Arena (BSD-3-Clause). Item dan jadwal: MLBBHub, dengan sumber fakta Liquipedia. Android Jetpack, Kotlin, OkHttp, dan Coil mendukung aplikasi ini."
            )
        }
        item {
            OutlinedButton({
                runCatching {
                    uri.openUri(
                        "https://github.com/WynnDev-rill/Wynn-Store/tree/main/road-to-immortal"
                    )
                }
            }) {
                Text("Source & lisensi")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SourcesScreen(
    catalog: Catalog,
    onBack: () -> Unit,
    refresh: () -> Unit,
    refreshing: Boolean,
    error: String?,
) {
    val uri = LocalUriHandler.current
    val sources =
        remember(catalog) {
            listOf("Hero" to catalog.source, "Jadwal season" to catalog.season.source) +
                catalog.meta.map { "Meta ${scopes[it.rank]} · ${it.days} hari" to it.source } +
                (catalog.items + catalog.emblems + catalog.spells)
                    .groupBy { it.source.name + it.category }
                    .values
                    .map { rows ->
                        rows.first().category to rows.minBy { it.source.updatedAt.orEmpty() }.source
                    } +
                catalog.builds.take(1).map { "Build Mythic" to it.source }
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { BackTitle("Data & sumber", onBack) }
        item {
            Surface(shape = Rounded, color = MaterialTheme.colorScheme.primaryContainer) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "${catalog.heroes.size} hero · ${catalog.items.size} item",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "${catalog.builds.map {it.heroId}.distinct().size} hero dengan build · ${catalog.meta.size} cakupan rank",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Caption("Snapshot disusun ${dateText(catalog.generatedAt)}")
                    Button(refresh, enabled = !refreshing) {
                        Text(if (refreshing) "Memperbarui…" else "Perbarui sekarang")
                    }
                }
            }
        }
        if (error != null)
            item {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        item {
            Text(
                "Season ${catalog.season.number?:"—"}",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Reset: ${dateText(catalog.season.resetsAt)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Caption(
                "Jam mengikuti zona waktu perangkat. Jadwal komunitas dapat berubah. Jika kedaluwarsa atau tak tersedia, target harian menunggu jadwal yang terverifikasi."
            )
        }
        item {
            Caption(
                "Target dibagi ke hari kalender yang masih bisa dimainkan, termasuk sisa hari ini. Target sebelum Mythic memakai tangga bintang dasar; bonus dan placement tidak diprediksi."
            )
        }
        if (catalog.patch.version.isNotBlank())
            item {
                Text("Patch ${catalog.patch.version}", style = MaterialTheme.typography.titleMedium)
                Caption(
                    "Label katalog komunitas · ${dateText(catalog.patch.updatedAt)}. Tiap bagian data dapat memiliki waktu revisi berbeda."
                )
            }
        item {
            Text("Diperbarui ≠ diperiksa", style = MaterialTheme.typography.titleLarge)
            Caption(
                "Diperbarui adalah waktu revisi sumber. Diperiksa adalah saat pipeline mengambil data. Statistik mengikuti jendela pertandingan sumber; bukan pertandingan langsung. Snapshot diperiksa setiap enam jam; item dan build paling sering sekali sehari."
            )
        }
        items(sources) { (label, source) ->
            Surface(shape = Rounded, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        source.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Caption(
                        "Revisi · ${dateText(source.updatedAt)}\nDiperiksa · ${dateText(source.checkedAt)}"
                    )
                    if (source.note.isNotBlank()) Caption(source.note)
                    TextButton(
                        {
                            if (source.url.startsWith("https://"))
                                runCatching { uri.openUri(source.url) }
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("Buka sumber")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(14.dp))
                    }
                }
            }
        }
        item {
            Caption(
                "Jika penyedia gagal, aplikasi mempertahankan snapshot terakhir. Gambar yang sudah dibuka disimpan di cache; gambar lain memerlukan koneksi."
            )
        }
    }
}
