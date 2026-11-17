@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package id.wynn.arithmemo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.arithmemo.R
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.update.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HomeScreen(
    s: Snapshot,
    hasUpdate: Boolean,
    start: (StudyMode) -> Unit,
    resume: () -> Unit,
    updates: () -> Unit,
    library: () -> Unit,
) {
    val stats = remember(s.attempts) { Stats.from(s.attempts) }
    val today = stats.today()
    val due =
        s.progress.count {
            s.settings.config.matches(Fact.parse(it.key)) &&
                it.value.due <= System.currentTimeMillis()
        }
    PageColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ArithMemo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    LocalDate.now()
                        .format(
                            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("id"))
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Image(
                painterResource(R.drawable.brand_icon),
                contentDescription = "Ikon ArithMemo",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
            )
        }
        Heading("Sedikit setiap hari.", "Buat angka terasa akrab, satu hafalan sekali waktu.")
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TrackChanges, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Target hari ini", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${stats.streak()} hari", style = MaterialTheme.typography.labelLarge)
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "$today",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "/ ${s.settings.goal} jawaban",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                LinearProgressIndicator(
                    progress = { (today.toFloat() / s.settings.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .25f),
                )
                Text(
                    if (today >= s.settings.goal) "Target tercapai. Kerja bagus hari ini!"
                    else
                        "${(s.settings.goal-today).coerceAtLeast(0)} jawaban lagi. Pelan-pelan juga berarti maju.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (s.session?.let { !it.finished } != true)
            Button(
                onClick = { start(StudyMode.RECALL) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text("Mulai mengingat")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
            }
        s.session
            ?.takeIf { !it.finished }
            ?.let {
                ModeRow(
                    Icons.Outlined.PlayArrow,
                    "Lanjutkan sesi",
                    "${it.mode.label} · ${it.answered} jawaban tersimpan",
                    resume,
                )
            }
        if (hasUpdate)
            ModeRow(
                Icons.Outlined.SystemUpdate,
                "Pembaruan tersedia",
                "Lihat versi terbaru ArithMemo",
                updates,
            )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("${s.progress.count {it.value.mastered}}", "Dikuasai", Modifier.weight(1f))
            Metric(
                if (stats.total > 0) "${stats.accuracy}%" else "—",
                "Akurasi",
                Modifier.weight(1f),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Pilih ritme Anda",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                s.settings.config.operations.joinToString(" · ") { it.label } +
                    " · ${s.settings.config.digits} digit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeRow(
                Icons.Outlined.Psychology,
                "Latihan mengingat",
                "10 soal, tanpa tekanan waktu",
                { start(StudyMode.RECALL) },
            )
            ModeRow(
                Icons.Outlined.AutoAwesome,
                "Ulangi terjadwal",
                if (due > 0) "$due kartu jatuh tempo · lanjutkan hafalan"
                else "Kartu baru dan pengulangan adaptif",
                { start(StudyMode.ADAPTIVE) },
            )
            ModeRow(
                Icons.Outlined.Timer,
                "Tes kecepatan",
                "60 detik · seberapa lancar Anda?",
                { start(StudyMode.SPEED) },
            )
            ModeRow(
                Icons.Outlined.Shuffle,
                "Soal acak",
                "10 soal dari rentang pilihan Anda",
                { start(StudyMode.RANDOM) },
            )
        }
        OutlinedButton(
            onClick = library,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Icon(Icons.Outlined.Style, null)
            Spacer(Modifier.width(8.dp))
            Text("Buka ruang hafalan")
        }
        Text(
            "Tersimpan di perangkat Anda. Siap dipakai tanpa internet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
