package id.wynn.screenguard.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Persistent, safe-area-aware dock. Active item expands without moving its tap region. */
@Composable fun FloatingDock(page: Page, navigate: (Page) -> Unit) {
    val tabs = listOf(Triple(Page.FREEZE, "Bekukan", Icons.Outlined.AcUnit),
        Triple(Page.HOME, "Aktivitas", Icons.Outlined.BarChart),
        Triple(Page.SETTINGS, "Pengaturan", Icons.Outlined.Settings))
    val active = if (page == Page.USAGE || page == Page.APP_STATS) Page.HOME else page
    val largeText = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        val showLabel = maxWidth >= 350.dp && !largeText
        Surface(Modifier.widthIn(max = 460.dp).fillMaxWidth(), shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)), shadowElevation = 10.dp) {
            Row(Modifier.selectableGroup().padding(7.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { (target, title, icon) ->
                    val selected = target == active
                    val color by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        tween(180), label = "dock surface")
                    val tint by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        tween(150), label = "dock icon")
                    val weight by animateFloatAsState(if (selected && showLabel) (if (title.length > 8) 2.6f else 2.1f) else 1f,
                        spring(dampingRatio = .9f, stiffness = 600f), label = "dock selection")
                    Box(Modifier.weight(weight).heightIn(min = 56.dp).clip(CircleShape).background(color)
                        .selectable(selected, role = Role.Tab, onClick = { navigate(target) })
                        .semantics { contentDescription = title }, contentAlignment = Alignment.Center) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, Modifier.size(23.dp), tint = tint)
                            AnimatedVisibility(selected && showLabel, enter = fadeIn(tween(180)), exit = fadeOut(tween(90))) {
                                Text(title, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Clip,
                                    modifier = Modifier.clearAndSetSemantics {})
                            }
                        }
                    }
                }
            }
        }
    }
}
