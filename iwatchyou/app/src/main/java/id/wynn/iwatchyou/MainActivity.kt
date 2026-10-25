package id.wynn.iwatchyou

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Ink=Color(0xFF142033)
private val Muted=Color(0xFF637087)
private val Blue=Color(0xFF347CF4)
private val Back=Color(0xFFF6F8FC)
private val Track=Color(0xFFE9EEF6)
private val Jakarta=FontFamily(Font(R.font.jakarta_regular,FontWeight.Normal),Font(R.font.jakarta_medium,FontWeight.Medium),Font(R.font.jakarta_bold,FontWeight.Bold))
private val Palette=listOf(Blue,Color(0xFFAB76E8),Color(0xFFF26E83),Color(0xFFE3AD38),Color(0xFF62BA91),Color(0xFF8C9EB7))
private fun typography(): Typography {
 val t=Typography()
 return Typography(displayLarge=t.displayLarge.copy(fontFamily=Jakarta),displayMedium=t.displayMedium.copy(fontFamily=Jakarta),displaySmall=t.displaySmall.copy(fontFamily=Jakarta),
 headlineLarge=t.headlineLarge.copy(fontFamily=Jakarta),headlineMedium=t.headlineMedium.copy(fontFamily=Jakarta),headlineSmall=t.headlineSmall.copy(fontFamily=Jakarta),
 titleLarge=t.titleLarge.copy(fontFamily=Jakarta),titleMedium=t.titleMedium.copy(fontFamily=Jakarta),titleSmall=t.titleSmall.copy(fontFamily=Jakarta),
 bodyLarge=t.bodyLarge.copy(fontFamily=Jakarta),bodyMedium=t.bodyMedium.copy(fontFamily=Jakarta),bodySmall=t.bodySmall.copy(fontFamily=Jakarta),
 labelLarge=t.labelLarge.copy(fontFamily=Jakarta),labelMedium=t.labelMedium.copy(fontFamily=Jakarta),labelSmall=t.labelSmall.copy(fontFamily=Jakarta))
}
class MainActivity: ComponentActivity() {
 private var update by mutableStateOf<UpdateManager.Release?>(null)
 private var message by mutableStateOf<String?>(null)
 private var setup by mutableStateOf(false)
 private var busy by mutableStateOf(false)
 private var continueSetup=false
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {
  WatchService.instance?.refreshNotification()
  if(continueSetup) { continueSetup=false; openAccessibility() }
 }
 private fun hasNotifications()=Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
 private fun openAccessibility() {
  val detail=Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(Intent.EXTRA_COMPONENT_NAME,ComponentName(this,WatchService::class.java).flattenToString())
  if(runCatching { startActivity(detail) }.isFailure) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
 }
 private fun begin() {
  setup=false
  if(!hasNotifications() && Build.VERSION.SDK_INT>=33) { continueSetup=true; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
  else openAccessibility()
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge(statusBarStyle=androidx.activity.SystemBarStyle.light(0,0),navigationBarStyle=androidx.activity.SystemBarStyle.light(0,0))
  setContent {
   MaterialTheme(colorScheme=lightColorScheme(primary=Blue,background=Back,surface=Color.White,onSurface=Ink,onSurfaceVariant=Muted),typography=remember { typography() }) {
    Page()
    if(setup) AlertDialog(onDismissRequest={setup=false},title={Text("Aktifkan pemantauan")},text={Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
     Text("Stopwatch muncul otomatis saat aplikasi digunakan dan berhenti saat layar terkunci.")
     Text("Aksesibilitas mengenali nama aplikasi dari jendela yang mendapat fokus. Isi layar, pesan, dan ketikan tidak dikumpulkan. Riwayat hanya disimpan di perangkat.",style=MaterialTheme.typography.bodyMedium)
     Text("1. Izinkan notifikasi waktu layar.\n2. Pilih IWatchYou di Aksesibilitas dan aktifkan.",style=MaterialTheme.typography.bodyMedium)
     if(Build.MANUFACTURER.equals("xiaomi",true)) Text("Jika HyperOS menghentikan pemantauan, izinkan mulai otomatis dan pilih baterai Tanpa batas pada info aplikasi.",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={begin()}) { Text("Lanjutkan") }},dismissButton={TextButton(onClick={setup=false}) { Text("Nanti") }})
    update?.let { release -> AlertDialog(onDismissRequest={if(!busy)update=null},title={Text("Pembaruan ${release.version}")},text={Text(if(busy) "Mengunduh pembaruan…" else "${release.notes}\n\nUkuran: ${"%.1f".format(Locale.US,release.size/1_000_000.0)} MB")},confirmButton={TextButton(enabled=!busy,onClick={busy=true; UpdateManager.install(this@MainActivity,release) { busy=false; update=null; message=it }}){Text(if(busy)"Mengunduh…" else "Unduh & pasang")}},dismissButton={TextButton(enabled=!busy,onClick={update=null}){Text("Nanti")}}) }
    message?.let { text -> AlertDialog(onDismissRequest={message=null},title={Text("IWatchYou")},text={Text(text)},confirmButton={TextButton(onClick={message=null}){Text("Tutup")}}) }
   }
  }
  UpdateManager.check(this) { r,_ -> update=r }
 }
 override fun onResume() { super.onResume(); WatchService.instance?.refreshNotification(); UpdateManager.resumeInstall(this) { message=it } }
 @Composable private fun Page() {
  var data by remember { mutableStateOf(TimeLedger.Snapshot(WatchService.today(),0,emptyMap(),null)) }
  var active by remember { mutableStateOf(false) }
  var dates by remember { mutableStateOf(emptyList<String>()) }
  var selected by remember { mutableStateOf<String?>(null) }
  var dateMenu by remember { mutableStateOf(false) }
  var settings by remember { mutableStateOf(false) }
  val owner=LocalLifecycleOwner.current
  LaunchedEffect(owner,selected) {
   owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
    val saved=withContext(Dispatchers.IO) { HistoryStore(applicationContext).use { store -> Pair(store.read(selected ?: WatchService.today()),store.days()) } }
    dates=(listOf(WatchService.today())+saved.second).distinct()
    data=saved.first ?: TimeLedger.Snapshot(selected ?: WatchService.today(),0,emptyMap(),null)
    while(isActive) {
     active=WatchService.instance!=null
     val current=WatchService.instance
     if(selected==null && current!=null) data=current.current()
     if(selected==null && data.day!=WatchService.today()) data=TimeLedger.Snapshot(WatchService.today(),0,emptyMap(),null)
     delay(1000)
    }
   }
  }
  val entries=remember(data.apps) { data.apps.entries.filter { it.value>=1000 }.sortedByDescending { it.value } }
  Column(Modifier.fillMaxSize().background(Back).windowInsetsPadding(WindowInsets.safeDrawing)) {
   LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(20.dp,22.dp,20.dp,18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
    item {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
       Text("IWatchYou",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.semantics { heading() })
       Spacer(Modifier.height(4.dp))
       Text("Waktu layar, lebih sadar setiap hari.",fontSize=12.sp,color=Muted)
      }
      Box {
       IconButton(onClick={settings=true}) { Icon(Icons.Outlined.Settings,contentDescription="Pengaturan",tint=Ink) }
       DropdownMenu(expanded=settings,onDismissRequest={settings=false}) {
        DropdownMenuItem(text={Text("Periksa pembaruan")},onClick={settings=false; UpdateManager.check(this@MainActivity,true){r,e -> if(r!=null)update=r else message=e }})
        DropdownMenuItem(text={Text("Izin & layanan")},onClick={settings=false; setup=true})
        DropdownMenuItem(text={Text("Info aplikasi & baterai")},onClick={settings=false; startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName")))})
       }
      }
     }
    }
    item {
     Surface(color=Color.White,shape=RoundedCornerShape(26.dp),tonalElevation=0.dp,shadowElevation=1.dp,modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(56.dp).background(Back,CircleShape),contentAlignment=Alignment.Center) { ClockGlyph(Modifier.size(28.dp),Ink) }
        Column(Modifier.weight(1f)) {
         Text(if(selected==null)"Waktu layar aktif hari ini" else "Waktu layar aktif",fontSize=12.sp,color=Muted)
         Spacer(Modifier.height(4.dp))
         Text(WatchService.short(data.screenMs),fontSize=27.sp,fontWeight=FontWeight.Bold,color=Ink,style=TextStyle(fontFeatureSettings="tnum"))
        }
       }
       HourChart(data.hours)
       Text("Durasi layar terbuka. Aplikasi dicatat saat mendapat fokus.",fontSize=11.sp,lineHeight=17.sp,color=Muted)
      }
     }
    }
    item {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
      Text("Penggunaan aplikasi",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f).semantics { heading() })
      Box {
       TextButton(onClick={dateMenu=true}) { Text(if(selected==null)"Hari ini ▾" else dayLabel(selected!!)+" ▾",fontSize=12.sp,color=Muted) }
       DropdownMenu(expanded=dateMenu,onDismissRequest={dateMenu=false},modifier=Modifier.heightIn(max=360.dp)) {
        (listOf(WatchService.today())+dates).distinct().forEach { day -> DropdownMenuItem(text={Text(if(day==WatchService.today())"Hari ini" else dayLabel(day))},onClick={selected=day.takeUnless { it==WatchService.today() };dateMenu=false}) }
       }
      }
     }
    }
    if(entries.isEmpty()) item {
     Surface(color=Color.White,shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
       Text(if(active)"Belum ada penggunaan aplikasi" else "Mulai lebih sadar waktu",fontWeight=FontWeight.Medium,color=Ink,fontSize=14.sp)
       Text(if(selected!=null)"Tidak ada aktivitas aplikasi yang tercatat pada tanggal ini." else if(active)"Buka aplikasi lain. Waktunya akan tercatat otomatis." else "Aktifkan pemantauan melalui kartu di bawah.",color=Muted,fontSize=12.sp,lineHeight=19.sp)
      }
     }
    }
    items(entries,key={it.key}) { entry ->
     AppRow(entry.key,entry.value,entries.first().value,Palette[entries.indexOf(entry)%Palette.size])
    }
    item { Text("Timer meredup setelah 3 detik. Ketuk dahulu untuk menggesernya.",fontSize=11.sp,lineHeight=18.sp,color=Muted,modifier=Modifier.padding(horizontal=3.dp)) }
   }
   val stateColor by animateColorAsState(if(active)Blue else Muted,label="monitoring")
   Surface(color=Color.White,shape=RoundedCornerShape(24.dp),shadowElevation=2.dp,modifier=Modifier.padding(start=16.dp,end=16.dp,bottom=12.dp).fillMaxWidth()) {
    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     Box(Modifier.size(46.dp).background(stateColor.copy(alpha=0.10f),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) { Text("⏻",color=stateColor,fontSize=28.sp) }
     Column(Modifier.weight(1f)) {
      Text(if(active)"Pemantauan aktif" else "Aktifkan pemantauan",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Ink)
      Spacer(Modifier.height(3.dp))
      Text(if(active)"Timer otomatis saat aplikasi digunakan" else "Satu kali izin, berjalan otomatis",fontSize=10.sp,lineHeight=16.sp,color=Muted)
     }
     Switch(checked=active,onCheckedChange={if(it)setup=true else WatchService.instance?.disableSelf()},modifier=Modifier.semantics { contentDescription="Pemantauan" })
    }
   }
  }
 }
 @Composable private fun AppRow(pkg: String, duration: Long, max: Long, color: Color) {
  // Package metadata is disk/Binder work and must not block composition or every timer tick.
  val info by produceState<Pair<String,android.graphics.drawable.Drawable?>?>(null,pkg) {
   value=withContext(Dispatchers.IO) { runCatching { val app=packageManager.getApplicationInfo(pkg,0); packageManager.getApplicationLabel(app).toString() to packageManager.getApplicationIcon(app) }.getOrElse { pkg.substringAfterLast('.') to null } }
  }
  val progress by animateFloatAsState((duration.toFloat()/max.coerceAtLeast(1)).coerceIn(0f,1f),label="usage")
  Surface(color=Color.White,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
   Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) {
    val icon=info?.second
    if(icon!=null) AndroidView(factory={ImageView(it).apply { scaleType=ImageView.ScaleType.FIT_CENTER }},update={it.setImageDrawable(icon)},modifier=Modifier.size(38.dp).clearAndSetSemantics {})
    else Box(Modifier.size(38.dp).background(Back,RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center) { Text(info?.first?.take(1) ?: "·",color=Muted) }
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Text(info?.first ?: "Aplikasi",modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=13.sp,fontWeight=FontWeight.Medium,color=Ink)
      Text(WatchService.short(duration),fontSize=11.sp,color=Ink)
     }
     LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(5.dp).clearAndSetSemantics {},color=color,trackColor=Track,strokeCap=androidx.compose.ui.graphics.StrokeCap.Round,drawStopIndicator={})
    }
   }
  }
 }
}
private fun dayLabel(day: String)=runCatching { LocalDate.parse(day).format(DateTimeFormatter.ofPattern("d MMM yyyy",Locale.forLanguageTag("id"))) }.getOrDefault(day)
@Composable private fun ClockGlyph(modifier: Modifier,color: Color) {
 Canvas(modifier) {
  val stroke=2.dp.toPx(); val c=center; val r=size.minDimension*0.42f
  drawCircle(color,r,c,style=androidx.compose.ui.graphics.drawscope.Stroke(stroke))
  drawLine(color,c,c.copy(y=c.y-r*0.6f),stroke,androidx.compose.ui.graphics.StrokeCap.Round)
  drawLine(color,c,c.copy(x=c.x+r*0.45f,y=c.y+r*0.25f),stroke,androidx.compose.ui.graphics.StrokeCap.Round)
 }
}
@Composable private fun HourChart(hours: List<Long>) {
 val max=(hours.maxOrNull() ?: 0L).coerceAtLeast(60_000)
 Column(verticalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.semantics(mergeDescendants=true) { contentDescription="Penggunaan layar per jam, pukul 00 sampai 24" }) {
  Row(Modifier.fillMaxWidth().height(42.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.Bottom) {
   repeat(24) { i ->
    val value=hours.getOrElse(i){0L}
    Box(Modifier.weight(1f).fillMaxHeight(if(value==0L)0.08f else (value.toFloat()/max).coerceIn(0.10f,1f)).background(if(value>0)Blue.copy(alpha=0.55f) else Track,RoundedCornerShape(3.dp)))
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { listOf("00","04","08","12","16","20","24").forEach { Text(it,color=Muted,fontSize=9.sp) } }
 }
}
