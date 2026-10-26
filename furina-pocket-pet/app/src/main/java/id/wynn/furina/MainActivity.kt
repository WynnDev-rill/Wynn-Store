package id.wynn.furina

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Blue=Color(0xFF3973DF)
private val Ink=Color(0xFF17253B)
private val Muted=Color(0xFF758197)
private val Pale=Color(0xFFF1F5FF)

class MainActivity : ComponentActivity() {
 private val configState=mutableStateOf(PetConfig())
 private val overlayState=mutableStateOf(false)
 private lateinit var prefs: PetSettings
 private lateinit var changes: SharedPreferences.OnSharedPreferenceChangeListener
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); enableEdgeToEdge()
  prefs=PetSettings(this); configState.value=prefs.read()
  changes=SharedPreferences.OnSharedPreferenceChangeListener { _,_ -> configState.value=prefs.read() }
  getSharedPreferences("pet",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(changes)
  setContent {
   MaterialTheme(colorScheme=lightColorScheme(primary=Blue,onPrimary=Color.White,background=Color(0xFFF7F9FD),surface=Color.White,onSurface=Ink,onBackground=Ink,secondary=Muted),typography=Typography()) {
    SettingsScreen()
   }
  }
 }
 override fun onResume() { super.onResume(); overlayState.value=Settings.canDrawOverlays(this); if(::prefs.isInitialized) configState.value=prefs.read() }
 override fun onDestroy() {
  if(::changes.isInitialized) getSharedPreferences("pet",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(changes)
  super.onDestroy()
 }
 private fun open(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } }
 private fun save(c: PetConfig) {
  prefs.save(c); configState.value=c
  if(PetRuntime.status.value.running) runCatching { PetService.command(this,PetService.REFRESH) }
 }
 @Composable private fun SettingsScreen() {
  val c by configState
  val status by PetRuntime.status.collectAsStateWithLifecycle()
  val scope=rememberCoroutineScope()
  var info by remember { mutableStateOf<ModelInfo?>(null) }
  var available by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
  var importing by remember { mutableStateOf(false) }
  var consent by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf("") }
  var updateText by remember { mutableStateOf("") }
  var update by remember { mutableStateOf<AppUpdate?>(null) }
  var checking by remember { mutableStateOf(false) }
  var modelMenu by remember { mutableStateOf(false) }
  val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
  suspend fun refreshModel() { withContext(Dispatchers.IO) {
   val store=ModelStore(this@MainActivity)
   val data=runCatching { store.inspect(store.selected(prefs.read())) }
   val files=store.list()
   withContext(Dispatchers.Main) { info=data.getOrNull(); available=files; data.exceptionOrNull()?.let { message=it.message ?: "Gagal membaca model" } }
  } }
  fun checkUpdates() { scope.launch {
   checking=true
   val result=withContext(Dispatchers.IO) { runCatching { UpdateChecker.check() } }
   update=result.getOrNull()
   updateText=result.fold({ if(it==null) "Belum ada versi baru yang dipublikasikan." else "Versi ${it.version} tersedia." },{ "Pemeriksaan gagal. Coba lagi saat tersambung internet." })
   checking=false
  } }
  val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
   if(uri!=null) scope.launch {
    importing=true
    // Keep the current pet/model until a complete import validates successfully.
    val result=withContext(Dispatchers.IO) { runCatching { ModelStore(this@MainActivity).importZip(uri) } }
    result.onSuccess { m -> save(prefs.read().copy(model=m.file.path,tapMotion="",waveMotion="",expression="")); message="Model berhasil diimpor. Pilih aksi yang tersedia di bawah." }
     .onFailure { message=it.message ?: "Impor gagal" }
    refreshModel(); importing=false
   }
  }
  LaunchedEffect(c.model) { refreshModel() }
  LaunchedEffect(Unit) { checkUpdates() }
  Scaffold(containerColor=MaterialTheme.colorScheme.background) { padding ->
   Box(Modifier.fillMaxSize().padding(padding)) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(top=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Box(Modifier.size(44.dp).background(Blue,RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center) { Text("f",color=Color.White,fontSize=31.sp,fontWeight=FontWeight.Medium) }
      Column(Modifier.padding(start=12.dp).weight(1f)) {
       Text("Furina",fontWeight=FontWeight.Bold,fontSize=24.sp,letterSpacing=(-0.5).sp)
       Text("POCKET PET",fontSize=10.sp,color=Muted,letterSpacing=2.sp,fontWeight=FontWeight.Medium)
      }
      Text("${BuildConfig.VERSION_NAME}",color=Muted,fontSize=12.sp)
     }
     Surface(color=Pale,shape=RoundedCornerShape(28.dp)) {
      Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(if(status.running) Color(0xFF4EAC84) else Muted,CircleShape))
        Text(when { !status.running -> "SIAP MENEMANI"; c.hidden -> "DISEMBUNYIKAN"; c.paused -> "SEDANG JEDA"; else -> "SEDANG MENEMANI" },Modifier.padding(start=8.dp),fontSize=10.sp,letterSpacing=1.2.sp,color=Muted,fontWeight=FontWeight.SemiBold)
       }
       Text("Teman kecil,\ndi mana saja.",fontSize=32.sp,lineHeight=38.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-1).sp)
       Text(if(c.model.isEmpty()) "Model uji WhaleGirl aktif. Impor model Furina milikmu untuk menggantinya." else "${info?.name ?: "Model lokal"} · Live2D",fontSize=13.sp,color=Muted,lineHeight=20.sp)
       Button(onClick={
        if(status.running) PetService.command(this@MainActivity,PetService.STOP)
        else if(!Settings.canDrawOverlays(this@MainActivity)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
        else {
         if(Build.VERSION.SDK_INT>=33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
         save(prefs.read().copy(paused=false,hidden=false))
         runCatching { PetService.command(this@MainActivity) }.onFailure { message=it.message ?: "Tidak dapat memulai" }
        }
       },Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp),enabled=!importing) {
        Text(if(status.running) "Hentikan karakter" else if(!overlayState.value) "Izinkan tampil di layar" else "Tampilkan karakter",fontWeight=FontWeight.SemiBold)
       }
       AnimatedVisibility(status.running) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
         OutlinedButton(onClick={ PetService.command(this@MainActivity,PetService.PAUSE) },Modifier.weight(1f),shape=RoundedCornerShape(14.dp)) { Text(if(c.paused) "Lanjutkan" else "Jeda") }
         OutlinedButton(onClick={ PetService.command(this@MainActivity,PetService.HIDE) },Modifier.weight(1f),shape=RoundedCornerShape(14.dp)) { Text(if(c.hidden) "Tampilkan" else "Sembunyikan") }
        }
       }
      }
     }
     if(status.error.isNotEmpty()) Notice(status.error,true)
     if(message.isNotEmpty()) Notice(message)
     Section("Karakter") {
      Row(verticalAlignment=Alignment.CenterVertically) {
       Column(Modifier.weight(1f)) { Text(info?.name ?: "Memuat model…",fontWeight=FontWeight.SemiBold); Text("${info?.motions?.size ?: 0} gerakan · ${info?.expressions?.size ?: 0} ekspresi",fontSize=12.sp,color=Muted) }
       TextButton(onClick={ modelMenu=true }) { Text("Ganti") }
       DropdownMenu(expanded=modelMenu,onDismissRequest={modelMenu=false}) {
        available.forEach { f -> DropdownMenuItem(text={ Text(f.name.removeSuffix(".model3.json")) },onClick={ modelMenu=false; save(prefs.read().copy(model=if(f.parentFile?.name=="whalegirl") "" else f.path,tapMotion="",waveMotion="",expression="")) }) }
       }
      }
      OutlinedButton(onClick={consent=true},Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),enabled=!importing) { Text("Impor ZIP Live2D") }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
       TextButton(onClick={open("https://booth.pm/en/items/5876181")}) { Text("Model Furina",fontSize=12.sp) }
       TextButton(onClick={save(prefs.read().copy(model="",tapMotion="",waveMotion="",expression=""))}) { Text("Pakai model uji",fontSize=12.sp) }
      }
      Text("Mendukung .model3.json, .moc3, tekstur, motion dan ekspresi dalam satu ZIP hingga 500 MB. Model disimpan lokal.",fontSize=12.sp,color=Muted,lineHeight=18.sp)
     }
     Section("Tampilan & gerak") {
      SettingSlider("Ukuran",c.size,"${c.size.toInt()} dp",100f..280f) { save(prefs.read().copy(size=it)) }
      SettingSlider("Kecepatan",c.speed,"${String.format(java.util.Locale.US,"%.1f",c.speed)}×",0.4f..2f) { save(prefs.read().copy(speed=it)) }
      SettingSlider("Keaktifan",c.activity,when { c.activity<0.33f -> "Tenang"; c.activity<0.7f -> "Seimbang"; else -> "Ceria" },0f..1f) { save(prefs.read().copy(activity=it)) }
      Toggle("Berkeliaran otomatis","Melayang halus mengikuti kemampuan rig",c.roam) { save(prefs.read().copy(roam=it)) }
      Toggle("Lompatan kecil","Sesekali melompat dengan gerak lembut",c.jump) { save(prefs.read().copy(jump=it)) }
      Toggle("Reaksi & ekspresi","Bereaksi saat disentuh dan sesekali menyapa",c.reactions) { save(prefs.read().copy(reactions=it)) }
      Toggle("Animasi 60 fps","30 fps lebih hemat · mode hemat baterai 15 fps",c.fps==60) { save(prefs.read().copy(fps=if(it) 60 else 30)) }
      TextButton(onClick={ save(prefs.read().copy(x=0.5f,y=0.8f)); if(status.running) PetService.command(this@MainActivity,PetService.HOME) },Modifier.align(Alignment.End)) { Text("Atur ulang posisi") }
     }
     Section("Aksi model") {
      Choice("Saat disentuh",c.tapMotion,info?.motions?.map { it.first } ?: emptyList()) { save(prefs.read().copy(tapMotion=it)) }
      Choice("Gerakan menyapa",c.waveMotion,info?.motions?.map { it.first } ?: emptyList()) { save(prefs.read().copy(waveMotion=it)) }
      Choice("Ekspresi favorit",c.expression,info?.expressions?.map { it.first } ?: emptyList()) { save(prefs.read().copy(expression=it)) }
      Text("Lambaian memakai motion model yang tersedia. Jika rig tidak mendukung, karakter menoleh atau bereaksi tanpa memaksakan gerakan tangan.",fontSize=12.sp,lineHeight=18.sp,color=Muted)
     }
     Section("Android & pembaruan") {
      Toggle("Izin tampil di atas aplikasi","Diperlukan agar karakter muncul di aplikasi lain",overlayState.value) {
       startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
      }
      TextButton(onClick={startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}) { Text("Pengaturan aplikasi & baterai") }
      Text("Pada HyperOS, aktifkan mulai otomatis dan pilih baterai Tanpa batas bila karakter sering berhenti. Setelah reboot atau paksa berhenti, buka aplikasi dan tampilkan lagi.",fontSize=12.sp,color=Muted,lineHeight=19.sp)
      HorizontalDivider(color=Pale)
      TextButton(onClick={if(update!=null) open(update!!.page) else checkUpdates()},enabled=!checking) { Text(if(checking) "Memeriksa…" else if(update!=null) "Unduh versi ${update!!.version}" else "Periksa pembaruan") }
      if(updateText.isNotEmpty()) Text(updateText,fontSize=12.sp,color=Muted)
     }
     Section("Tentang") {
      Text("Offline · Tanpa akun · Tanpa percakapan AI",fontSize=13.sp,fontWeight=FontWeight.Medium)
      Text("Model uji WhaleGirl © 7l-ui, MIT. This application contains Live2D Cubism SDK developed by Live2D Inc. Live2D dan karakter Furina dimiliki pemegang hak masing-masing.",fontSize=12.sp,color=Muted,lineHeight=18.sp)
      Text("Build pengujian pribadi. Impor lokal tidak memberikan izin penggunaan model. Publikasi umum menunggu peninjauan lisensi Expandable Application Live2D.",fontSize=12.sp,color=Muted,lineHeight=18.sp)
      if(status.running) Text("${status.renderer}\n${status.parameters} parameter · ${status.frames} frame · ${if(status.suspended) "Dijeda" else status.mode}",fontSize=11.sp,color=Muted,lineHeight=17.sp)
      TextButton(onClick={open("https://github.com/WynnDev-rill/Wynn-Store/tree/main/furina-pocket-pet")}) { Text("Kode sumber & lisensi") }
     }
     Text("made by wynn",Modifier.align(Alignment.CenterHorizontally),fontSize=11.sp,color=Muted)
    }
    if(importing) Surface(Modifier.fillMaxSize(),color=Color.White.copy(alpha=0.94f)) {
     Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
      CircularProgressIndicator(Modifier.size(32.dp),strokeWidth=3.dp); Spacer(Modifier.height(18.dp)); Text("Mengimpor model…"); Text("File besar dapat memerlukan waktu.",color=Muted,fontSize=12.sp)
     }
    }
   }
  }
  if(consent) AlertDialog(onDismissRequest={consent=false},title={Text("Impor model lokal")},text={Text("Gunakan model yang kamu berhak pakai di aplikasi ini. Model BOOTH Furina melarang distribusi ulang; ketentuan aplikasinya perlu izin pembuat. File tidak diunggah atau dibagikan.")},confirmButton={TextButton(onClick={consent=false;picker.launch(arrayOf("application/zip","application/x-zip-compressed","application/octet-stream"))}) { Text("Pilih ZIP milikku") }},dismissButton={TextButton(onClick={consent=false}) { Text("Batal") }})
 }
}

