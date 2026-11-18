package id.wynn.vocamemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.Locale
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
    val grouped = browsing && vm.wordFilter != WordFilter.Random
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
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Kamus",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }.padding(bottom = 3.dp),
            )
            VocaSearch(
                value = vm.query,
                onChange = {
                    vm.search(it)
                    toTop()
                },
                label = "Cari kata atau arti",
                modifier = Modifier.testTag("search"),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (browsing) {
                    listOf(WordFilter.All, WordFilter.Unsaved, WordFilter.Random).forEach { filter ->
                        FilterChip(
                            selected = vm.wordFilter == filter,
                            onClick = {
                                focus.clearFocus()
                                vm.browse(filter, vm.alphabet.takeUnless { filter == WordFilter.Random }.orEmpty())
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
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            focus.clearFocus()
                            alphabetOpen = true
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            when (vm.alphabet) {
                                "" -> "Lompat A–Z"
                                "{" -> "Lompat A–Z · #"
                                else -> "Lompat A–Z · ${vm.alphabet}"
                            }
                        )
                        Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp))
                    }
                    FilledTonalButton(
                        onClick = {
                            focus.clearFocus()
                            vm.discover()
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Temukan Kata")
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp))
        LazyColumn(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("dictionary-list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
        ) {
            itemsIndexed(rows, key = { _, result -> result.key }) { index, result ->
                val letter = result.key.dictionaryLetter()
                val sectionStart = grouped && (index == 0 || rows[index - 1].key.dictionaryLetter() != letter)
                DictionaryEntry(result, grouped, letter.takeIf { sectionStart }, result.key in savedWords) {
                    focus.clearFocus(force = true)
                    vm.openWord(
                        result.word,
                        result.summary.takeIf { !browsing && vm.language == "id" },
                    )
                }
            }
            if (vm.searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (browsing && vm.browseMore && !vm.browseError)
                item(key = "next-page") {
                    LaunchedEffect(vm.browseRows.size, vm.browseBusy) { vm.loadWords() }
                    if (vm.browseBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else TextButton(onClick = vm::loadWords) { Text("Muat kata berikutnya") }
                }
            if (browsing && vm.browseError)
                item {
                    TextButton(onClick = vm::loadWords) { Text("Kamus gagal dimuat. Coba lagi") }
                }
            if (rows.isEmpty() && !vm.searching && (!browsing || !vm.browseMore))
                item {
                    Hint(
                        Icons.Rounded.Search,
                        if (browsing) "Tidak ada kata dalam filter ini" else "Kata belum ditemukan",
                        if (browsing) "Ubah filter atau huruf." else "Coba ejaan atau arah pencarian lain.",
                    )
                }
            if (browsing && vm.wordFilter == WordFilter.Random)
                item {
                    TextButton(onClick = {
                        vm.browse(WordFilter.Random)
                        toTop()
                    }) {
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
                        val key = if (letter == "#") "{" else letter
                        TextButton(
                            onClick = {
                                alphabetOpen = false
                                val filter = if (vm.wordFilter == WordFilter.Random) WordFilter.All else vm.wordFilter
                                vm.browse(filter, key)
                                toTop()
                            },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (vm.alphabet == key)
                                    MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            ),
                            contentPadding = PaddingValues(6.dp),
                        ) { Text(letter) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { alphabetOpen = false }) { Text("Tutup") } },
        )
    }
}

private fun String.dictionaryLetter(): String =
    firstOrNull()?.uppercase(Locale.ROOT)?.takeIf { it.length == 1 && it[0] in 'A'..'Z' } ?: "#"

@Composable
private fun DictionaryEntry(
    result: WordResult,
    grouped: Boolean,
    letter: String?,
    saved: Boolean,
    open: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = open)
            .heightIn(min = 68.dp).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (grouped) {
            Box(Modifier.widthIn(min = 32.dp).padding(end = 10.dp)) {
                if (letter != null) {
                    Text(
                        letter,
                        color = colors.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(result.word, style = entryType(23, 29))
            Text(result.summary, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            result.suggestion?.let {
                Text(it, color = colors.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (saved) {
            Icon(
                Icons.Rounded.Bookmark,
                "Ada di Memo",
                Modifier.padding(start = 8.dp).size(18.dp),
                tint = colors.secondary,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            Modifier.padding(start = 8.dp).size(20.dp),
            tint = colors.onSurfaceVariant,
        )
    }
    HorizontalDivider(Modifier.padding(start = if (grouped) 32.dp else 0.dp))
}

@Composable
internal fun VocaSearch(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
        color = Color.Transparent,
        shape = shape,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onChange("") }) {
                        Icon(Icons.Rounded.Close, "Hapus pencarian")
                    }
                }
            },
            singleLine = true,
            shape = shape,
            modifier = modifier.fillMaxWidth().glass(shape),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent),
        )
    }
}
