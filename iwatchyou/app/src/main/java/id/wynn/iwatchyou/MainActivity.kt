package id.wynn.iwatchyou

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import kotlinx.coroutines.delay

private val Ink=Color(0xFF111D34)
private val Muted=Color(0xFF7A869C)
private val Blue=Color(0xFF397DF6)
private val Back=Color(0xFFF6F8FC)
private val Border=Color(0xFFE9EDF5)
private val Jakarta=FontFamily(Font(R.font.jakarta_regular,FontWeight.Normal),Font(R.font.jakarta_medium,FontWeight.Medium),Font(R.font.jakarta_bold,FontWeight.Bold))

class MainActivity : ComponentActivity() {
 private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
 private var update by mutableStateOf<UpdateManager.Release?>(null)
 private var message by mutableStateOf<String?>(null)
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor=Back.toArgb()
  window.navigationBarColor=Back.toArgb()
  window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  setContent {
   MaterialTheme(colorScheme=lightColorScheme(primary=Blue,background=Back,surface=Color.White,onSurface=Ink),typography=Typography(bodyLarge=androidx.compose.ui.text.TextStyle(fontFamily=Jakarta),bodyMedium=androidx.compose.ui.text.TextStyle(fontFamily=Jakarta),bodySmall=androidx.compose.ui.text.TextStyle(fontFamily=Jakarta),titleLarge=androidx.compose.ui.text.TextStyle(fontFamily=Jakarta),titleMedium=androidx.compose.ui.text.TextStyle(fontFamily=Jakarta),labelLarge=androidx.compose.ui.text.TextStyle(fontFamily=Jakarta))) {
    Page()
    val next=update
    if(next!=null) AlertDialog(
     onDismissRequest={update=null},
     title={Text("Pembaruan IWatchYou")},
     text={Text("Versi ${next.version} tersedia. APK akan diperiksa checksum, identitas paket, dan sertifikatnya sebelum pemasangan.")},
     confirmButton={TextButton(onClick={
      update=null
      message="Mengunduh pembaruan..."
      UpdateManager.install(this@MainActivity,next) { message=it }
     }){Text("Unduh & pasang")}},
     dismissButton={TextButton(onClick={update=null}){Text("Nanti")}}
    )
    val notice=message
    if(notice!=null) AlertDialog(onDismissRequest={message=null},title={Text("IWatchYou")},text={Text(notice)},confirmButton={TextButton(onClick={message=null}){Text("Tutup")}})
   }
  }
  UpdateManager.check(this) { r,_ -> if(r!=null) update=r }
 }
 override fun onResume() {
  super.onResume()
  if (WatchService.instance != null && Build.VERSION.SDK_INT>=33 &&
   checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
   val p=getSharedPreferences("updater",MODE_PRIVATE)
   if(!p.getBoolean("notice_requested",false)) {
    p.edit().putBoolean("notice_requested",true).apply()
    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
   }
  }
 }
 @Composable private fun Page() {
  var data by remember { mutableStateOf(WatchService.readSnapshot(this)) }
  var active by remember { mutableStateOf(WatchService.instance!=null) }
  LaunchedEffect(Unit) {
   while(true) {
    data=WatchService.readSnapshot(this@MainActivity)
    active=WatchService.instance!=null
    delay(1000)
   }
  }
  Column(modifier=Modifier.fillMaxSize().background(Back)) {
   LazyColumn(modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=25.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    item {
     Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) {
      Column(modifier=Modifier.weight(1f)) {
       Text("IWatchYou",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink)
       Text("Waktu layar, lebih sadar setiap hari.",fontSize=12.sp,color=Muted)
      }
      IconButton(onClick={UpdateManager.check(this@MainActivity,true){r,e-> if(r!=null) update=r else if(e!=null)message=e}}) {
       Icon(Icons.Outlined.Settings,contentDescription="Periksa pembaruan",tint=Ink)
      }
     }
    }
    item {
     Surface(color=Color.White,shape=RoundedCornerShape(26.dp),shadowElevation=3.dp,modifier=Modifier.fillMaxWidth()) {
      Column(Modifier.padding(23.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(15.dp)) {
        Box(Modifier.size(62.dp).background(Back,CircleShape),contentAlignment=Alignment.Center) {
         Icon(Icons.Outlined.AccessTime,contentDescription=null,tint=Blue,modifier=Modifier.size(30.dp))
        }
        Column {
         Text("Waktu layar aktif hari ini",color=Muted,fontSize=13.sp)
         Text(WatchService.short(data.screenMs),fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink)
        }
       }
       val sorted=data.apps.values.sortedDescending().take(8)
       val max=(sorted.maxOrNull() ?: 1L).coerceAtLeast(1)
       Row(modifier=Modifier.fillMaxWidth().height(42.dp),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.Bottom) {
        if(sorted.isEmpty()) repeat(15) {
         Box(Modifier.weight(1f).fillMaxHeight((0.12f+(it%5)*0.08f)).background(Border,RoundedCornerShape(4.dp)))
        } else sorted.forEachIndexed { i,v ->
         Box(Modifier.weight(1f).fillMaxHeight((0.12f+0.88f*v/max).toFloat()).background(Blue.copy(alpha=(0.35f+i*0.07f).coerceAtMost(1f)),RoundedCornerShape(4.dp)))
        }
       }
       Text("Hanya saat layar aktif. Aplikasi di latar belakang tidak dihitung.",color=Muted,fontSize=11.sp,lineHeight=17.sp)
      }
     }
    }
    item {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
      Text("Penggunaan aplikasi",color=Ink,fontWeight=FontWeight.Bold,fontSize=16.sp)
      Text("Hari ini",color=Muted,fontSize=12.sp)
     }
    }
    if(data.apps.isEmpty()) item {
     Surface(color=Color.White,shape=RoundedCornerShape(22.dp)) {
      Text(if(active)"Buka aplikasi lain untuk mulai mencatat waktu." else "Aktifkan pemantauan melalui kartu di bawah.",modifier=Modifier.padding(22.dp),color=Muted,fontSize=13.sp)
     }
    }
    val sorted=data.apps.entries.sortedByDescending { it.value }
    items(sorted,key={it.key}) { item ->
     val p=(item.value.toFloat()/(sorted.maxOfOrNull{it.value}?.coerceAtLeast(1L) ?: 1L)).coerceIn(0f,1f)
     Surface(color=Color.White,shape=RoundedCornerShape(19.dp),modifier=Modifier.fillMaxWidth()) {
      Row(Modifier.padding(horizontal=13.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
       val icon=remember(item.key) { try {packageManager.getApplicationIcon(item.key)}catch(_:Exception){null} }
       if(icon!=null) AndroidView(factory={ImageView(it).apply { setImageDrawable(icon);scaleType=ImageView.ScaleType.FIT_CENTER }},modifier=Modifier.size(37.dp))
       else Box(Modifier.size(37.dp).background(Back,RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center) { Text("•",color=Blue) }
       Column(modifier=Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
         val name=remember(item.key) {
          try { val info=packageManager.getApplicationInfo(item.key,0);packageManager.getApplicationLabel(info).toString() }
          catch(_:Exception){item.key.substringAfterLast(".")}
         }
         Text(name,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=13.sp,fontWeight=FontWeight.Medium,color=Ink)
         Text(WatchService.short(item.value),fontSize=12.sp,color=Ink)
        }
        LinearProgressIndicator(progress={p},modifier=Modifier.fillMaxWidth().height(5.dp),color=Blue,trackColor=Border,strokeCap=androidx.compose.ui.graphics.StrokeCap.Round)
       }
      }
     }
    }
   }
   Surface(color=Color.White,shadowElevation=10.dp,shape=RoundedCornerShape(topStart=22.dp,topEnd=22.dp)) {
    Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=15.dp),verticalAlignment=Alignment.CenterVertically) {
     Box(Modifier.size(48.dp).background(if(active) Color(0xFFE6F0FF) else Back,CircleShape),contentAlignment=Alignment.Center) {
      Icon(Icons.Outlined.PowerSettingsNew,contentDescription=null,tint=if(active) Blue else Muted)
     }
     Spacer(Modifier.width(12.dp))
     Column(Modifier.weight(1f)) {
      Text(if(active)"Pemantauan aktif" else "Aktifkan pemantauan",fontSize=14.sp,color=Ink,fontWeight=FontWeight.Bold)
      Text(if(active)"Timer otomatis saat aplikasi dibuka" else "Izinkan aksesibilitas untuk memulai",fontSize=11.sp,color=Muted)
     }
     Switch(checked=active,onCheckedChange={ turnOn ->
      if(turnOn) {
       startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
       message="Pilih IWatchYou di Aksesibilitas lalu aktifkan layanannya. IWatchYou tidak membaca isi layar."
      } else WatchService.instance?.disableSelf()
     })
    }
   }
  }
 }
}
