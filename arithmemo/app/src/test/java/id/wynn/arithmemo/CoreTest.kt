package id.wynn.arithmemo

import id.wynn.arithmemo.core.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test
    fun allOperationsRespectDigitsAndIntegerDivision() {
        for (digits in 1..3) for (op in Operation.entries) for (group in FactGroup.entries) {
            val config = PracticeConfig(setOf(op), digits, group)
            repeat(300) {
                val f = Facts.random(config, Random(it))
                val range = Facts.range(digits)
                assertTrue("$f $digits", f.a in range && f.b in range)
                assertEquals(op, f.op)
                assertTrue(group.contains(f, digits))
                assertTrue(f.answer >= 0)
                if (op == Operation.DIVIDE) assertEquals(0, f.a % f.b)
            }
        }
    }

    @Test
    fun schedulerRepeatsMistakesAndExpandsSuccessfulIntervals() {
        val now = 1_000_000L
        val p = Scheduler.review(Progress(), Rating.GOOD, now)
        assertEquals(1, p.streak)
        assertEquals(now + 86_400_000L, p.due)
        val q = Scheduler.review(p, Rating.GOOD, p.due)
        assertTrue(q.intervalDays > p.intervalDays)
        val failed = Scheduler.review(q, Rating.AGAIN, q.due)
        assertEquals(0, failed.streak)
        assertEquals(q.due + 60_000L, failed.due)
        assertFalse(failed.mastered)
    }

    @Test
    fun factKeyRoundTripsAndRejectsInvalidOperands() {
        val f = Fact(12, Operation.MULTIPLY, 34)
        assertEquals(f, Fact.parse(f.key))
        assertEquals(408, f.answer)
        assertThrows(IllegalArgumentException::class.java) { Fact(1, Operation.DIVIDE, 0) }
        assertThrows(IllegalArgumentException::class.java) { Fact.parse("bad") }
    }

    @Test
    fun boundedListIsStableUniqueAndPaged() {
        val cfg = PracticeConfig(setOf(Operation.ADD), 1)
        val deck = Facts.catalog(cfg)
        assertEquals(81, deck.size)
        assertEquals(deck.size, deck.map { it.key }.toSet().size)
        assertEquals(deck, Facts.catalog(cfg))
    }
}