@Composable private fun Section(title: String,content: @Composable ColumnScope.()->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text(title,Modifier.padding(start=3.dp),fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Ink)
  Surface(shape=RoundedCornerShape(22.dp),color=Color.White) { Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content) }
 }
}
@Composable private fun Notice(text: String,error: Boolean=false) {
 Surface(color=if(error) Color(0xFFFFEEEE) else Pale,shape=RoundedCornerShape(16.dp)) { Text(text,Modifier.padding(16.dp),fontSize=12.sp,color=if(error) Color(0xFF9E3B3B) else Ink,lineHeight=19.sp) }
}
@Composable private fun SettingSlider(label: String,value: Float,detail: String,range: ClosedFloatingPointRange<Float>,save: (Float)->Unit) {
 var local by remember(value) { mutableFloatStateOf(value) }
 Column {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,fontSize=14.sp); Text(detail,fontSize=12.sp,color=Blue) }
  Slider(value=local,onValueChange={local=it},valueRange=range,onValueChangeFinished={save(local)},modifier=Modifier.height(36.dp))
 }
}
@Composable private fun Toggle(title: String,detail: String,value: Boolean,onChange: (Boolean)->Unit) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  Column(Modifier.weight(1f).padding(end=8.dp)) { Text(title,fontSize=14.sp); Text(detail,fontSize=11.sp,color=Muted,lineHeight=17.sp) }
  Switch(checked=value,onCheckedChange=onChange)
 }
}
@Composable private fun Choice(label: String,selected: String,options: List<String>,save: (String)->Unit) {
 var open by remember { mutableStateOf(false) }
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  Text(label,Modifier.weight(1f),fontSize=13.sp)
  Box {
   TextButton(onClick={open=true},enabled=options.isNotEmpty()) { Text(if(selected.isEmpty()) "Otomatis" else selected.take(22),fontSize=12.sp) }
   DropdownMenu(expanded=open,onDismissRequest={open=false}) {
    DropdownMenuItem(text={Text("Otomatis")},onClick={open=false;save("")})
    options.forEach { option -> DropdownMenuItem(text={Text(option)},onClick={open=false;save(option)}) }
   }
  }
 }
}
