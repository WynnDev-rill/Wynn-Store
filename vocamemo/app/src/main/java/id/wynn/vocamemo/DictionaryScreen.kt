package id.wynn.vocamemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DictionaryScreen(vm: VocaModel, memos: List<Memo>) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val state = vm.dictionaryScroll
    var alphabetOpen by rememberSaveable { mutableStateOf(false) }
    val savedWords = remember(memos) { memos.mapTo(hashSetOf()) { dictionaryKey(it.word) } }
    val browsing = vm.query.isBlank()
    val rows =
        remember(browsing, vm.results, vm.browseRows, vm.wordFilter, savedWords) {
            val available = if (browsing) vm.browseRows else vm.results
            if (vm.wordFilter == WordFilter.Unsaved) available.filter { it.key !in savedWords }
            else available
        }
    fun toTop() {
        scope.launch { state.scrollToItem(0) }
    }

    Column(Modifier.fillMaxSize()) {
        VocaSearch(
            value = vm.query,
            onChange = {
                vm.search(it)
                toTop()
            },
            label = "Cari Inggris atau Indonesia",
            trailing = {
                if (vm.query.isNotEmpty())
                    IconButton(
                        onClick = {
                            vm.search("")
                            toTop()
                        }
                    ) {
                        Icon(Icons.Rounded.Close, "Hapus pencarian")
                    }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).testTag("search"),
        )
        FlowRow(
            Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (browsing) {
                WordFilter.entries.filter { it != WordFilter.Random }.forEach { filter ->
                    FilterChip(
                        selected = vm.wordFilter == filter,
                        onClick = {
                            focus.clearFocus()
                            vm.browse(filter)
                            toTop()
                        },
                        label = { Text(filter.label) },
                        shape = MaterialTheme.shapes.large,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            } else {
                FilterChip(
                    vm.language == "en",
                    {
                        vm.changeLanguage("en")
                        toTop()
                    },
                    { Text("Inggris → Indonesia") },
                )
                FilterChip(
                    vm.language == "id",
                    {
                        vm.changeLanguage("id")
                        toTop()
                    },
                    { Text("Indonesia → Inggris") },
                )
            }
        }
        if (browsing) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = {
                        focus.clearFocus()
                        alphabetOpen = true
                    }
                ) {
                    Icon(Icons.Rounded.SortByAlpha, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (vm.alphabet.isEmpty()) "Lompat A–Z"
                        else "Lompat A–Z · ${if (vm.alphabet == "{") "#" else vm.alphabet}"
                    )
                }
                TextButton(
                    onClick = {
                        focus.clearFocus()
                        vm.discover()
                    }
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Temukan Kata")
                }
                IconButton(onClick = {
                    focus.clearFocus()
                    vm.browse(WordFilter.Random)
                    toTop()
                }) {
                    Icon(Icons.Rounded.Shuffle,
                        if (vm.wordFilter == WordFilter.Random) "Acak lagi" else "Acak daftar kata",
                        tint = if (vm.wordFilter == WordFilter.Random) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (vm.searching || (browsing && vm.browseBusy))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("dictionary-list"),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(rows, key = { it.key }) { result ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(role = Role.Button) {
                            focus.clearFocus(force = true)
                            vm.openWord(
                                result.word,
                                result.summary.takeIf { !browsing && vm.language == "id" },
                            )
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WordTile(result.word, wordPhotos[result.key], Modifier.size(42.dp))
                    Column(Modifier.weight(1f)) {
                        Text(result.word, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold)
                        Text(result.summary, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        result.suggestion?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (result.key in savedWords) {
                        Icon(Icons.Rounded.BookmarkBorder, "Tersimpan", Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (browsing && vm.browseMore && !vm.browseError)
                item(key = "next-page") {
                    LaunchedEffect(vm.browseRows.size, vm.browseBusy) { vm.loadWords() }
                    if (!vm.browseBusy)
                        TextButton(onClick = vm::loadWords) { Text("Muat kata berikutnya") }
                }
            if (browsing && vm.browseError)
                item {
                    TextButton(onClick = vm::loadWords) { Text("Kamus gagal dimuat. Coba lagi") }
                }
            if (rows.isEmpty() && !vm.searching && (!browsing || !vm.browseMore))
                item {
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (browsing) "Daftar ini kosong" else "Kata belum ditemukan",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (browsing) "Pilih Semua Kata untuk menjelajah lagi."
                                else "Coba ejaan atau arah pencarian lain.")
                        }
                    }
                }

        }
    }
    if (alphabetOpen) {
        AlertDialog(
            onDismissRequest = { alphabetOpen = false },
            title = { Text("Lompat ke huruf") },
            text = {
                FlowRow(
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (('A'..'Z').map { it.toString() } + "#").forEach { letter ->
                        TextButton(
                            onClick = {
                                alphabetOpen = false
                                val filter =
                                    if (vm.wordFilter == WordFilter.Random || vm.wordFilter == WordFilter.Illustrated) WordFilter.All
                                    else vm.wordFilter
                                vm.browse(filter, if (letter == "#") "{" else letter)
                                toTop()
                            },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            contentPadding = PaddingValues(6.dp),
                        ) {
                            Text(letter)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { alphabetOpen = false }) { Text("Tutup") } },
        )
    }
}
