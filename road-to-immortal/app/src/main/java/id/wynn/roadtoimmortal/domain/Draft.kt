package id.wynn.roadtoimmortal.domain
import id.wynn.roadtoimmortal.data.*
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ln
fun searchKey(value:String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}"),"").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"),"")
val lanes=listOf("EXP","Jungle","Mid","Gold","Roam")
data class DraftSuggestion(val hero:Hero,val score:Double,val reasons:List<String>,val evidence:Int)
object DraftEngine {
 /** Editorial ranking only; never presented as team win probability. */
 fun metaScore(m:HeroMeta)=(m.win?:50.0)+ln(1.0+(m.pick?:0.0))*1.4+(m.ban?:0.0)*.025
 fun laneAssignment(heroes:List<Hero>):Map<String,Int> {
  var best=emptyMap<String,Int>()
  fun assign(index:Int,current:Map<String,Int>) {
   if(index==heroes.size){if(current.size>best.size) best=current;return}
   val hero=heroes[index]
   hero.lanes.filter {it in lanes&&it !in current}.forEach {assign(index+1,current+(it to hero.id))}
   assign(index+1,current)
  }
  assign(0,emptyMap());return best
 }
 fun recommend(catalog:Catalog,allies:List<Int>,enemies:List<Int>,bans:Set<Int>,rank:String,lane:String="Semua"):List<DraftSuggestion> {
  val selected=(allies+enemies).toSet()+bans;val meta=catalog.slice(rank)?.heroes?.associateBy {it.heroId}.orEmpty()
  val team=allies.mapNotNull(catalog.heroById::get);val coverage=laneAssignment(team).size
  return catalog.heroes.asSequence().filter {it.id !in selected&&(lane=="Semua"||lane in it.lanes)}.map {h->
   val own=meta[h.id];var score=own?.let(::metaScore)?:50.0;var evidence=0;val reasons=mutableListOf<String>()
   if(laneAssignment(team+h).size>coverage){score+=4;reasons+="Mengisi lane yang belum terisi"}
   for(enemy in enemies){
    val outgoing=(meta[enemy]?.counters.orEmpty()+meta[enemy]?.strongAgainst.orEmpty()).firstOrNull {it.heroId==h.id}
    val incoming=(own?.counters.orEmpty()+own?.strongAgainst.orEmpty()).firstOrNull {it.heroId==enemy}
    val advantage=outgoing?.delta?:incoming?.delta?.let {-it}
    if(advantage!=null){score+=advantage.coerceIn(-10.0,10.0)*.75;evidence++;if(advantage>.5) reasons+="Unggul secara statistik vs ${catalog.heroById[enemy]?.name}";if(advantage< -1) reasons+="Waspadai ${catalog.heroById[enemy]?.name}"}
    else if(h.id in catalog.heroById[enemy]?.counters?.ids.orEmpty()){score++;reasons+="Counter panduan vs ${catalog.heroById[enemy]?.name}"}
   }
   for(ally in allies){val pair=meta[ally]?.synergy?.firstOrNull {it.heroId==h.id}?:own?.synergy?.firstOrNull {it.heroId==ally};pair?.delta?.let {score+=it.coerceIn(-8.0,8.0)*.5;evidence++;if(it>.5) reasons+="Sinergi dengan ${catalog.heroById[ally]?.name}"}}
   if(reasons.isEmpty()) reasons+=if(own!=null) "Berdasarkan meta rank pilihan" else "Cocokkan lane dan kenyamanan bermain"
   DraftSuggestion(h,score,reasons.distinct().take(3),evidence)
  }.sortedByDescending {it.score}.toList()
 }
 fun itemCounters(items:List<Equipment>,threat:String)=items.filter {threat in it.tags}.sortedByDescending {it.price?:0}
}
