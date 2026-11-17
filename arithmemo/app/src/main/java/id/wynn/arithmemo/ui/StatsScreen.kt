@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package id.wynn.arithmemo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.update.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun StatsScreen(s: Snapshot) {
    val stats = remember(s.attempts) { Stats.from(s.attempts) }
    val now = System.currentTimeMillis()
    val due = s.progress.count { it.value.due <= now }
    val mastered = s.progress.count { it.value.mastered }
    PageColumn {
        Heading("Perkembangan Anda", "Kemajuan kecil tetap layak dicatat.")
        if (stats.total == 0)
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Outlined.Insights,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Perjalanan Anda dimulai di sini",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Selesaikan satu latihan. Akurasi, kecepatan, dan penguasaan Anda akan muncul di sini."
                    )
                }
            }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("${stats.total}", "Total jawaban", Modifier.weight(1f))
            Metric(
                if (stats.total == 0) "—" else "${stats.accuracy}%",
                "Akurasi",
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(
                if (stats.averageMs == 0L) "—"
                else
                    String.format(
                        Locale.forLanguageTag("id"),
                        "%.1f dtk",
                        stats.averageMs / 1000.0,
                    ),
                "Rata-rata benar",
                Modifier.weight(1f),
            )
            Metric("${stats.streak()}", "Hari beruntun", Modifier.weight(1f))
        }
        Text(
            "Penguasaan hafalan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatLine("Dikuasai", mastered.toString())
                StatLine("Sedang dipelajari", (s.progress.size - mastered).toString())
                StatLine("Jatuh tempo", due.toString())
                Text(
                    "Dikuasai = minimal 3 jawaban berhasil berturut-turut dan interval pengulangan minimal 7 hari. Penilaian flashcard dihitung sebagai laporan ingatan; waktu hanya dari jawaban ketik yang benar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "7 hari terakhir",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
                val max =
                    days.maxOfOrNull { stats.byDay[it] ?: 0 }?.coerceAtLeast(s.settings.goal)
                        ?: s.settings.goal
                days.forEach { day ->
                    val count = stats.byDay[day] ?: 0
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                    ) {
                        StatLine(
                            if (day == LocalDate.now()) "Hari ini"
                            else
                                day.format(
                                    DateTimeFormatter.ofPattern(
                                        "EEE, d MMM",
                                        Locale.forLanguageTag("id"),
                                    )
                                ),
                            "$count jawaban",
                        )
                        LinearProgressIndicator(
                            progress = { count.toFloat() / max },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    }
                }
            }
        }
        Text(
            "Per operasi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Operation.entries.forEach { op ->
            val attempts = s.attempts.filter { it.fact.op == op }
            val a = Stats.from(attempts)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatLine(
                        op.label,
                        if (a.total == 0) "Belum dilatih" else "${a.accuracy}% benar",
                    )
                    Text(
                        "${a.total} jawaban · ${s.progress.count {Fact.parse(it.key).op==op && it.value.mastered}} kartu dikuasai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
