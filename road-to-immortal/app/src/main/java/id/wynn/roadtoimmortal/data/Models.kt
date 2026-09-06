package id.wynn.roadtoimmortal.data

import kotlinx.serialization.Serializable

@Serializable
data class Provenance(
    val name: String = "MLBB",
    val url: String = "https://www.mobilelegends.com/rank",
    val updatedAt: String? = null,
    val checkedAt: String = "",
    val status: String = "official",
    val note: String = "",
)

@Serializable
data class Skill(
    val id: String,
    val name: String,
    val icon: String = "",
    val description: String = "",
    val cooldown: String = "",
    val tags: List<String> = emptyList(),
    val form: Int = 1,
)

@Serializable data class Relation(val ids: List<Int> = emptyList(), val description: String = "")

@Serializable
data class Hero(
    val id: Int,
    val name: String,
    val icon: String,
    val portrait: String = "",
    val roles: List<String> = emptyList(),
    val lanes: List<String> = emptyList(),
    val speciality: List<String> = emptyList(),
    val story: String = "",
    val difficulty: Int = 0,
    val skills: List<Skill> = emptyList(),
    val counters: Relation = Relation(),
    val strongAgainst: Relation = Relation(),
    val synergy: Relation = Relation(),
    val updatedAt: String? = null,
)

@Serializable data class Effect(val name: String = "", val description: String)

@Serializable
data class Equipment(
    val id: String,
    val name: String,
    val icon: String,
    val category: String,
    val price: Int? = null,
    val description: String = "",
    val stats: List<String> = emptyList(),
    val effects: List<Effect> = emptyList(),
    val recipe: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val source: Provenance = Provenance(),
)

@Serializable data class RankBadge(val key: String, val name: String, val icon: String)

@Serializable
data class Build(
    val heroId: Int,
    val title: String = "Item inti",
    val items: List<String>,
    val spell: String? = null,
    val emblems: List<String> = emptyList(),
    val winRate: Double? = null,
    val pickRate: Double? = null,
    val rank: String = "mythic",
    val lane: String = "",
    val patch: String? = null,
    val source: Provenance = Provenance(),
)

@Serializable
data class Matchup(
    val heroId: Int,
    val winRate: Double? = null,
    val delta: Double? = null,
    val appearance: Double? = null,
)

@Serializable
data class HeroMeta(
    val heroId: Int,
    val win: Double? = null,
    val pick: Double? = null,
    val ban: Double? = null,
    val counters: List<Matchup> = emptyList(),
    val strongAgainst: List<Matchup> = emptyList(),
    val synergy: List<Matchup> = emptyList(),
)

@Serializable
data class MetaSlice(
    val rank: String,
    val days: Int = 7,
    val source: Provenance,
    val heroes: List<HeroMeta> = emptyList(),
)

@Serializable
data class Season(
    val number: Int? = null,
    val startsAt: String? = null,
    val resetsAt: String? = null,
    val source: Provenance = Provenance(),
    val confidence: String = "unavailable",
)

@Serializable
data class Patch(
    val version: String = "",
    val title: String = "",
    val url: String = "",
    val updatedAt: String? = null,
)

@Serializable
data class TierRule(
    val key: String,
    val name: String,
    val divisions: Int,
    val starsPerDivision: Int,
)

@Serializable
data class RankRules(
    val tiers: List<TierRule> =
        listOf(
            TierRule("warrior", "Warrior", 3, 3),
            TierRule("elite", "Elite", 3, 4),
            TierRule("master", "Master", 4, 4),
            TierRule("grandmaster", "Grandmaster", 5, 5),
            TierRule("epic", "Epic", 5, 5),
            TierRule("legend", "Legend", 5, 5),
        ),
    val immortalStars: Int = 100,
    val mythicPromotion: Int = 1,
)

@Serializable
data class Catalog(
    val schemaVersion: Int = 1,
    val generatedAt: String,
    val source: Provenance,
    val heroes: List<Hero>,
    val items: List<Equipment> = emptyList(),
    val emblems: List<Equipment> = emptyList(),
    val spells: List<Equipment> = emptyList(),
    val ranks: List<RankBadge> = emptyList(),
    val builds: List<Build> = emptyList(),
    val meta: List<MetaSlice> = emptyList(),
    val season: Season = Season(),
    val patch: Patch = Patch(),
    val rankRules: RankRules = RankRules(),
) {
    val heroById by lazy { heroes.associateBy { it.id } }
    val equipmentById by lazy { (items + emblems + spells).associateBy { it.id } }

    fun slice(rank: String, days: Int = 7) = meta.firstOrNull { it.rank == rank && it.days == days }
}

@Serializable
data class RemoteConfig(
    val schemaVersion: Int = 1,
    val refreshHours: Int = 6,
    val catalogUrls: List<String> =
        listOf(
            "https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/road-to-immortal/data/catalog.json",
            "https://cdn.jsdelivr.net/gh/WynnDev-rill/Wynn-Store@main/road-to-immortal/data/catalog.json",
        ),
    val disabled: Boolean = false,
)
