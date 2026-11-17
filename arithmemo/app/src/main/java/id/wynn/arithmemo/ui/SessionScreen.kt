@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package id.wynn.arithmemo.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.update.*

@Composable
internal fun SessionScreen(
    s: StudySession,
    busy: Boolean,
    vm: MemoViewModel,
    pause: () -> Unit,
    finish: () -> Unit,
    haptics: Boolean,
    snackbar: SnackbarHostState,
) {
    val feedback = LocalHapticFeedback.current
    LaunchedEffect(s.feedback) {
        if (s.feedback != null && haptics)
            feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(s.mode.label, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = pause) { Icon(Icons.Outlined.Close, "Jeda sesi") }
                },
                actions = {
                    Text(
                        if (s.mode == StudyMode.SPEED) "${(s.remainingMs+999)/1000} dtk"
                        else "${(s.index+1).coerceAtMost(s.cards.size)} / ${s.cards.size}",
                        modifier = Modifier.padding(end = 20.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (s.finished) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Outlined.TaskAlt,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Heading(
                        "Satu langkah lebih lancar.",
                        "Semua jawaban sudah tersimpan di perangkat.",
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("${s.correct}/${s.answered}", "Benar", Modifier.weight(1f))
                        Metric(
                            if (s.answered > 0) "${s.correct*100/s.answered}%" else "—",
                            "Akurasi sesi",
                            Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Text(
                        if (s.answered == 0)
                            "Belum ada jawaban. Anda dapat mencoba lagi kapan saja."
                        else "Kartu yang sulit akan muncul lebih cepat pada pengulangan terjadwal.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Button(
                        onClick = finish,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        enabled = !busy,
                    ) {
                        Text("Kembali ke beranda")
                    }
                }
            } else {
                val f = s.current
                if (f != null) {
                    item {
                        LinearProgressIndicator(
                            progress = {
                                if (s.mode == StudyMode.SPEED) 1 - s.remainingMs / 60000f
                                else s.index.toFloat() / s.cards.size
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    }
                    item {
                        EquationCard(
                            f,
                            if (s.revealed) f.answer.toString() else "?",
                            if (s.mode == StudyMode.FLASHCARD)
                                "Ingat dahulu sebelum membuka jawaban"
                            else "Temukan jawabannya dari ingatan",
                            s.revealed,
                        )
                    }
                    if (s.feedback != null) {
                        item {
                            Surface(
                                color =
                                    if (s.feedback) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.large,
                                modifier =
                                    Modifier.fillMaxWidth().semantics {
                                        liveRegion = LiveRegionMode.Polite
                                    },
                            ) {
                                Column(
                                    Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        if (s.feedback) "Benar, sudah tersimpan."
                                        else "Belum tepat. Mari ingat lagi.",
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text("${f.prompt} = ${f.answer}")
                                    if (!s.feedback && s.mode != StudyMode.FLASHCARD)
                                        Text(
                                            "Jawaban Anda: ${s.typed}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = vm::next,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                enabled = !busy,
                            ) {
                                Text(if (s.index == s.cards.lastIndex) "Lihat hasil" else "Lanjut")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                            }
                        }
                    } else if (s.mode == StudyMode.FLASHCARD) {
                        if (!s.revealed)
                            item {
                                Button(
                                    onClick = vm::reveal,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    enabled = !busy,
                                ) {
                                    Text("Buka jawaban")
                                }
                            }
                        else {
                            item {
                                Text(
                                    "Seberapa mudah Anda mengingatnya?",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Rating.entries.chunked(2).forEach { ratings ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            ratings.forEach { rating ->
                                                OutlinedButton(
                                                    onClick = { vm.answer(rating) },
                                                    enabled = !busy,
                                                    modifier =
                                                        Modifier.weight(1f).heightIn(min = 56.dp),
                                                ) {
                                                    Text(rating.label)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = s.typed,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Jawaban Anda") },
                                placeholder = { Text("Gunakan papan angka") },
                                modifier = Modifier.fillMaxWidth().testTag("answerInput"),
                                textStyle = MaterialTheme.typography.headlineSmall,
                                singleLine = true,
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                        listOf("1", "2", "3"),
                                        listOf("4", "5", "6"),
                                        listOf("7", "8", "9"),
                                        listOf("hapus", "0", "periksa"),
                                    )
                                    .forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            row.forEach { key ->
                                                val label =
                                                    when (key) {
                                                        "hapus" -> "Hapus angka terakhir"
                                                        "periksa" -> "Periksa jawaban"
                                                        else -> "Angka $key"
                                                    }
                                                FilledTonalButton(
                                                    onClick = {
                                                        when (key) {
                                                            "hapus" -> vm.input(s.typed.dropLast(1))
                                                            "periksa" -> vm.answer()
                                                            else -> vm.input(s.typed + key)
                                                        }
                                                    },
                                                    enabled =
                                                        !busy &&
                                                            (key != "periksa" ||
                                                                s.typed.isNotEmpty()),
                                                    modifier =
                                                        Modifier.weight(1f)
                                                            .heightIn(min = 56.dp)
                                                            .semantics {
                                                                contentDescription = label
                                                            },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors =
                                                        if (key == "periksa")
                                                            ButtonDefaults.filledTonalButtonColors(
                                                                containerColor =
                                                                    MaterialTheme.colorScheme
                                                                        .primary,
                                                                contentColor =
                                                                    MaterialTheme.colorScheme
                                                                        .onPrimary,
                                                            )
                                                        else
                                                            ButtonDefaults.filledTonalButtonColors(),
                                                ) {
                                                    when (key) {
                                                        "hapus" ->
                                                            Icon(
                                                                Icons.AutoMirrored.Outlined
                                                                    .Backspace,
                                                                null,
                                                            )
                                                        "periksa" ->
                                                            Icon(Icons.Outlined.Check, null)
                                                        else ->
                                                            Text(
                                                                key,
                                                                style =
                                                                    MaterialTheme.typography
                                                                        .titleLarge,
                                                            )
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    if (s.mode == StudyMode.SPEED)
                        item {
                            Text(
                                "Waktu dijeda saat Anda meninggalkan sesi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            }
        }
    }
}
