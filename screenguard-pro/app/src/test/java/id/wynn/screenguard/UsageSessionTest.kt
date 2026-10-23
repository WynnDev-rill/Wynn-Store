package id.wynn.screenguard
import id.wynn.screenguard.domain.*
import org.junit.Assert.*
import org.junit.Test

class UsageSessionTest {
    private fun event(time: Long, kind: UsageEvent.Kind, activity: String = "A") = UsageEvent(time, "com.example.app", activity, kind)
    @Test fun midnightClippingPreservesOriginalOpeningForAccurateCounts() {
        val result = UsageTimeline.sessions(listOf(event(50, UsageEvent.Kind.RESUME), event(150, UsageEvent.Kind.PAUSE)), 100, 200).single()
        assertEquals(100L, result.start); assertEquals(150L, result.end); assertEquals(50L, result.openedAt)
    }
    @Test fun overlappingActivitiesAreOneForegroundSession() {
        val result = UsageTimeline.sessions(listOf(event(10, UsageEvent.Kind.RESUME), event(20, UsageEvent.Kind.RESUME, "B"),
            event(30, UsageEvent.Kind.PAUSE), event(40, UsageEvent.Kind.PAUSE, "B")), 0, 100)
        assertEquals(1, result.size); assertEquals(30L, result.single().end - result.single().start)
    }
    @Test fun shutdownNeverInventsLongSession() {
        assertTrue(UsageTimeline.sessions(listOf(event(10, UsageEvent.Kind.RESUME), event(30, UsageEvent.Kind.SHUTDOWN)), 0, 1000).isEmpty())
    }
}
