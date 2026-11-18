package id.wynn.vocamemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun ProfileScreen(
    vm: VocaModel,
    memos: List<Memo>,
    reviews: List<Review>,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
) {
    val date = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val activity = remember(reviews, date, zone) {
        reviews.groupingBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() }.eachCount()
    }
    val todayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val today = remember(reviews, todayStart) { reviews.filter { it.time >= todayStart } }
    val mastered = memos.count { it.mastered }
    val accuracy = if (reviews.isEmpty()) "—" else "${reviews.count { it.correct } * 100 / reviews.size}%"
    val difficult = remember(memos) { memos.filter { it.lapses > 0 }.sortedByDescending { it.lapses }.take(10) }
    val colors = MaterialTheme.colorScheme
    val profile = vm.googleProfile
    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier.size(54.dp).background(colors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (profile == null) Icon(Icons.Rounded.Person, null, tint = colors.primary)
                            else Text(profile.name.firstOrNull()?.uppercase().orEmpty(),
                                style = MaterialTheme.typography.titleLarge, color = colors.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(profile?.name ?: "Tamu", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold)
                            Text(profile?.email ?: "Profil di perangkat ini", color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (profile == null) TextButton(onClick = onLogin) { Text("Masuk dengan Google") }
                    else TextButton(onClick = onSignOut) { Text("Keluar dari Google") }
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer)))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("Target hari ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("${today.size} / ${vm.goal} jawaban", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(
                    progress = { (today.size.toFloat() / vm.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    trackColor = colors.surface.copy(alpha = .64f),
                )
                TextButton(onClick = { vm.page("Latihan") }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(if (today.size >= vm.goal) "Target tercapai · Latihan lagi" else "Mulai latihan")
                }
            }
        }
        item {
            Surface(color = colors.surface, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Aktivitas 7 hari", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    val days = (6 downTo 0).map { date.minusDays(it.toLong()) }
                    val maxCount = days.maxOf { activity[it] ?: 0 }.coerceAtLeast(1)
                    val locale = Locale.forLanguageTag("id-ID")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        days.forEach { day ->
                            val n = activity[day] ?: 0
                            val name = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                            Column(
                                Modifier.weight(1f).semantics(mergeDescendants = true) {
                                    contentDescription = "$name, $n jawaban"
                                },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(Modifier.fillMaxWidth(.6f).height(56.dp), contentAlignment = Alignment.BottomCenter) {
                                    Box(Modifier.fillMaxWidth().height((56 * n.toFloat() / maxCount).dp.coerceAtLeast(3.dp))
                                        .background(if (day == date) colors.primary else colors.primaryContainer,
                                            RoundedCornerShape(6.dp)))
                                }
                                Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                    color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(color = colors.surface, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Progres belajar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    ProfileRow("Kata tersimpan", memos.size.toString())
                    ProfileRow("Dikuasai", mastered.toString())
                    ProfileRow("Jawaban sepanjang waktu", reviews.size.toString())
                    ProfileRow("Akurasi sepanjang waktu", accuracy)
                    ProfileRow("Akurasi hari ini", if (today.isEmpty()) "—" else "${today.count { it.correct } * 100 / today.size}%")
                }
            }
        }
        item {
            Text("Pencapaian", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            val badges = listOf(
                Triple("Kata pertama", memos.isNotEmpty(), Icons.Rounded.Bookmark),
                Triple("Target harian", today.size >= vm.goal, Icons.Rounded.EmojiEvents),
                Triple("5 kata dikuasai", mastered >= 5, Icons.Rounded.CheckCircle),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                badges.forEach { (label, earned, icon) ->
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = if (earned) colors.tertiaryContainer else colors.surfaceContainer) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(if (earned) icon else Icons.Rounded.Lock, null, Modifier.size(20.dp))
                            Text(label, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(if (earned) "Tercapai" else "Belum tercapai",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            Text("Perlu diulang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            if (difficult.isEmpty()) Text("Belum ada kata yang perlu perhatian.",
                color = colors.onSurfaceVariant)
        }
        items(difficult, key = { it.id }) { memo ->
            Surface(shape = MaterialTheme.shapes.medium, color = colors.surface) {
                Box(Modifier.padding(horizontal = 14.dp)) {
                    ProfileRow("${memo.word} · ${memo.meaning}", "${memo.lapses} kali")
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    }
}
