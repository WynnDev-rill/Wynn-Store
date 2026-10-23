package id.wynn.screenguard

import id.wynn.screenguard.domain.*
import id.wynn.screenguard.domain.UsageEvent.Kind.*
import org.junit.Assert.*
import org.junit.Test

class ScreenTimeAccuracyTest {
    private fun e(t: Long, k: UsageEvent.Kind, pkg: String = "a", activity: String = "Main") = UsageEvent(t,pkg,activity,k)
    @Test fun splitScreenDoesNotDoubleCountPhysicalScreenTime() {
        val events = listOf(e(10,RESUME),e(20,RESUME,"b"),e(80,PAUSE),e(90,PAUSE,"b"))
        assertEquals(140L, UsageTimeline.calculate(events,0,100).values.sum())
        val screen = UsageTimeline.screenTime(events,0,100)
        assertEquals(80L,screen.durationMs); assertTrue(screen.estimated)
    }
    @Test fun lockedScreenAndAmbientDisplayAreExcluded() {
        val events=listOf(e(0,LOCK),e(10,SCREEN_ON),e(20,UNLOCK),e(80,LOCK),e(90,SCREEN_OFF))
        val s=UsageTimeline.screenTime(events,0,100)
        assertEquals(60L,s.durationMs);assertFalse(s.estimated);assertEquals(1,s.unlocks)
    }
    @Test fun screenTimeIncludesHomeAndThisAppIndependentlyOfPackageTotals() {
        val events=listOf(e(10,SCREEN_ON),e(20,RESUME,"launcher"),e(30,PAUSE,"launcher"),e(40,RESUME,"guard"),e(60,PAUSE,"guard"),e(90,SCREEN_OFF))
        assertEquals(80L,UsageTimeline.screenTime(events,0,100).durationMs)
    }
    @Test fun midnightAndOpenCurrentSessionAreClippedToSelectedDay() {
        assertEquals(50L,UsageTimeline.screenTime(listOf(e(10,SCREEN_ON),e(150,SCREEN_OFF)),100,200).durationMs)
        assertEquals(60L,UsageTimeline.screenTime(listOf(e(10,SCREEN_ON)),100,160).durationMs)
    }
    @Test fun shutdownUnknownTailNeverInventsHours() {
        val events=listOf(e(10,SCREEN_ON),e(20,RESUME),e(50,SHUTDOWN),e(80,STARTUP))
        assertEquals(0L,UsageTimeline.screenTime(events,0,1000).durationMs)
        assertTrue(UsageTimeline.calculate(events,0,1000).isEmpty())
    }
    @Test fun duplicateScreenEventsDoNotResetOrDuplicateIntervals() {
        val events=listOf(e(10,SCREEN_ON),e(20,SCREEN_ON),e(30,UNLOCK),e(90,SCREEN_OFF),e(95,SCREEN_OFF))
        assertEquals(80L,UsageTimeline.screenTime(events,0,100).durationMs)
    }
    @Test fun lockClosesForegroundSessionsEvenWithoutPauseEvent() {
        val events=listOf(e(10,RESUME),e(40,LOCK),e(50,RESUME,"b"),e(80,SCREEN_OFF))
        assertEquals(mapOf("a" to 30L),UsageTimeline.calculate(events,0,100))
    }
    @Test fun resumeBeforeUnlockIsNotLost() {
        val events=listOf(e(0,LOCK),e(10,SCREEN_ON),e(15,RESUME),e(20,UNLOCK),e(80,PAUSE),e(90,SCREEN_OFF))
        assertEquals(60L,UsageTimeline.calculate(events,0,100)["a"])
    }
    @Test fun missingDisplayStartUsesForegroundFallbackInsteadOfFalseZero() {
        val events=listOf(e(10,RESUME),e(80,PAUSE),e(90,SCREEN_OFF))
        val screen=UsageTimeline.screenTime(events,0,100)
        assertEquals(70L,screen.durationMs);assertTrue(screen.estimated)
    }
    @Test fun unionHandlesNestedAdjacentAndDisjointIntervals() {
        assertEquals(30L,UsageTimeline.unionDuration(listOf(0L to 10L,2L to 5L,10L to 20L,30L to 40L,50L to 50L)))
    }
}
