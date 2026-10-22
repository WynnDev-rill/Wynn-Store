package id.wynn.screenguard

import id.wynn.screenguard.domain.*
import id.wynn.screenguard.domain.UsageEvent.Kind.*
import org.junit.Assert.*
import org.junit.Test

class UsageTimelineTest {
    private fun e(t: Long, kind: UsageEvent.Kind, app: String = "a", activity: String = "Main") = UsageEvent(t, app, activity, kind)
    @Test fun normalSession() { assertEquals(50L, UsageTimeline.calculate(listOf(e(10, RESUME), e(60, PAUSE)), 0, 100)["a"]) }
    @Test fun openSessionIncluded() { assertEquals(90L, UsageTimeline.calculate(listOf(e(10, RESUME)), 0, 100)["a"]) }
    @Test fun crossingMidnightIsClipped() { assertEquals(50L, UsageTimeline.calculate(listOf(e(10, RESUME), e(100, PAUSE)), 50, 150)["a"]) }
    @Test fun duplicateResumeDoesNotResetStart() { assertEquals(90L, UsageTimeline.calculate(listOf(e(10, RESUME), e(30, RESUME)), 0, 100)["a"]) }
    @Test fun activitiesUseUnion() { assertEquals(70L, UsageTimeline.calculate(listOf(e(10, RESUME), e(20, RESUME, activity = "Other"), e(50, PAUSE), e(80, PAUSE, activity = "Other")), 0, 100)["a"]) }
    @Test fun screenOffClosesAllPackages() { assertEquals(mapOf("a" to 40L, "b" to 30L), UsageTimeline.calculate(listOf(e(10, RESUME), e(20, RESUME, "b"), e(50, SCREEN_OFF)), 0, 100)) }
    @Test fun shutdownDiscardsUnknownTail() { assertTrue(UsageTimeline.calculate(listOf(e(10, RESUME), e(50, SHUTDOWN)), 0, 100).isEmpty()) }
    @Test fun startupResetsUnmatchedSessions() { assertEquals(20L, UsageTimeline.calculate(listOf(e(10, RESUME), e(50, STARTUP), e(60, RESUME), e(80, PAUSE)), 0, 100)["a"]) }
    @Test fun staleEventsDoNotCreateNegativeTime() { assertTrue(UsageTimeline.calculate(listOf(e(10, RESUME), e(30, PAUSE)), 50, 100).isEmpty()) }
    @Test fun unmatchedPauseIgnored() { assertTrue(UsageTimeline.calculate(listOf(e(30, PAUSE)), 0, 100).isEmpty()) }
    @Test fun dstIsHandledByEpochClipping() { assertEquals(3_600_000L, UsageTimeline.calculate(listOf(e(0, RESUME), e(7_200_000, PAUSE)), 3_600_000, 7_200_000)["a"]) }
}
