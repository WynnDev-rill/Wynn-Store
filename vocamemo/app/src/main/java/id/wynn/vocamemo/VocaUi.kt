package id.wynn.vocamemo

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val destinations = listOf(
    "Kamus" to Icons.AutoMirrored.Rounded.MenuBook,
    "Memo" to Icons.Rounded.BookmarkBorder,
    "Latihan" to Icons.Rounded.Style,
    "Profil" to Icons.Rounded.PersonOutline,
)

@Composable
internal fun VocaNavigation(page: String, onPage: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.widthIn(max = 520.dp).fillMaxWidth(),
            color = colors.surface,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
                val tabWidth = maxWidth / destinations.size
                val index = destinations.indexOfFirst { it.first == page }.coerceAtLeast(0)
                val highlight by animateDpAsState(
                    targetValue = tabWidth * index,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 460f),
                    label = "Active navigation tab",
                )
                Box(
                    Modifier.offset(x = highlight).width(tabWidth).padding(top = 2.dp).height(51.dp)
                        .background(
                            Brush.radialGradient(
                                0f to colors.primary.copy(alpha = 0.28f),
                                0.65f to colors.secondary.copy(alpha = 0.11f),
                                1f to Color.Transparent,
                            )
                        )
                )
                Row(Modifier.fillMaxWidth()) {
                    destinations.forEach { (label, icon) ->
                        val selected = label == page
                        Column(
                            Modifier.weight(1f).heightIn(min = 64.dp).clip(MaterialTheme.shapes.large)
                                .testTag("nav-$label").selectable(selected, role = Role.Tab) { onPage(label) }
                                .padding(horizontal = 2.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                Modifier.size(width = 40.dp, height = 29.dp)
                                    .background(
                                        if (selected) colors.primaryContainer else Color.Transparent,
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, null, Modifier.size(21.dp),
                                    tint = if (selected) colors.primary else colors.onSurfaceVariant)
                            }
                            Text(label, style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) colors.onSurface else colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun VocaRail(page: String, onPage: (String) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Spacer(Modifier.weight(1f))
        destinations.forEach { (label, icon) ->
            NavigationRailItem(page == label, { onPage(label) }, { Icon(icon, null) },
                label = { Text(label) })
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun VocaSearch(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = trailing,
        singleLine = true,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = colors.surface,
            focusedContainerColor = colors.surface,
            unfocusedBorderColor = colors.outlineVariant,
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
        ),
    )
}
