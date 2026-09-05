package id.wynn.roadtoimmortal.ui
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.wynn.roadtoimmortal.MainViewModel
import id.wynn.roadtoimmortal.data.Equipment
import kotlinx.coroutines.delay
import java.time.Instant
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ImmortalRoot(vm:MainViewModel){
 val data by vm.data.collectAsStateWithLifecycle();val prefs by vm.preferences.collectAsStateWithLifecycle()
 var tab by rememberSaveable {mutableIntStateOf(0)};var heroStack by rememberSaveable {mutableStateOf(listOf<Int>())}
 var gearId by rememberSaveable {mutableStateOf<String?>(null)};var rankSheet by rememberSaveable {mutableStateOf(false)}
 var settings by rememberSaveable {mutableStateOf(false)};var sources by rememberSaveable {mutableStateOf(false)}
 var now by remember {mutableStateOf(Instant.now())};val holder=rememberSaveableStateHolder()
 LaunchedEffect(Unit){while(true){now=Instant.now();delay(30_000)}}
 val catalog=data.catalog;val openHero:(Int)->Unit={heroStack=heroStack+it};val openGear:(Equipment)->Unit={gearId=it.id}
 BackHandler(heroStack.isNotEmpty()||settings||sources||tab!=0){when {sources->sources=false;settings->settings=false;heroStack.isNotEmpty()->heroStack=heroStack.dropLast(1);else->tab=0}}
 ImmortalTheme(prefs.theme){
  Scaffold(bottomBar={if(heroStack.isEmpty()&&!settings&&!sources) NavigationBar(containerColor=MaterialTheme.colorScheme.surface){listOf("Perjalanan" to Icons.Outlined.AutoAwesome,"Jelajah" to Icons.Outlined.GridView,"Meta" to Icons.Outlined.Leaderboard,"Draft" to Icons.Outlined.Groups).forEachIndexed {index,pair->NavigationBarItem(tab==index,{tab=index},icon={Icon(pair.second,null)},label={Text(pair.first)})}}}){padding->
   Column(Modifier.fillMaxSize().padding(padding)){
    if(data.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
    AnimatedVisibility(data.error!=null&&catalog!=null){Surface(color=MaterialTheme.colorScheme.secondaryContainer){Row(Modifier.fillMaxWidth().padding(start=16.dp),verticalAlignment=Alignment.CenterVertically){Text("Mode tersimpan · pembaruan tertunda",Modifier.weight(1f),style=MaterialTheme.typography.labelMedium);TextButton(vm::refresh){Text("Coba lagi")}}}}
    when {
     catalog==null&&data.loading->Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator();Spacer(Modifier.height(18.dp));Text("Menyiapkan perjalananmu…")}
     catalog==null->EmptyState("Data belum bisa dibuka",data.error?:"Hubungkan internet untuk mengambil katalog MLBB.",Icons.Outlined.CloudOff,"Coba lagi",vm::refresh)
     sources->SourcesScreen(catalog,{sources=false},vm::refresh,data.refreshing,data.error)
     settings->SettingsScreen(prefs,vm::theme,{settings=false},{sources=true})
     heroStack.isNotEmpty()->catalog.heroById[heroStack.last()]?.let {h->holder.SaveableStateProvider("hero-${h.id}"){HeroScreen(h,catalog,h.id in prefs.favorites,{vm.favorite(h.id)},{heroStack=heroStack.dropLast(1)},openHero,openGear,{sources=true})}}
     else->AnimatedContent(tab,label="Navigasi",transitionSpec={fadeIn() togetherWith fadeOut()}){page->holder.SaveableStateProvider("tab-$page"){when(page){
      0->HomeScreen(catalog,prefs,now,{rankSheet=true},openHero,{tab=2},{settings=true},{sources=true})
      1->AtlasScreen(catalog,prefs.favorites,openHero,openGear)
      2->MetaScreen(catalog,openHero,{sources=true})
      else->DraftScreen(catalog,openHero,openGear)
     }}}
    }
   }
  }
  if(rankSheet&&catalog!=null) RankPicker(catalog,prefs.position,vm::rank,{rankSheet=false})
  gearId?.let {id->catalog?.equipmentById?.get(id)?.let {item->key(id){EquipmentSheet(item,catalog,{gearId=null},openGear)}}}
 }
}
