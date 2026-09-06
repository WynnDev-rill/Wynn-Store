package id.wynn.roadtoimmortal.domain

import id.wynn.roadtoimmortal.data.*
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlinx.serialization.Serializable

@Serializable
data class RankPosition(val tier: String = "mythic", val division: Int = 1, val stars: Int = 0)

data class RoadTarget(
    val remaining: Int,
    val days: Int?,
    val daily: Int?,
    val progress: Float,
    val expired: Boolean,
)

object Road {
    fun normalize(p: RankPosition, rules: RankRules = RankRules()): RankPosition {
        val rule = rules.tiers.firstOrNull { it.key == p.tier }
        return if (rule != null)
            p.copy(
                division = p.division.coerceIn(1, rule.divisions),
                stars = p.stars.coerceIn(0, rule.starsPerDivision),
            )
        else RankPosition(stars = p.stars.coerceIn(0, 9999))
    }

    fun badge(p: RankPosition) =
        if (p.tier != "mythic") p.tier
        else
            when {
                p.stars >= 100 -> "immortal"
                p.stars >= 50 -> "glory"
                p.stars >= 25 -> "honor"
                else -> "mythic"
            }

    fun roman(n: Int) = listOf("", "I", "II", "III", "IV", "V").getOrElse(n) { n.toString() }

    fun label(p: RankPosition, rules: RankRules = RankRules()) =
        when (badge(p)) {
            "immortal" -> "Mythical Immortal"
            "glory" -> "Mythical Glory"
            "honor" -> "Mythical Honor"
            "mythic" -> "Mythic"
            else -> "${rules.tiers.firstOrNull {it.key==p.tier}?.name} ${roman(p.division)}"
        }

    fun remaining(position: RankPosition, rules: RankRules = RankRules()): Int {
        val p = normalize(position, rules)
        if (p.tier == "mythic") return (rules.immortalStars - p.stars).coerceAtLeast(0)
        val index = rules.tiers.indexOfFirst { it.key == p.tier }
        val current = rules.tiers[index]
        return rules.immortalStars + rules.mythicPromotion + p.division * current.starsPerDivision -
            p.stars + rules.tiers.drop(index + 1).sumOf { it.divisions * it.starsPerDivision }
    }

    /** Calendar opportunities include today's partial day. The reset instant is exclusive. */
    fun target(
        p: RankPosition,
        season: Season,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        rules: RankRules = RankRules(),
    ): RoadTarget {
        val left = remaining(p, rules)
        val end = season.resetsAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val days =
            end?.takeIf { it.isAfter(now) }
                ?.let {
                    (ChronoUnit.DAYS.between(
                            now.atZone(zone).toLocalDate(),
                            it.minusNanos(1).atZone(zone).toLocalDate(),
                        ) + 1)
                        .toInt()
                        .coerceAtLeast(1)
                }
        return RoadTarget(
            left,
            days,
            if (left == 0) 0 else days?.let { ceil(left.toDouble() / it).toInt() },
            if (p.tier == "mythic") (p.stars.toFloat() / rules.immortalStars).coerceIn(0f, 1f)
            else 0f,
            end != null && !end.isAfter(now),
        )
    }

    fun countdown(season: Season, now: Instant): String {
        val end =
            season.resetsAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?: return "Jadwal belum tersedia"
        val d = Duration.between(now, end)
        return when {
            d.isNegative || d.isZero -> "Menunggu season baru"
            d.toDays() > 0 -> "${d.toDays()} hari ${d.toHours()%24} jam"
            else -> "${d.toHours()} jam ${d.toMinutes()%60} menit"
        }
    }
}
