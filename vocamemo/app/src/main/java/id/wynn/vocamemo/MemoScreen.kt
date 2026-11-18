package id.wynn.vocamemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun MemoScreen(vm: VocaModel, memos: List<Memo>, tts: Pronouncer) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Semua") }
    var group by rememberSaveable { mutableStateOf("Semua") }
    var edit by remember { mutableStateOf<Memo?>(null) }
    val shown =
        memos.filter {
            (group == "Semua" || it.groupName == group) &&
                when (filter) {
                    "Favorit" -> it.favourite
                    "Dikuasai" -> it.mastered
                    "Belajar" -> !it.mastered
                    else -> true
                } &&
                (it.word.contains(search, true) ||
                    it.meaning.contains(search, true) ||
                    it.note.contains(search, true))
        }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
    ) {
        item {
            VocaSearch(search, { search = it }, "Cari di Memo")
            Spacer(Modifier.height(6.dp))
            FlowRowCompat {
                listOf("Semua", "Belajar", "Favorit", "Dikuasai").forEach { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) },
                    )
                }
            }
            GroupPicker(memos.map { it.groupName }.distinct().sorted(), group, { group = it })
            Text(
                "Memo Anda",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            HorizontalDivider()
        }
        if (shown.isEmpty())
            item {
                Hint(
                    Icons.Rounded.Bookmarks,
                    if (memos.isEmpty()) "Memo masih kosong" else "Tidak ada memo yang cocok",
                    if (memos.isEmpty())
                        "Simpan arti dari Kamus untuk berlatih."
                    else "Coba kata atau filter lainnya.",
                ) {
                    Button(onClick = { vm.page("Kamus") }) { Text("Buka Kamus") }
                }
            }
        items(shown, key = { it.id }) { memo ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f).clickable(role = Role.Button) { edit = memo }
                        .heightIn(min = 56.dp).padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(memo.word, style = entryType(23, 29))
                    Text(
                        memo.meaning,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (memo.mastered || memo.groupName != "Umum") {
                        Text(
                            if (memo.mastered) "Dikuasai" else memo.groupName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                IconButton(onClick = { tts.speak(memo.word) }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Ucapkan ${memo.word}")
                }
                IconButton(onClick = { vm.save(memo.copy(favourite = !memo.favourite)) }) {
                    Icon(
                        if (memo.favourite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        if (memo.favourite) "Hapus favorit ${memo.word}" else "Favoritkan ${memo.word}",
                        tint = if (memo.favourite) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
        }
    }
    edit?.let { m ->
        MemoEditor(
            m,
            onDismiss = { edit = null },
            onSave = {
                vm.save(it)
                edit = null
            },
            onDelete = {
                vm.delete(m)
                edit = null
            },
        )
    }
}

@Composable
private fun MemoEditor(
    m: Memo,
    onDismiss: () -> Unit,
    onSave: (Memo) -> Unit,
    onDelete: () -> Unit,
) {
    var meaning by remember { mutableStateOf(m.meaning) }
    var group by remember { mutableStateOf(m.groupName) }
    var note by remember { mutableStateOf(m.note) }
    var mastered by remember { mutableStateOf(m.mastered) }
    var confirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(m.word) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    meaning,
                    { meaning = it.take(1000) },
                    label = { Text("Arti yang dihafalkan") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (m.context.isNotBlank()) {
                    Text(
                        m.context,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    group,
                    { group = it.take(80) },
                    label = { Text("Koleksi / kelompok") },
                    singleLine = true,
                )
                OutlinedTextField(
                    note,
                    { note = it.take(5000) },
                    label = { Text("Catatan pribadi") },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(mastered, { mastered = it })
                    Text("Sudah dikuasai", Modifier.weight(1f))
                }
                TextButton(onClick = { confirm = true }) {
                    Icon(Icons.Rounded.DeleteOutline, null)
                    Text("Hapus memo")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = meaning.isNotBlank() && group.isNotBlank(),
                onClick = {
                    onSave(
                        m.copy(
                            meaning = meaning,
                            groupName = group,
                            note = note,
                            mastered = mastered,
                        )
                    )
                },
            ) {
                Text("Simpan perubahan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Hapus ${m.word}?") },
            text = { Text("Memo akan dihapus. Riwayat latihan tetap tersimpan untuk statistik.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } },
        )
}

@Composable
internal fun GroupPicker(groups: List<String>, value: String, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Koleksi: $value")
            Icon(Icons.Rounded.ArrowDropDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            (listOf("Semua") + groups.filter { it != "Semua" }).forEach { g ->
                DropdownMenuItem(
                    text = { Text(g) },
                    onClick = {
                        change(g)
                        expanded = false
                    },
                )
            }
        }
    }
}
