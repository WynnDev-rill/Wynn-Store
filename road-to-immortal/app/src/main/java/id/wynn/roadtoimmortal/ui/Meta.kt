package id.wynn.roadtoimmortal.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*
@Composable fun MetaScreen(catalog:Catalog,onHero:(Int)->Unit,onSources:()->Unit){
 var rank by rememberSaveable {mutableStateOf("mythic")};var lane by rememberSaveable {mutableStateOf("Semua")};var sort by rememberSaveable {mutableStateOf("Win rate")};var query by rememberSaveable {mutableStateOf("")}
 val slice=catalog.slice(rank)
 val entries=remember(catalog,rank,lane,sort,query){slice?.heroes.orEmpty().filter {m->catalog.heroById[m.heroId]?.let {h->(lane=="Semua"||lane in h.lanes||lane in h.roles)&&searchKey(query) in searchKey(h.name+h.roles.joinToString())}==true}.sortedByDescending {when(sort){"Pick rate"->it.pick;"Ban rate"->it.ban;else->it.win}?:-1.0}}
 LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item {Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Baca medan.",style=MaterialTheme.typography.headlineMedium);Caption("Statistik ${slice?.days?:7} hari")};ScopePicker(rank,{rank=it})}}
  item {SearchBox(query,{query=it},"Cari hero di meta")}
  item {ChoiceRow(listOf("Semua")+lanes+catalog.heroes.flatMap {it.roles}.distinct().sorted(),lane,{lane=it})}
  item {ChoiceRow(listOf("Win rate","Pick rate","Ban rate"),sort,{sort=it})}
  item {slice?.let {SourceLine(it.source,onSources)}}
  if(slice==null||entries.isEmpty()) item {EmptyState(if(slice==null) "Statistik belum tersedia" else "Hero tidak ditemukan",if(slice==null) "Pilih rank lain atau periksa pembaruan data." else "Coba nama, role, atau lane lain.")}
  else itemsIndexed(entries,key={_,m->m.heroId}){index,m->
   catalog.heroById[m.heroId]?.let {hero->
    Surface(onClick={onHero(hero.id)},shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surfaceContainerLow){
     Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Artwork(hero.icon,null,Modifier.size(52.dp));Column(Modifier.weight(1f)){Text(hero.name,style=MaterialTheme.typography.titleMedium);Caption(hero.lanes.joinToString(" · "))};Text(String.format("%02d",index+1),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)}
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
       listOf("Win" to m.win,"Pick" to m.pick,"Ban" to m.ban).forEach {(label,value)->Column(Modifier.weight(1f)){Caption(label);Text(rate(value),style=MaterialTheme.typography.titleMedium,color=if(sort.startsWith(label)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)}}
      }
     }
    }
   }
  }
  if(slice!=null) item {Caption("Win rate adalah hasil sampel, bukan jaminan kemenangan. Jumlah pertandingan tidak dipublikasikan. Filter role/lane memilih hero; statistik tetap agregat hero pada rank pilihan.")}
 }
}
