package id.wynn.vocamemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val Indonesian = Locale.forLanguageTag("id-ID")

@Composable
internal fun StatisticsScreen(vm: VocaModel, memos: List<Memo>, reviews: List<Review>) {
    val zone = ZoneId.systemDefault()
    val date by produceState(LocalDate.now(zone), zone) {
        while (true) {
            val now = Instant.now()
            value = now.atZone(zone).toLocalDate()
            val nextDay = value.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            delay((nextDay - now.toEpochMilli()).coerceAtLeast(1))
        }
    }
    val number = remember { NumberFormat.getIntegerInstance(Indonesian) }
    val days = remember(reviews, date, zone) { weekActivity(reviews, date, zone) }
    val difficult = remember(memos) { memos.filter { it.lapses > 0 }.sortedByDescending { it.lapses }.take(10) }
    val mastered = remember(memos) { memos.count { it.mastered } }
    val lifetimeAccuracy = remember(reviews) { accuracy(reviews.count { it.correct }, reviews.size) }
    val today = days.last()
    LazyColumn(
        modifier = Modifier.testTag("statistics-list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionTitle("Target hari ini")
                    Text(
                        "${number.format(today.answers)} / ${number.format(vm.goal)} jawaban",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    LinearProgressIndicator(
                        progress = { (today.answers.toFloat() / vm.goal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .76f),
                    )
                    Text(
                        if (today.answers >= vm.goal) "Target tercapai"
                        else "${number.format(vm.goal - today.answers)} jawaban lagi",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Aktivitas 7 hari")
                Text(
                    "${number.format(days.sumOf { it.answers })} jawaban dalam 7 hari",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ActivityChart(days, number)
                if (reviews.isEmpty()) {
                    TextButton(onClick = { vm.page(if (memos.isEmpty()) "Kamus" else "Latihan") }) {
                        Text(if (memos.isEmpty()) "Cari kata untuk dilatih" else "Mulai latihan")
                    }
                }
            }
        }
        item {
            Column {
                SectionTitle("Ringkasan")
                StatLine("Kata tersimpan", number.format(memos.size))
                StatLine("Dikuasai", number.format(mastered))
                StatLine("Jawaban sepanjang waktu", number.format(reviews.size))
                StatLine("Akurasi sepanjang waktu", lifetimeAccuracy)
                StatLine("Akurasi hari ini", accuracy(today.correct, today.answers))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SectionTitle("Kata tersulit")
                Text(
                    if (difficult.isEmpty()) "Belum ada kata sulit tercatat." else "Berdasarkan jumlah jawaban yang perlu diulang.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(difficult, key = { it.id }) { memo ->
            StatLine("${memo.word} · ${memo.meaning}", "${number.format(memo.lapses)} kali")
        }
        item {
            var open by remember { mutableStateOf(false) }
            TextButton(onClick = { open = !open }) {
                Text(if (open) "Tutup cara hitung" else "Cara statistik dihitung")
            }
            if (open) {
                Text(
                    "Akurasi menghitung jawaban tepat dan penilaian Sulit, Ingat, atau Mudah. Daftar dan ulang mekanis tidak dihitung. Kata dikuasai ditandai di Memo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private data class DayActivity(val date: LocalDate, val answers: Int, val correct: Int)

private fun weekActivity(reviews: List<Review>, date: LocalDate, zone: ZoneId): List<DayActivity> {
    val start = date.minusDays(6)
    val first = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val last = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val totals = IntArray(7)
    val correct = IntArray(7)
    for (review in reviews) {
        if (review.time < first || review.time >= last) continue
        val day = Instant.ofEpochMilli(review.time).atZone(zone).toLocalDate()
        val index = (day.toEpochDay() - start.toEpochDay()).toInt()
        totals[index]++
        if (review.correct) correct[index]++
    }
    return List(7) { DayActivity(start.plusDays(it.toLong()), totals[it], correct[it]) }
}

private fun accuracy(correct: Int, total: Int): String =
    if (total > 0) "${correct.toLong() * 100 / total}%" else "Belum ada"

@Composable
private fun ActivityChart(days: List<DayActivity>, number: NumberFormat) {
    val colors = MaterialTheme.colorScheme
    val dayName = remember { DateTimeFormatter.ofPattern("EEE", Indonesian) }
    val fullDate = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Indonesian) }
    val peak = days.maxOf { it.answers }.coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            val amount = number.format(day.answers)
            val active = index == days.lastIndex
            Column(
                Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription = "${day.date.format(fullDate)}, $amount jawaban"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    amount,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant,
                )
                Box(Modifier.height(86.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.align(Alignment.BottomCenter))
                    val height = if (day.answers == 0) 2.dp else (86f * day.answers / peak).coerceAtLeast(5f).dp
                    Box(
                        Modifier.fillMaxWidth(.59f).widthIn(max = 28.dp).height(height)
                            .background(
                                when {
                                    day.answers == 0 -> colors.outlineVariant
                                    active -> colors.primary
                                    else -> colors.primary.copy(alpha = .56f)
                                },
                                RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                            )
                    )
                }
                Text(
                    day.date.format(dayName),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
    }
    HorizontalDivider()
}
