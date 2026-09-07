package id.wynn.roadtoimmortal.domain

import id.wynn.roadtoimmortal.data.*
import kotlin.math.ln
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Ordered IDs survive catalog changes; missing heroes are never silently deleted. */
object HeroPool {
    const val LIMIT = 10

    fun decode(raw: String?): Map<String, List<Int>> =
        runCatching {
                Json.decodeFromString<Map<String, List<Int>>>(raw ?: "{}")
                    .filterKeys { it in lanes }
                    .mapValues { (_, ids) -> ids.filter { it > 0 }.distinct().take(LIMIT) }
            }
            .getOrDefault(emptyMap())

    fun encode(pool: Map<String, List<Int>>) = Json.encodeToString(pool)

    fun add(pool: Map<String, List<Int>>, lane: String, id: Int): Map<String, List<Int>> {
        val old = pool[lane].orEmpty()
        if (lane !in lanes || id <= 0 || id in old || old.size >= LIMIT) return pool
        return pool + (lane to old + id)
    }

    fun remove(pool: Map<String, List<Int>>, lane: String, id: Int) =
        pool + (lane to pool[lane].orEmpty().filterNot { it == id })

    fun reorder(
        pool: Map<String, List<Int>>,
        lane: String,
        id: Int,
        offset: Int,
    ): Map<String, List<Int>> {
        val old = pool[lane].orEmpty().toMutableList()
        val from = old.indexOf(id)
        val to = from + offset
        if (from < 0 || to !in old.indices) return pool
        old.removeAt(from)
        old.add(to, id)
        return pool + (lane to old)
    }

    fun move(
        pool: Map<String, List<Int>>,
        from: String,
        to: String,
        id: Int,
    ): Map<String, List<Int>> {
        if (from == to || to !in lanes || id !in pool[from].orEmpty()) return pool
        if (id !in pool[to].orEmpty() && pool[to].orEmpty().size >= LIMIT) return pool
        return remove(add(pool, to, id), from, id)
    }
}

data class TierEntry(val hero: Hero, val meta: HeroMeta, val tier: String, val score: Double)

object TierEngine {
    val tiers = listOf("SS", "S", "A", "B", "C")

    // Conservative shrinkage reduces dominance of very rare picks; these are model weights.
    fun score(m: HeroMeta): Double {
        val pick = (m.pick ?: 0.0).coerceAtLeast(0.0)
        return 50 +
            ((m.win ?: 50.0) - 50) * pick / (pick + .5) +
            ln(1 + pick) * 1.4 +
            (m.ban ?: 0.0) * .025
    }

    fun entries(catalog: Catalog, rank: String, lane: String): List<TierEntry> {
        val rows =
            catalog
                .slice(rank)
                ?.heroes
                .orEmpty()
                .filter { it.win?.isFinite() == true && it.pick?.isFinite() == true }
                .mapNotNull { m ->
                    catalog.heroById[m.heroId]?.takeIf { lane in it.lanes }?.let { it to m }
                }
                .sortedWith(
                    compareByDescending<Pair<Hero, HeroMeta>> { score(it.second) }
                        .thenBy { it.first.id }
                )
        return rows.mapIndexed { i, (hero, meta) ->
            val percentile = i.toDouble() / rows.size.coerceAtLeast(1)
            TierEntry(
                hero,
                meta,
                when {
                    percentile < .10 -> "SS"
                    percentile < .30 -> "S"
                    percentile < .60 -> "A"
                    percentile < .85 -> "B"
                    else -> "C"
                },
                score(meta),
            )
        }
    }
}

data class Party(
    val heroes: List<Hero>,
    val assignment: Map<String, Int>,
    val score: Int,
    val reasons: List<String>,
    val pairEvidence: Int,
)

