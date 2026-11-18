package id.wynn.vocamemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
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
        OutlinedTextField(
            value = vm.query,
            onValueChange = {
                vm.search(it)
                toTop()
            },
            label = { Text("Cari Inggris atau Indonesia") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
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
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("search"),
        )
        FlowRow(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (browsing) {
                WordFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = vm.wordFilter == filter,
                        onClick = {
                            focus.clearFocus()
                            vm.browse(filter)
                            toTop()
                        },
                        label = { Text(filter.label) },
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
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = {
                        focus.clearFocus()
                        alphabetOpen = true
                    }
                ) {
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
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Temukan Kata")
                }
            }
        }
        if (vm.searching || (browsing && vm.browseBusy))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        HorizontalDivider()
        LazyColumn(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("dictionary-list"),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(rows, key = { it.key }) { result ->
                ListItem(
                    headlineContent = { Text(result.word) },
                    supportingContent = {
                        Column {
                            Text(result.summary)
                            result.suggestion?.let {
                                Text(it, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
                    modifier =
                        Modifier.clickable(role = Role.Button) {
                            focus.clearFocus(force = true)
                            vm.openWord(
                                result.word,
                                result.summary.takeIf { !browsing && vm.language == "id" },
                            )
                        },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
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
                    Text(
                        if (browsing) "Tidak ada kata lain dalam filter ini."
                        else "Kata belum ditemukan. Coba ejaan atau arah pencarian lain.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            if (browsing && vm.wordFilter == WordFilter.Random)
                item {
                    TextButton(
                        onClick = {
                            vm.browse(WordFilter.Random)
                            toTop()
                        }
                    ) {
                        Text("Acak lagi")
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
                                    if (vm.wordFilter == WordFilter.Random) WordFilter.All
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
