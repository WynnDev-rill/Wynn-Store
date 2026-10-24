package id.wynn.screenguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.data.*
import id.wynn.screenguard.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun activityApp(pkg: String, apps: List<InstalledApp>): InstalledApp = apps.find {it.packageName==pkg}
    ?: InstalledApp(pkg,pkg,0,-1,true,false,true,false,launchable=false)
fun readableDuration(ms: Long): String {
    val minutes=ms.coerceAtLeast(0)/60000
    return when {ms<=0 -> "0 mnt";minutes==0L -> "<1 mnt";minutes<60 -> "$minutes mnt";else -> "${minutes/60} jam ${minutes%60} mnt"}
}
@Composable fun DateRangeControl(date: LocalDate,count: Int,earliest: LocalDate?,change: (LocalDate)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        IconButton({change(date.minusDays(1))},enabled=earliest!=null&&date.isAfter(earliest)) {Icon(Icons.Outlined.ChevronLeft,"Tanggal sebelumnya")}
        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(if(count==1) dateLabel(date) else "${dateLabel(date.minusDays(count.toLong()-1))} – ${dateLabel(date)}",style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
            Text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy",Locale.forLanguageTag("id"))),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton({change(date.plusDays(1))},enabled=date.isBefore(LocalDate.now())) {Icon(Icons.Outlined.ChevronRight,"Tanggal berikutnya")}
    }
}
@Composable fun ActivityMetric(label: String,value: String,modifier: Modifier=Modifier) {
    Column(modifier,verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(value,style=MaterialTheme.typography.titleLarge)
        Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun Coverage(summary: ActivitySummary) {
    if(summary.partial) Text(if(summary.available.isEmpty()) "Data tidak tersedia" else "Data sebagian • ${summary.available.size}/${summary.requestedDays} hari",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable fun DailyUsageChart(days: List<UsageDay>,selectedDate: LocalDate,onSelect: (LocalDate)->Unit) {
    val max=days.filter {it.available}.maxOfOrNull {it.totalMs}?.coerceAtLeast(60000)?:60000
    val names=listOf("Sen","Sel","Rab","Kam","Jum","Sab","Min")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val columnWidth = if(days.size <= 7) maxWidth / days.size.coerceAtLeast(1) else 48.dp
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(if(days.size <= 7) 0.dp else 4.dp),verticalAlignment=Alignment.Bottom) {
        days.forEach {day ->
            val chosen=day.date==selectedDate
            Column(Modifier.width(columnWidth).clip(RoundedCornerShape(12.dp)).clickable {onSelect(day.date)}.padding(vertical=8.dp)
                .semantics(mergeDescendants=true) {contentDescription="${day.date}: ${if(day.available) readableDuration(day.totalMs) else "data tidak tersedia"}";selected=chosen},
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(day.available) duration(day.totalMs) else "—",Modifier.heightIn(min=36.dp),style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center)
                Box(Modifier.height(96.dp).width(24.dp),contentAlignment=Alignment.BottomCenter) {
                    if(day.available&&day.totalMs>0) Box(Modifier.width(22.dp).fillMaxHeight((day.totalMs.toFloat()/max).coerceIn(.015f,1f)).clip(RoundedCornerShape(5.dp)).background(if(chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha=.42f)))
                    else Text(if(day.available) "0" else "—",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(names[day.date.dayOfWeek.value-1],style=MaterialTheme.typography.labelSmall,color=if(chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${day.date.dayOfMonth}",style=MaterialTheme.typography.labelMedium)
            }
        }
    }
    }
}
@Composable fun ActivityScreen(device: DeviceData,back: (()->Unit)?=null,refresh: ()->Unit={},permissions: ()->Unit={},selected: (InstalledApp,LocalDate,Int)->Unit) {
    var dateText by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
    var period by rememberSaveable {mutableIntStateOf(0)}
    var query by rememberSaveable {mutableStateOf("")}
    var info by remember {mutableStateOf(false)}
    val date=LocalDate.parse(dateText)
    val count=listOf(1,7,30)[period]
    val summary=ActivitySummary.of(device.days,date,count)
    val sorted=summary.apps.entries.sortedByDescending {it.value}.map {activityApp(it.key,device.apps) to it.value}.filter {it.first.label.contains(query,true)||it.first.packageName.contains(query,true)}
    val chart=if(count==1) device.days.filter {!it.date.isAfter(date)}.takeLast(7) else summary.days
    Column {
        PageHeader("Aktivitas",back,action={IconButton(refresh,enabled=!device.loading) {Icon(Icons.Outlined.Refresh,"Perbarui statistik")}})
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,8.dp,20.dp,24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {PillTabs(listOf("Hari","7 hari","30 hari"),period) {period=it}}
            item {DateRangeControl(date,count,device.days.firstOrNull()?.date) {dateText=it.toString()}}
            if(!device.usageAccess) item {FilledTonalButton(permissions,Modifier.fillMaxWidth()) {Text("Aktifkan akses statistik")}}
            item {GuardCard(spacing=20.dp) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Waktu layar",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton({info=true}) {Icon(Icons.Outlined.Info,"Tentang waktu layar",Modifier.size(20.dp))}
                }
                Text(if(device.usageAccess&&summary.available.isNotEmpty()) readableDuration(summary.screenMs) else "—",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.testTag("activity-total"))
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    ActivityMetric("Aplikasi digunakan",if(summary.available.isEmpty()) "—" else "${summary.apps.size}",Modifier.weight(1f))
                    ActivityMetric("Buka kunci",if(summary.available.isEmpty()) "—" else "${summary.unlocks}",Modifier.weight(1f))
                }
                if(count>1) Text("Rata-rata ${readableDuration(summary.screenMs/summary.available.size.coerceAtLeast(1))}/hari",style=MaterialTheme.typography.bodyMedium)
                Coverage(summary)
            }}
            item {GuardCard {
                SectionTitle(if(count==30) "Aktivitas 30 hari" else "Aktivitas harian")
                DailyUsageChart(chart,date) {dateText=it.toString();period=0}
            }}
            item {SectionTitle("Penggunaan aplikasi", "${summary.apps.size} aplikasi")}
            item {OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Cari aplikasi")},singleLine=true,leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(18.dp))}
            if(sorted.isEmpty()&&device.usageAccess) item {Text(if(summary.available.isEmpty()) "Data tidak tersedia" else "Tidak ada penggunaan",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(sorted,key={it.first.packageName}) {(app,ms) -> GuardCard(contentPadding=12.dp,spacing=0.dp) {
                UsageRow(app,ms,summary.appMs) {selected(app,date,count)}
            }}
        }
    }
    if(info) AlertDialog(onDismissRequest={info=false},title={Text("Waktu layar")},text={Text("Durasi layar aktif saat tidak terkunci. Durasi per aplikasi dihitung terpisah; dua aplikasi yang tampil bersamaan tidak menggandakan total layar.\n\nData sebagian berarti Android hanya menyediakan sebagian riwayat periode ini.")},confirmButton={TextButton({info=false}) {Text("Tutup")}})
}
