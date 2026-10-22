package id.wynn.screenguard

import id.wynn.screenguard.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class PolicyEngineTest {
    private val monday = ZonedDateTime.parse("2026-10-19T23:00:00+07:00[Asia/Jakarta]")
    private val active = GuardSettings(monitorEnabled = true)
    @Test fun overnightBelongsToStartDay() {
        val w = TimeWindow(days = setOf(1))
        assertTrue(w.contains(monday)); assertTrue(w.contains(monday.plusHours(6)))
        assertFalse(w.contains(monday.plusHours(7))); assertFalse(w.contains(monday.plusDays(1)))
    }
    @Test fun daytimeIsHalfOpen() {
        val w = TimeWindow(startMinute = 60, endMinute = 120)
        assertTrue(w.contains(monday.withHour(1).withMinute(0)))
        assertFalse(w.contains(monday.withHour(2).withMinute(0)))
    }
    @Test fun identicalTimesNeverLockAllDay() { assertFalse(TimeWindow(startMinute = 0, endMinute = 0).contains(monday)) }
    @Test fun disabledScheduleNeverLocks() { assertFalse(TimeWindow(enabled = false).contains(monday)) }
    @Test fun exactDailyLimitBlocks() {
        val rule = AppRule("a.b", dailyLimitMinutes = 1)
        assertFalse(PolicyEngine.decide(rule, active, 59_999, monday).blocked)
        assertTrue(PolicyEngine.decide(rule, active, 60_000, monday).blocked)
    }
    @Test fun zeroLimitIsUnlimited() { assertFalse(PolicyEngine.decide(AppRule("a.b"), active, Long.MAX_VALUE, monday).blocked) }
    @Test fun monitoringOffReleasesAutomaticRules() { assertFalse(PolicyEngine.decide(AppRule("a.b", dailyLimitMinutes = 1, windows = listOf(TimeWindow())), GuardSettings(), 90_000, monday).blocked) }
    @Test fun manualFreezeDoesNotDependOnMonitor() { assertTrue(PolicyEngine.decide(AppRule("a.b", freeze = FreezeMode.DISABLE), GuardSettings(), 0, monday).blocked) }
    @Test fun overrideExpiresAtExactBoundary() {
        val deadline = monday.toInstant().toEpochMilli()
        val r = AppRule("a.b", dailyLimitMinutes = 1, overrideUntil = deadline)
        assertFalse(PolicyEngine.decide(r, active, 90_000, monday.minusNanos(1_000_000)).blocked)
        assertTrue(PolicyEngine.decide(r, active, 90_000, monday).blocked)
    }
    @Test fun focusOnlyBlocksSelectedPackages() {
        val s = active.copy(focusUntil = monday.plusMinutes(10).toInstant().toEpochMilli())
        assertTrue(PolicyEngine.decide(AppRule("a.b", focus = true), s, 0, monday).blocked)
        assertFalse(PolicyEngine.decide(AppRule("a.b"), s, 0, monday).blocked)
    }
    @Test fun manualFreezeCannotBeOverriddenByStaleGrace() {
        assertTrue(PolicyEngine.decide(AppRule("a.b", freeze = FreezeMode.SUSPEND, overrideUntil = Long.MAX_VALUE), active, 0, monday).blocked)
    }
    @Test fun invalidScheduleRejected() { assertThrows(IllegalArgumentException::class.java) { TimeWindow(startMinute = 1500) } }
    @Test fun invalidDailyLimitRejected() { assertThrows(IllegalArgumentException::class.java) { AppRule("a.b", dailyLimitMinutes = -1) } }
    @Test fun safetyRejectsCoreUidSelfLauncherAndShizuku() {
        listOf("com.android.systemui", "com.miui.home.launcher", "moe.shizuku.privileged.api", "id.wynn.screenguard.debug").forEach { assertTrue(PackageSafety.protected(it, 10050, "id.wynn.screenguard")) }
        assertTrue(PackageSafety.protected("com.example.app", 1000, "id.wynn.screenguard"))
        assertFalse(PackageSafety.protected("com.google.android.youtube", 10123, "id.wynn.screenguard"))
    }
    @Test fun shellInjectionNamesRejected() {
        listOf("com.app;reboot", "com.app\n", "-a", "a b", "a", "com.app$(id)").forEach { assertFalse(PackageSafety.validPackage(it)) }
        assertTrue(PackageSafety.validPackage("com.example.app_1"))
    }
}
