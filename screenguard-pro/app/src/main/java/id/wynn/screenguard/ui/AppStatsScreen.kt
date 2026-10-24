package id.wynn.screenguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import id.wynn.screenguard.data.*
import id.wynn.screenguard.domain.*
import java.time.LocalDate

@Composable fun AppStatsScreen(app: InstalledApp,device: DeviceData,back: ()->Unit,loadDetails: suspend (String,LocalDate)->AppDayDetail,
    protect: ()->Unit,initialDate: String?=null,initialPeriod: Int=1) {
    var dateText by rememberSaveable(app.packageName) {mutableStateOf(initialDate?:LocalDate.now().toString())}
    var period by rememberSaveable(app.packageName) {mutableIntStateOf(listOf(1,7,30).indexOf(initialPeriod).coerceAtLeast(0))}
    var allHours by rememberSaveable {mutableStateOf(false)}
    var hour by rememberSaveable {mutableIntStateOf(-1)}
    val date=LocalDate.parse(dateText)
    val count=listOf(1,7,30)[period]
    val summary=ActivitySummary.of(device.days,date,count)
    val total=summary.apps[app.packageName]?:0
    var detail by remember(app.packageName,date,count) {mutableStateOf<AppDayDetail?>(null)}
    var error by remember(app.packageName,date,count) {mutableStateOf(false)}
    LaunchedEffect(app.packageName,date,count,device.refreshedAt) {
        try {
            val list=summary.available.map {loadDetails(app.packageName,it.date)}
            detail=AppDayDetail(date,List(24) {h->list.sumOf {it.hourly.getOrElse(h) {0}}},list.sumOf {it.opens},list.maxOfOrNull {it.longestMs}?:0,list.any {it.eventsAvailable})
        } catch(e: Exception) {if(e is kotlinx.coroutines.CancellationException) throw e;error=true}
    }
    val chart=(if(count==1) device.days.filter {!it.date.isAfter(date)}.takeLast(7) else summary.days).map {it.copy(screenMs=it.apps[app.packageName]?:0)}
    val hours=detail?.hourly?:List(24) {0L}
    val peak=hours.indices.maxByOrNull {hours[it]}?.takeIf {hours[it]>0}
    Column {
        PageHeader("Statistik aplikasi",back)
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,8.dp,20.dp,24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {AppIcon(app,56.dp);Text(app.label,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f))}}
            item {PillTabs(listOf("Hari","7 hari","30 hari"),period) {period=it;hour=-1}}
            item {DateRangeControl(date,count,device.days.firstOrNull()?.date) {dateText=it.toString();hour=-1}}
            item {GuardCard(spacing=20.dp) {
                Text("Durasi penggunaan",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if(device.usageAccess&&summary.available.isNotEmpty()) readableDuration(total) else "—",style=MaterialTheme.typography.headlineLarge)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    ActivityMetric("Sesi",detail?.takeIf {it.eventsAvailable}?.opens?.toString()?:"—",Modifier.weight(1f))
                    ActivityMetric("Sesi terlama",detail?.takeIf {it.eventsAvailable}?.let {duration(it.longestMs)}?:"—",Modifier.weight(1f))
                }
                HorizontalDivider()
                ValueRow("Bagian penggunaan aplikasi",if(summary.appMs>0) "${total*100/summary.appMs}%" else "—")
                if(count>1) ValueRow("Rata-rata per hari",readableDuration(total/summary.available.size.coerceAtLeast(1)))
                Coverage(summary)
            }}
            item {GuardCard {SectionTitle("Per hari");DailyUsageChart(chart,date) {dateText=it.toString();period=0}}}
            item {GuardCard {
                SectionTitle(if(count==1) "Per jam" else "Jam aktif")
                when {
                    error -> Text("Rincian tidak tersedia",color=MaterialTheme.colorScheme.error)
                    detail==null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    detail?.eventsAvailable!=true -> Text("Data per jam tidak tersedia",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {
                        HourActivityChart(hours,hour) {hour=it}
                        if(hour>=0) ValueRow("%02d.00–%02d.00".format(hour,hour+1),readableDuration(hours[hour]))
                        if(peak!=null) ValueRow("Jam paling aktif","%02d.00 • %s".format(peak,duration(hours[peak])))
                    }
                }
            }}
            if(detail?.eventsAvailable==true) {
                item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Rincian per jam",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    TextButton({allHours=!allHours}) {Text(if(allHours) "Jam aktif" else "24 jam")}
                }}
                items((0..23).filter {allHours||hours[it]>0},key={it}) {h ->
                    ValueRow("%02d.00–%02d.00".format(h,h+1),readableDuration(hours[h]))
                }
            }
            if(app.launchable&&!app.protected) item {FilledTonalButton(protect,Modifier.fillMaxWidth()) {Icon(Icons.Outlined.AcUnit,null);Spacer(Modifier.width(8.dp));Text("Buka Bekukan")}}
        }
    }
}
@Composable fun ValueRow(label: String,value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,style=MaterialTheme.typography.titleMedium)
    }
}
@Composable fun HourActivityChart(hours: List<Long>,selected: Int,onSelect: (Int)->Unit) {
    val max=hours.maxOrNull()?.coerceAtLeast(60000)?:60000
    val color=MaterialTheme.colorScheme.primary
    Column {
        // Pointer selection supplements the full text list; values remain accessible without gestures.
        Canvas(Modifier.fillMaxWidth().height(100.dp).pointerInput(hours) { detectTapGestures { onSelect((it.x / size.width * 24).toInt().coerceIn(0,23)) } }.semantics {contentDescription="Penggunaan per jam: "+hours.mapIndexedNotNull {h,ms->if(ms>0) "$h.00 ${readableDuration(ms)}" else null}.joinToString()}) {
            repeat(3) {n->drawLine(color.copy(alpha=.12f),Offset(0f,size.height*n/2),Offset(size.width,size.height*n/2),1.dp.toPx())}
            hours.forEachIndexed {h,ms->
                val slot=size.width/24
                val height=if(ms>0) (ms.toFloat()/max*size.height).coerceAtLeast(2.dp.toPx()) else 1.dp.toPx()
                drawRoundRect(color.copy(alpha=if(ms==0L) .12f else if(h==selected) 1f else .65f),Offset(slot*h+slot*.2f,size.height-height),Size(slot*.6f,height),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {listOf("00","06","12","18","24").forEach {Text(it,style=MaterialTheme.typography.bodySmall)}}
    }
}
