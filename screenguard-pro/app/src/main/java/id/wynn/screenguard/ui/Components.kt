package id.wynn.screenguard.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import id.wynn.screenguard.R
import id.wynn.screenguard.data.UsageDay
import id.wynn.screenguard.domain.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun GuardCard(modifier: Modifier = Modifier, contentPadding: Dp = 16.dp, spacing: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)), shadowElevation = 0.dp) {
        Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
private val iconCache = android.util.LruCache<String, ImageBitmap>(64)
@Composable fun AppIcon(app: InstalledApp, size: Dp = 40.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, app.packageName) {
        value = iconCache.get(app.packageName) ?: withContext(Dispatchers.IO) { runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(112, 112).asImageBitmap().also { iconCache.put(app.packageName, it) } }.getOrNull() }
    }
    Box(Modifier.size(size).clearAndSetSemantics {}.clip(RoundedCornerShape(size * .23f)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize())
        else Text(app.label.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
@Composable fun BrandMark(size: Dp = 42.dp) { Image(painterResource(R.drawable.brand_mark), null, Modifier.size(size).clip(RoundedCornerShape(size * .24f))) }
@Composable fun PageHeader(title: String, back: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) action() else Spacer(Modifier.width(16.dp))
    }
}
@Composable fun SectionTitle(title: String, trailing: String? = null, action: (() -> Unit)? = null) {
    if (LocalDensity.current.fontScale > 1.3f && trailing != null && action != null) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            TextButton(action, Modifier.align(Alignment.End)) { Text(trailing) }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailing != null && action != null) TextButton(action) { Text(trailing) }
    }
}
@Composable fun InfoBanner(text: String, icon: ImageVector = Icons.Outlined.Info, warning: Boolean = false, action: (() -> Unit)? = null) {
    Surface(Modifier.fillMaxWidth().then(if (action != null) Modifier.clickable(onClick = action) else Modifier),
        shape = RoundedCornerShape(16.dp), color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodySmall, color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
            if (action != null) Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp))
        }
    }
}
@Composable fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Insights, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun PillTabs(tabs: List<String>, selected: Int, select: (Int) -> Unit) {
    if (LocalDensity.current.fontScale > 1.3f) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { i, title -> FilterChip(selected == i, { select(i) }, label = { Text(title) }, shape = RoundedCornerShape(24.dp)) }
        }
        return
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
        tabs.forEachIndexed { i, title ->
            val selectedColor by animateColorAsState(if (selected == i) MaterialTheme.colorScheme.primary else Color.Transparent, tween(180), label = "tab color")
            Surface(onClick = { select(i) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { this.selected = selected == i },
                shape = RoundedCornerShape(22.dp), color = selectedColor) {
                Box(Modifier.padding(horizontal = 5.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = if (selected == i) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable fun UsageRow(app: InstalledApp, ms: Long, total: Long, compact: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = if (compact) 48.dp else 56.dp).clickable(onClick = onClick).padding(vertical = if (compact) 6.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(app, if (compact) 28.dp else 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.label, style = if (compact) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (ms > 0) duration(ms) else "0 mnt", style = MaterialTheme.typography.labelLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val progress by animateFloatAsState(if (total > 0) (ms.toFloat() / total).coerceIn(0f, 1f) else 0f, label = "usage")
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
                Text("${if (total > 0) ms * 100 / total else 0}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 30.dp))
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
fun dateLabel(date: java.time.LocalDate): String = when (date) {
    java.time.LocalDate.now() -> "Hari ini"
    java.time.LocalDate.now().minusDays(1) -> "Kemarin"
    else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("id")))
}

@Composable fun WeekChart(days: List<UsageDay>, modifier: Modifier = Modifier, labels: Boolean = true, chartHeight: Dp = 96.dp,
    selectedDate: java.time.LocalDate? = null, onSelect: ((UsageDay) -> Unit)? = null) {
    val color = MaterialTheme.colorScheme.primary
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(480, easing = FastOutSlowInEasing)) }
    val max = days.maxOfOrNull { it.totalMs }?.coerceAtLeast(3_600_000) ?: 3_600_000
    val names = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val chartWidth = if (onSelect != null) maxOf(maxWidth, 48.dp * days.size) else maxWidth
        Row(Modifier.horizontalScroll(rememberScrollState()).width(chartWidth), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEach { day ->
                val selected = selectedDate == day.date
                val action = if (onSelect != null) Modifier.clickable(role = Role.Button) { onSelect(day) } else Modifier
                Column(Modifier.weight(1f).then(action).semantics(mergeDescendants = true) {
                    contentDescription = "${day.date}: ${if (day.totalMs == 0L) "0 menit" else duration(day.totalMs)}"
                    if (onSelect != null) this.selected = selected
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.fillMaxWidth().height(if (labels) chartHeight else 62.dp)) {
                        repeat(3) { n -> val y = size.height * n / 2; drawLine(color.copy(alpha = .1f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
                        val height = (day.totalMs.toFloat() / max * (size.height - 4.dp.toPx()) * reveal.value).coerceAtLeast(2.dp.toPx())
                        val width = minOf(size.width * .42f, 22.dp.toPx())
                        drawRoundRect(Brush.verticalGradient(listOf(color.copy(alpha = if (selected) .8f else .38f), color.copy(alpha = if (selected) 1f else .7f))),
                            Offset((size.width - width) / 2, size.height - height), androidx.compose.ui.geometry.Size(width, height), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                    }
                    if (labels) Text(names[day.date.dayOfWeek.value - 1], Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall,
                        color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
@Composable fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
