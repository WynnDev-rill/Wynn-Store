package id.wynn.roadtoimmortal
import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*
import org.junit.Assert.*
import org.junit.Test
class DraftTest {
 private fun hero(id:Int,lanes:List<String>)=Hero(id,"Hero $id","https://example.com/$id",lanes=lanes)
 @Test fun flexibleLaneBacktracks(){val m=DraftEngine.laneAssignment(listOf(hero(1,listOf("Roam","Mid")),hero(2,listOf("Roam"))));assertEquals(2,m.size);assertEquals(1,m["Mid"]);assertEquals(2,m["Roam"])}
 @Test fun excludesAllSelectedAndBanned(){val c=Catalog(generatedAt="2026-09-05T00:00:00Z",source=Provenance(),heroes=(1..5).map {hero(it,listOf("Mid"))});assertEquals(listOf(4,5),DraftEngine.recommend(c,listOf(1),listOf(2),setOf(3),"mythic").map {it.hero.id})}
 @Test fun opponentDeltaFavorsCounter(){val c=Catalog(generatedAt="2026-09-05T00:00:00Z",source=Provenance(),heroes=(1..3).map {hero(it,listOf("Mid"))},meta=listOf(MetaSlice("mythic",source=Provenance(),heroes=listOf(HeroMeta(1,counters=listOf(Matchup(2,57.0,5.0)),strongAgainst=listOf(Matchup(3,43.0,-5.0)))))));val r=DraftEngine.recommend(c,emptyList(),listOf(1),emptySet(),"mythic");assertEquals(2,r.first().hero.id);assertTrue(r.first().score>r.last().score)}
 @Test fun punctuationAndAccentInsensitive(){assertEquals("change",searchKey("Chang’e"));assertEquals("freya",searchKey("Fréya"))}
}
