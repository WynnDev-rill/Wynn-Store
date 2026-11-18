package id.wynn.vocamemo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@Composable
internal fun PracticeHome(vm: VocaModel, memos: List<Memo>) {
    var reverse by rememberSaveable { mutableStateOf(false) }
    var group by rememberSaveable { mutableStateOf("Semua") }
    val groupMemos = memos.filter { !it.mastered && (group == "Semua" || it.groupName == group) }
    val due = groupMemos.count { it.due <= System.currentTimeMillis() }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SectionTitle("Pengulangan terjadwal")
                    Text(
                        "$due kata siap diulang",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = { vm.start("Spaced repetition", reverse, group) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Mulai pengulangan adaptif") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Arah latihan")
                FlowRowCompat {
                    FilterChip(!reverse, { reverse = false }, label = { Text("Inggris → Indonesia") })
                    FilterChip(reverse, { reverse = true }, label = { Text("Indonesia → Inggris") })
                }
                GroupPicker(memos.map { it.groupName }.distinct().sorted(), group) { group = it }
            }
        }
        item {
            Column {
                SectionTitle("Mode latihan")
                Spacer(Modifier.height(8.dp))
                listOf(
                    Triple("Flashcard", "Balik dan nilai kartu", Icons.Rounded.Style),
                    Triple("Ulang mekanis", "Pengulangan tanpa batas", Icons.Rounded.Repeat),
                    Triple("Mengetik", "Tulis jawaban dari ingatan", Icons.Rounded.Keyboard),
                    Triple("Tes kecepatan", "Jawab dalam 60 detik", Icons.Rounded.Timer),
                    Triple("Daftar", "Baca kata dan arti", Icons.AutoMirrored.Rounded.List),
                ).forEach { (mode, description, icon) ->
                    ListItem(
                        headlineContent = { Text(mode, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text(description) },
                        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(role = Role.Button) { vm.start(mode, reverse, group) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
internal fun PracticeScreen(vm: VocaModel, tts: Pronouncer) {
    val p = vm.practice ?: return
    val lifecycle = LocalLifecycleOwner.current
    var active by remember {
        mutableStateOf(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var auto by rememberSaveable { mutableStateOf(false) }
    var exit by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val o = LifecycleEventObserver { _, _ ->
            active = lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.lifecycle.addObserver(o)
        onDispose { lifecycle.lifecycle.removeObserver(o) }
    }
    LaunchedEffect(active, p.finished) {
        if (active && !p.finished) {
            var last = android.os.SystemClock.elapsedRealtime()
            while (true) {
                delay(250)
                val now = android.os.SystemClock.elapsedRealtime()
                vm.tick(now - last)
                last = now
            }
        }
    }
    LaunchedEffect(auto, active, p.attempt, p.revealed) {
        if (auto && active && p.mode == "Ulang mekanis" && !p.finished) {
            delay(if (p.revealed) 2400 else 3000)
            if (p.revealed) vm.nextUnscored() else vm.reveal()
        }
    }
    BackHandler { exit = true }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                .glass(MaterialTheme.shapes.large).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { exit = true }) { Icon(Icons.Rounded.Close, "Akhiri latihan") }
            Text(
                p.mode,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (p.mode == "Tes kecepatan")
                Pill("${(p.remainingMs+999)/1000} dtk", Icons.Rounded.Timer)
        }
        if (p.finished) {
            Column(
                Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(20.dp))
                Title(
                    "Sesi selesai.",
                    "${p.reviewed} jawaban • ${if(p.reviewed==0)0 else p.right*100/p.reviewed}% akurasi",
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = vm::end, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text("Kembali ke Latihan")
                }
            }
        } else if (p.mode == "Daftar") {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(p.cards, key = { it.id }) { m ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(
                                if (p.reverse) m.meaning else m.word,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (p.reverse) m.word else m.meaning,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            IconButton(onClick = { tts.speak(m.word) }) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Ucapkan ${m.word}")
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (p.reverse) "Indonesia → Inggris" else "Inggris → Indonesia",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${p.reviewed} terjawab", style = MaterialTheme.typography.labelLarge)
                }
                if (p.mode == "Spaced repetition")
                    LinearProgressIndicator(
                        progress = { p.reviewed.toFloat() / p.cards.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                val typed = p.mode == "Mengetik" || p.mode == "Tes kecepatan"
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 230.dp)
                            .then(
                                if (!typed) Modifier.clickable(role = Role.Button) { vm.reveal() }
                                else Modifier
                            ),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp).animateContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Text(
                            p.prompt,
                            style = if (p.reverse) MaterialTheme.typography.headlineLarge else entryType(36, 46),
                            textAlign = TextAlign.Center,
                        )
                        if (p.current.context.isNotBlank())
                            Text(
                                p.current.context,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        if (p.revealed) {
                            HorizontalDivider()
                            AnimatedContent(p.answer, label = "Jawaban kartu") { answer ->
                                Text(
                                    answer,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = { tts.speak(p.current.word) }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Dengarkan kata")
                }
                if (typed) {
                    OutlinedTextField(
                        p.typed,
                        vm::type,
                        label = { Text("Jawaban Anda") },
                        singleLine = true,
                        enabled = !p.checked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    )
                    if (!p.checked)
                        Button(
                            onClick = vm::checkAnswer,
                            enabled = p.typed.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Periksa jawaban")
                        }
                    else {
                        Text(
                            if (p.correct) "Tepat!" else "Belum tepat. Jawaban: ${p.answer}",
                            color =
                                if (p.correct) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Button(
                            onClick = { vm.grade(if (p.correct) 3 else 1) },
                            enabled = !vm.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Kata berikutnya")
                        }
                    }
                    Text(
                        "Jawaban dicocokkan dengan arti yang tersimpan di Memo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (p.mode == "Ulang mekanis") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(auto, { auto = it })
                        Spacer(Modifier.width(12.dp))
                        Text("Putar otomatis")
                    }
                    Text(
                        "Putaran otomatis tidak menambah statistik jawaban.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = vm::reveal,
                            enabled = !p.revealed,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Lihat arti")
                        }
                        Button(onClick = vm::nextUnscored, modifier = Modifier.weight(1f)) {
                            Text("Berikutnya")
                        }
                    }
                } else if (!p.revealed)
                    Button(onClick = vm::reveal, modifier = Modifier.fillMaxWidth()) {
                        Text("Tampilkan jawaban")
                    }
                else {
                    Text(
                        "Nilai ingatan",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    FlowRowCompat {
                        listOf(1 to "Ulang lagi", 2 to "Sulit", 3 to "Ingat", 4 to "Mudah")
                            .forEach { (r, label) ->
                                OutlinedButton(onClick = { vm.grade(r) }, enabled = !vm.busy) {
                                    Text(label)
                                }
                            }
                    }
                }
            }
        }
    }
    if (exit)
        AlertDialog(
            onDismissRequest = { exit = false },
            title = { Text("Akhiri sesi ini?") },
            text = {
                Text(
                    "Jawaban yang sudah dinilai tetap tersimpan. Sesi bisa dimulai lagi kapan saja."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.end()
                        exit = false
                    }
                ) {
                    Text("Akhiri sesi")
                }
            },
            dismissButton = { TextButton(onClick = { exit = false }) { Text("Lanjut belajar") } },
        )
}
