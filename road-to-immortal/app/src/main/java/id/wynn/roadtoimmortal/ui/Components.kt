package id.wynn.roadtoimmortal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.wynn.roadtoimmortal.data.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

val Rounded = RoundedCornerShape(24.dp)
val scopes =
    linkedMapOf(
        "all" to "Semua rank",
        "epic" to "Epic",
        "legend" to "Legend",
        "mythic" to "Mythic",
        "honor" to "Honor",
        "glory" to "Glory+",
    )

fun rate(v: Double?) = v?.let { String.format(Locale.forLanguageTag("id-ID"), "%.1f%%", it) } ?: "—"

fun dateText(v: String?) =
    v?.let {
        runCatching {
                DateTimeFormatter.ofPattern("d MMM yyyy · HH.mm", Locale.forLanguageTag("id-ID"))
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(it))
            }
            .getOrNull()
    } ?: "Tanggal sumber tidak tersedia"

fun ageText(p: Provenance): String {
    val h =
        p.updatedAt?.let {
            runCatching { (Instant.now().epochSecond - Instant.parse(it).epochSecond) / 3600 }
                .getOrNull()
        } ?: return "Tanggal belum tersedia"
    return when {
        h < 0 -> dateText(p.updatedAt)
        h < 1 -> "< 1 jam lalu"
        h < 24 -> "$h jam lalu"
        else -> "${h/24} hari lalu"
    }
}

fun categoryLabel(v: String) =
    when (v.lowercase()) {
        "jungling" -> "Jungle"
        "roaming" -> "Roam"
        "metadata" -> "Belum terverifikasi"
        else -> v
    }

@Composable
fun Artwork(
    url: String?,
    name: String?,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Crop,
    round: Dp = 16.dp,
    fallbackUrl: String? = null,
) {
    var activeUrl by
        remember(url, fallbackUrl) {
            mutableStateOf(url?.takeIf(String::isNotBlank) ?: fallbackUrl)
        }
    var state by remember(url, fallbackUrl) { mutableIntStateOf(0) }
    Box(
        modifier
            .clip(RoundedCornerShape(round))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = activeUrl,
            contentDescription = name,
            contentScale = scale,
            modifier = Modifier.matchParentSize(),
            onLoading = { state = 0 },
            onSuccess = { state = 1 },
            onError = {
                if (!fallbackUrl.isNullOrBlank() && activeUrl != fallbackUrl) {
                    activeUrl = fallbackUrl
                    state = 0
                } else state = 2
            },
        )
        if (state != 1)
            Icon(
                if (state == 2) Icons.Outlined.BrokenImage else Icons.Outlined.Image,
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f),
            )
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onAction) { Text(action) }
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun Tag(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = .09f))
                .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
fun ChoiceRow(
    choices: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(choices) { v -> FilterChip(selected == v, { onSelect(v) }, label = { Text(v) }) }
    }
}

@Composable
fun ScopePicker(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ open = true }, contentPadding = PaddingValues(horizontal = 14.dp)) {
            Text(scopes[value] ?: value)
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        }
        DropdownMenu(open, { open = false }) {
            scopes.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChange(key)
                        open = false
                    },
                    trailingIcon = { if (value == key) Icon(Icons.Outlined.Check, null) },
                )
            }
        }
    }
}

@Composable
fun SearchBox(
    query: String,
    onChange: (String) -> Unit,
    placeholder: String = "Cari hero atau role",
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        query,
        onChange,
        modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        shape = RoundedCornerShape(18.dp),
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            if (query.isNotEmpty())
                IconButton({ onChange("") }) { Icon(Icons.Outlined.Close, "Hapus pencarian") }
        },
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    icon: ImageVector = Icons.Outlined.SearchOff,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) FilledTonalButton(onAction) { Text(action) }
    }
}

@Composable
fun HeroRow(hero: Hero, subtitle: String, right: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Artwork(hero.icon, null, Modifier.size(54.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(hero.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (right != null)
                Text(
                    right,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
        }
    }
}

@Composable
fun GearRow(gear: List<Equipment>, onClick: (Equipment) -> Unit, size: Dp = 48.dp) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(gear, key = { it.id }) { item ->
            Box(
                Modifier.size(size.coerceAtLeast(48.dp)).clip(RoundedCornerShape(14.dp)).clickable {
                    onClick(item)
                },
                contentAlignment = Alignment.Center,
            ) {
                Artwork(item.icon, item.name, Modifier.size(size), round = 12.dp)
            }
        }
    }
}

@Composable
fun SourceLine(source: Provenance, onClick: () -> Unit) {
    TextButton(onClick, contentPadding = PaddingValues(0.dp)) {
        Icon(Icons.Outlined.Update, null, Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text("${source.name} · ${ageText(source)}", style = MaterialTheme.typography.labelMedium)
    }
}
