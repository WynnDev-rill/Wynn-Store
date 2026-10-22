package id.wynn.screenguard.ui

import androidx.compose.animation.core.animateFloatAsState
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
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)), shadowElevation = .5.dp) {
        Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
@Composable fun AppIcon(app: InstalledApp, size: Dp = 40.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) { runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(112, 112).asImageBitmap() }.getOrNull() }
    }
    Box(Modifier.size(size).clip(RoundedCornerShape(size * .23f)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize())
        else Text(app.label.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
@Composable fun BrandMark(size: Dp = 42.dp) { Image(painterResource(R.drawable.ic_guard), null, Modifier.size(size)) }
@Composable fun PageHeader(title: String, back: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) action() else Spacer(Modifier.width(16.dp))
    }
}
@Composable fun SectionTitle(title: String, trailing: String? = null, action: (() -> Unit)? = null) {
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
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
        tabs.forEachIndexed { i, title ->
            Surface(onClick = { select(i) }, modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics { this.selected = selected == i },
                shape = RoundedCornerShape(22.dp), color = if (selected == i) MaterialTheme.colorScheme.primary else Color.Transparent) {
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
@Composable fun WeekChart(days: List<UsageDay>, modifier: Modifier = Modifier, labels: Boolean = true, chartHeight: Dp = 96.dp) {
    val color = MaterialTheme.colorScheme.primary
    val max = days.maxOfOrNull { it.totalMs }?.coerceAtLeast(3_600_000) ?: 3_600_000
    val text = days.joinToString { "${it.date}: ${if (it.totalMs == 0L) "0 menit" else duration(it.totalMs)}" }
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "Penggunaan harian. $text" }) {
        Canvas(Modifier.fillMaxWidth().height(if (labels) chartHeight else 62.dp)) {
            val count = days.size.coerceAtLeast(1)
            val spacing = size.width / count
            val width = (spacing * .38f).coerceAtMost(21.dp.toPx())
            repeat(3) { n -> val y = size.height * n / 2; drawLine(color.copy(alpha = .08f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
            days.forEachIndexed { index, day ->
                val height = (day.totalMs.toFloat() / max * (size.height - 4.dp.toPx())).coerceAtLeast(2.dp.toPx())
                drawRoundRect(Brush.verticalGradient(listOf(color.copy(alpha = .36f), color.copy(alpha = .8f))),
                    Offset(spacing * (index + .5f) - width / 2, size.height - height), androidx.compose.ui.geometry.Size(width, height), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            }
        }
        if (labels) Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            val names = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            days.forEach { day -> Text(names[day.date.dayOfWeek.value - 1], Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
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
