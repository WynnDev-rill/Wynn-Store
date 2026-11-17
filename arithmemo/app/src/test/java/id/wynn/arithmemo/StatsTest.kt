package id.wynn.arithmemo

import id.wynn.arithmemo.core.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class StatsTest {
    @Test
    fun statisticsUseLocalDatesAndDoNotTimeSelfRatedFlashcards() {
        val f = Fact(3, Operation.ADD, 4)
        val at = Instant.parse("2026-11-17T23:30:00Z").toEpochMilli()
        val attempts =
            listOf(
                Attempt("typed", f, true, 2000, Rating.GOOD, at),
                Attempt("flash", f, true, 0, Rating.EASY, at),
                Attempt("wrong", f, false, 9000, Rating.AGAIN, at),
            )
        val stats = Stats.from(attempts, ZoneId.of("Asia/Jakarta"))
        assertEquals(3, stats.total)
        assertEquals(66, stats.accuracy)
        assertEquals(2000, stats.averageMs)
        assertEquals(3, stats.today(LocalDate.of(2026, 11, 18)))
        assertEquals(1, stats.streak(LocalDate.of(2026, 11, 18)))
        assertEquals(0, Stats.from(emptyList()).accuracy)
    }
}
