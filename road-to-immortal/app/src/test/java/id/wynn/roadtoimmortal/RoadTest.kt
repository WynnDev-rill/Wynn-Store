package id.wynn.roadtoimmortal

import id.wynn.roadtoimmortal.data.*
import id.wynn.roadtoimmortal.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class RoadTest {
    private val now = Instant.parse("2026-09-05T12:00:00Z")
    private val season = Season(resetsAt = "2026-09-16T08:00:00Z")

    @Test
    fun mythicThresholds() {
        assertEquals("mythic", Road.badge(RankPosition(stars = 24)))
        assertEquals("honor", Road.badge(RankPosition(stars = 25)))
        assertEquals("glory", Road.badge(RankPosition(stars = 50)))
        assertEquals("immortal", Road.badge(RankPosition(stars = 100)))
    }

    @Test
    fun dailyTargetUsesLocalPlayableDates() {
        val r = Road.target(RankPosition(stars = 40), season, now, ZoneId.of("Asia/Jakarta"))
        assertEquals(60, r.remaining)
        assertEquals(12, r.days)
        assertEquals(5, r.daily)
    }

    @Test
    fun missingSeasonNeverFabricatesTarget() {
        assertNull(Road.target(RankPosition(stars = 40), Season(), now).daily)
    }

    @Test
    fun expiredSeasonIsNotProjected() {
        val r = Road.target(RankPosition(stars = 40), Season(resetsAt = now.toString()), now)
        assertNull(r.daily)
        assertTrue(r.expired)
    }

    @Test
    fun midnightExclusive() {
        val r =
            Road.target(
                RankPosition(stars = 90),
                Season(resetsAt = "2026-09-06T00:00:00Z"),
                now,
                ZoneId.of("UTC"),
            )
        assertEquals(1, r.days)
        assertEquals(10, r.daily)
    }

    @Test
    fun achievedDoesNotGoNegative() {
        val r = Road.target(RankPosition(stars = 999), season, now)
        assertEquals(0, r.daily)
        assertEquals(1f, r.progress)
    }

    @Test
    fun promotionAndDivisionCarry() {
        assertEquals(101, Road.remaining(RankPosition("legend", 1, 5)))
        assertEquals(
            Road.remaining(RankPosition("legend", 2, 5)) - 1,
            Road.remaining(RankPosition("legend", 1, 1)),
        )
        assertEquals(RankPosition("legend", 5, 5), Road.normalize(RankPosition("legend", 20, 20)))
    }

    @Test
    fun daylightSavingCountsDates() {
        val r =
            Road.target(
                RankPosition(stars = 90),
                Season(resetsAt = "2026-11-02T05:00:00Z"),
                Instant.parse("2026-11-01T04:00:00Z"),
                ZoneId.of("America/New_York"),
            )
        assertEquals(1, r.days)
    }

    @Test
    fun invalidTimestampDoesNotCrash() {
        assertNull(Road.target(RankPosition(), Season(resetsAt = "invalid"), now).daily)
    }
}