object PartyEngine {
    fun recommend(
        catalog: Catalog,
        rank: String,
        size: Int,
        seed: Int? = null,
        enemy: Int? = null,
    ): List<Party> {
        if (size !in 2..5 || seed == enemy && seed != null) return emptyList()
        val meta = catalog.slice(rank)?.heroes.orEmpty().associateBy { it.heroId }
        if (meta.isEmpty()) return emptyList()
        val candidates =
            catalog.heroes.filter {
                it.id != enemy && it.lanes.any(lanes::contains) && meta[it.id]?.win != null
            }
        val fixed =
            seed?.let { catalog.heroById[it] } ?: if (seed != null) return emptyList() else null
        fun evaluate(heroes: List<Hero>): Party {
            var sum = heroes.map { meta[it.id]?.let(TierEngine::score) ?: 50.0 }.average()
            var evidence = 0
            var synergy = 0.0
            for (i in heroes.indices) for (j in i + 1 until heroes.size) {
                val a = heroes[i]
                val b = heroes[j]
                val pair =
                    meta[a.id]?.synergy?.firstOrNull { it.heroId == b.id && it.delta != null }
                        ?: meta[b.id]?.synergy?.firstOrNull {
                            it.heroId == a.id && it.delta != null
                        }
                if (pair != null) {
                    synergy += pair.delta!!.coerceIn(-8.0, 8.0)
                    evidence++
                }
            }
            if (evidence > 0) sum += synergy / evidence * .5
            var counter = 0.0
            if (enemy != null)
                heroes.forEach { h ->
                    val own = meta[h.id]
                    val pair =
                        (own?.strongAgainst.orEmpty() + own?.counters.orEmpty()).firstOrNull {
                            it.heroId == enemy && it.delta != null
                        }
                    // Source edges report the opponent's advantage against the selected hero.
                    if (pair != null) counter -= pair.delta!!.coerceIn(-8.0, 8.0) / heroes.size
                }
            sum += counter * .5
            val frontline = heroes.any { "Tank" in it.roles || "Fighter" in it.roles }
            val magic = heroes.any { "Mage" in it.roles }
            val sustained = heroes.any { "Marksman" in it.roles || "Fighter" in it.roles }
            if (size == 5)
                sum +=
                    (if (frontline) 1 else -2) + (if (magic) 1 else -2) + (if (sustained) 1 else -2)
            val reasons = buildList {
                add(if (heroes.size == 5) "5 lane terisi" else "Lane saling melengkapi")
                if (synergy > 0) add("Sinergi positif")
                if (counter > 0) add("Matchup mendukung")
                if (size == 5 && frontline) add("Ada garis depan")
            }
            return Party(
                heroes,
                DraftEngine.laneAssignment(heroes),
                ((sum - 35) * 3).toInt().coerceIn(0, 100),
                reasons,
                evidence,
            )
        }
        // A bounded beam searches combinations without blocking composition or exploring n^5.
        var beam = listOf(fixed?.let(::listOf).orEmpty())
        repeat(size - (if (fixed == null) 0 else 1)) {
            val seen = hashSetOf<String>()
            beam =
                beam
                    .flatMap { group ->
                        candidates
                            .filter { h -> group.none { it.id == h.id } }
                            .mapNotNull { h ->
                                val next = group + h
                                val key = next.map { it.id }.sorted().joinToString(",")
                                if (
                                    seen.add(key) &&
                                        DraftEngine.laneAssignment(next).size == next.size
                                )
                                    next
                                else null
                            }
                    }
                    .sortedByDescending { evaluate(it).score }
                    .take(60)
        }
        val selected = mutableListOf<Party>()
        beam
            .map(::evaluate)
            .sortedByDescending { it.score }
            .forEach { p ->
                if (
                    selected.size < 6 &&
                        selected.none { prior ->
                            prior.heroes.count { a -> p.heroes.any { it.id == a.id } } >=
                                size - (if (size == 5) 1 else 0)
                        }
                )
                    selected += p
            }
        return selected
    }
}
