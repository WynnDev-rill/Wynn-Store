package id.wynn.vocamemo

import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test
    fun normalizedExactAnswers() {
        assertTrue(Answers.matches("  RUMAH  ", "rumah"))
        assertTrue(Answers.matches("cafe\u0301", "café"))
        assertFalse(Answers.matches("rum", "rumah"))
        assertFalse(Answers.matches("", "rumah"))
        assertFalse(Answers.matches("buku", "buku | kitab")) // chosen meaning is one answer
    }

    @Test
    fun adaptiveSchedule() {
        val now = 1_000_000L
        var m = Memo(word = "apple", meaning = "apel")
        m = Scheduler.grade(m, 3, now)
        assertEquals(1, m.repetitions)
        assertEquals(now + 86_400_000L, m.due)
        m = Scheduler.grade(m, 3, now)
        assertEquals(6, m.interval)
        m = Scheduler.grade(m, 4, now)
        assertTrue(m.interval > 6)
        m = Scheduler.grade(m, 1, now)
        assertEquals(0, m.repetitions)
        assertEquals(now + 60_000, m.due)
        assertEquals(1, m.lapses)
        repeat(100) { m = Scheduler.grade(m, 2, now) }
        assertTrue(m.ease >= 1.3)
        assertTrue(m.interval <= 3650)
    }

    @Test
    fun updateTrustBoundary() {
        assertTrue(
            UpdatePolicy.trusted(
                "https://github.com/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v1.0.1/VocaMemo.apk"
            )
        )
        assertFalse(
            UpdatePolicy.trusted(
                "http://github.com/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v1/VocaMemo.apk"
            )
        )
        assertFalse(
            UpdatePolicy.trusted(
                "https://github.com.evil.org/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v1/VocaMemo.apk"
            )
        )
        assertFalse(
            UpdatePolicy.trusted(
                "https://github.com/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v1/../VocaMemo.apk"
            )
        )
        assertFalse(
            UpdatePolicy.trusted(
                "https://github.com/WynnDev-rill/Wynn-Store/releases/download/screenguard-v1/VocaMemo.apk"
            )
        )
        assertFalse(
            UpdatePolicy.trusted(
                "https://github.com/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v1/VocaMemo.apk?x=1"
            )
        )
    }
}
