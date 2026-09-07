package id.wynn.roadtoimmortal

import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*
import org.junit.Assert.*
import org.junit.Test

class PlanningTest {
    @Test
    fun oldInstallAndCorruptPoolAreSafe() {
        assertEquals(emptyMap<String, List<Int>>(), HeroPool.decode(null))
        assertEquals(emptyMap<String, List<Int>>(), HeroPool.decode("broken"))
        val p = HeroPool.decode("{\"EXP\":[1,1,-2,3],\"unknown\":[4]}")
        assertEquals(listOf(1, 3), p["EXP"])
        assertFalse(p.containsKey("unknown"))
        assertEquals(p, HeroPool.decode(HeroPool.encode(p)))
    }

    @Test
    fun capacityAndFailedMoveNeverLoseSourceHero() {
        val p = mapOf("EXP" to (1..10).toList(), "Mid" to listOf(20))
        assertEquals(p, HeroPool.add(p, "EXP", 11))
        assertEquals(p, HeroPool.move(p, "Mid", "EXP", 20))
        assertEquals(p, HeroPool.add(p, "EXP", 1))
        val moved = HeroPool.move(p, "EXP", "Gold", 3)
        assertEquals(listOf(3), moved["Gold"])
        assertFalse(3 in moved["EXP"].orEmpty())
    }

    @Test
    fun orderAndIndependentLanesSurviveRoundTrip() {
        val p = mapOf("EXP" to listOf(1, 2, 3), "Mid" to listOf(1))
        val changed = HeroPool.reorder(p, "EXP", 3, -1)
        assertEquals(listOf(1, 3, 2), changed["EXP"])
        assertEquals(listOf(1), changed["Mid"])
        assertEquals(changed, HeroPool.decode(HeroPool.encode(changed)))
        assertEquals(p, HeroPool.reorder(p, "EXP", 1, -1))
        assertEquals(listOf(2, 3), HeroPool.remove(p, "EXP", 1)["EXP"])
    }

    private fun catalog(): Catalog {
        val heroes =
            (1..20).map { i ->
                Hero(
                    i,
                    "Hero $i",
                    "https://example.com/$i",
                    lanes = listOf(lanes[(i - 1) % 5]),
                    roles =
                        listOf(if (i % 5 == 0) "Tank" else if (i % 5 == 2) "Mage" else "Fighter"),
                )
            }
        return Catalog(
            generatedAt = "2026-09-07T00:00:00Z",
            source = Provenance(),
            heroes = heroes,
            meta =
                listOf(
                    MetaSlice(
                        "mythic",
                        source = Provenance(),
                        heroes =
                            heroes.map {
                                HeroMeta(
                                    it.id,
                                    win = 50.0 + it.id / 3,
                                    pick = 2.0,
                                    ban = 1.0,
                                    synergy = listOf(Matchup(if (it.id == 1) 2 else 1, delta = 3.0)),
                                )
                            },
                    )
                ),
        )
    }

    @Test
    fun tiersRespectExactRankLaneAndFreshCatalog() {
        val c = catalog()
        assertTrue(TierEngine.entries(c, "legend", "EXP").isEmpty())
        val entries = TierEngine.entries(c, "mythic", "EXP")
        assertEquals(4, entries.size)
        assertTrue(entries.all { "EXP" in it.hero.lanes })
        assertEquals(16, entries.first().hero.id)
        assertEquals("SS", entries.first().tier)
        assertTrue(
            TierEngine.score(HeroMeta(1, win = 90.0, pick = .001)) <
                TierEngine.score(HeroMeta(2, win = 54.0, pick = 5.0))
        )
    }

    @Test
    fun partyKeepsSeedExcludesEnemyAndAssignsUniqueLanes() {
        for (size in listOf(2, 3, 5)) {
            val result = PartyEngine.recommend(catalog(), "mythic", size, seed = 1, enemy = 2)
            assertTrue(result.isNotEmpty())
            result.forEach { p ->
                assertEquals(size, p.heroes.size)
                assertEquals(size, p.heroes.map { it.id }.distinct().size)
                assertEquals(size, p.assignment.size)
                assertTrue(p.heroes.any { it.id == 1 })
                assertFalse(p.heroes.any { it.id == 2 })
                assertTrue(p.score in 0..100)
            }
        }
        assertTrue(PartyEngine.recommend(catalog(), "missing", 2).isEmpty())
        assertTrue(PartyEngine.recommend(catalog(), "mythic", 2, 1, 1).isEmpty())
    }

    @Test
    fun opponentAdvantageLowersRecommendationInsteadOfRewardingIt() {
        val all = catalog()
        val base = all.copy(heroes = all.heroes.take(2))
        val risky =
            base.copy(
                meta =
                    base.meta.map { slice ->
                        slice.copy(
                            heroes =
                                slice.heroes.map { m ->
                                    if (m.heroId == 1)
                                        m.copy(counters = listOf(Matchup(20, delta = 8.0)))
                                    else m
                                }
                        )
                    }
            )
        val neutral = PartyEngine.recommend(base, "mythic", 2, seed = 1, enemy = 20).first()
        val countered = PartyEngine.recommend(risky, "mythic", 2, seed = 1, enemy = 20).first()
        assertTrue(countered.score < neutral.score)
        assertFalse("Matchup mendukung" in countered.reasons)
    }
}
