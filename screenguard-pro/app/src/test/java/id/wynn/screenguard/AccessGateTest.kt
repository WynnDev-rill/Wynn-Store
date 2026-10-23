package id.wynn.screenguard

import id.wynn.screenguard.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class AccessGateTest {
    private val now = ZonedDateTime.parse("2026-10-23T12:00:00Z")
    private val wall = now.toInstant().toEpochMilli()
    private val closed = AppRule("com.example.app", freeze = FreezeMode.DISABLE, gate = AccessGate(), ownedDisable = true)
    private val opened = closed.copy(overrideUntil = wall + 300_000, sessionStartedWall = wall,
        sessionStartedElapsed = 10_000, sessionUntilElapsed = 310_000, sessionBoot = 3)
    @Test fun gateBlocksWithoutRequiringUsagePermissionOrMonitoring() {
        assertTrue(PolicyEngine.decide(closed, GuardSettings(), 0, now).blocked)
    }
    @Test fun sessionOverridesManualFreezeUntilBothClocksExpire() {
        assertFalse(PolicyEngine.decide(opened, GuardSettings(), 0, now.plusSeconds(1), 11_000, 3).blocked)
        assertTrue(PolicyEngine.decide(opened, GuardSettings(), 0, now.plusSeconds(300), 310_000, 3).blocked)
        assertEquals(FreezeMode.DISABLE, opened.freeze)
    }
    @Test fun rollbackForwardClockChangesAndRebootNeverExtendSession() {
        assertFalse(AccessSession.isOpen(opened, wall - 1, 11_000, 3))
        assertFalse(AccessSession.isOpen(opened, wall + 10_000, 11_000, 3))
        assertFalse(AccessSession.isOpen(opened, wall + 1000, 11_000, 4))
        assertFalse(AccessSession.isOpen(opened, wall + 1000, 100, 3))
        assertFalse(AccessSession.isOpen(opened.copy(sessionUntilElapsed = 2_000_000), wall + 1000, 11_000, 3))
    }
    @Test fun automaticGateOnlyClosesWhenDailyLimitOrWindowApplies() {
        val r = closed.copy(freeze = FreezeMode.NONE, gate = AccessGate(automaticOnly = true), dailyLimitMinutes = 30)
        assertFalse(PolicyEngine.decide(r, GuardSettings(monitorEnabled = true), 1000, now).blocked)
        assertTrue(PolicyEngine.decide(r, GuardSettings(monitorEnabled = true), 30 * 60_000, now).blocked)
        assertFalse(PolicyEngine.decide(opened.copy(gate = AccessGate(automaticOnly = true)), GuardSettings(monitorEnabled = true), 100_000_000, now, 10_000, 3).blocked)
    }
    @Test fun migrationPreservesNetworkOwnershipAndAutomaticRules() {
        val legacy = GuardState(GuardSettings(unlockMode = UnlockMode.WAIT, waitSeconds = 60), mapOf(
            "com.example.app" to closed.copy(gate = null, freeze = FreezeMode.SUSPEND, ownedSuspension = true, networkBlocked = true),
            "com.example.other" to AppRule("com.example.other", dailyLimitMinutes = 15)), 1)
        val migrated = GuardMigration.migrate(legacy)
        val r = migrated.rules.getValue("com.example.app")
        assertEquals(GateMode.WAIT, r.gate?.mode); assertEquals(60, r.gate?.waitSeconds)
        assertTrue(r.ownedSuspension); assertTrue(r.networkBlocked); assertEquals(FreezeMode.DISABLE, r.freeze)
        assertEquals(15, migrated.rules.getValue("com.example.other").dailyLimitMinutes)
        assertEquals(migrated, GuardMigration.migrate(migrated))
    }
    @Test fun idleLegacyFocusSelectionNeverSilentlyFreezesAnApplication() {
        val idle = GuardState(GuardSettings(monitorEnabled = true), mapOf("com.example.app" to AppRule("com.example.app", focus = true)), 1)
        val migrated = GuardMigration.migrate(idle, wall)
        assertNull(migrated.rules.getValue("com.example.app").gate)
        assertFalse(migrated.rules.getValue("com.example.app").focus)
        assertFalse(migrated.settings.monitorEnabled)
        val active = idle.copy(settings = idle.settings.copy(focusUntil = wall + 60_000))
        assertNotNull(GuardMigration.migrate(active, wall).rules.getValue("com.example.app").gate)
    }
    @Test fun challengeDifficultyAndInvalidAnswersAreHandled() {
        MathLevel.entries.forEach { level -> repeat(50) {
            val c = MathChallenge.next(level)
            assertTrue(c.accepts((c.a + c.b * c.c).toString()))
            assertFalse(c.accepts("1.5")); assertFalse(c.accepts("999999999999"))
            if (level == MathLevel.EASY) assertEquals(1, c.c)
        } }
    }
    @Test fun sessionsKeepRecoveryServiceAliveEvenWhenNoDailyRulesExist() {
        assertTrue(GuardState(rules = mapOf(opened.packageName to opened)).needsMonitoring)
        assertFalse(GuardState(rules = mapOf(closed.packageName to closed)).needsMonitoring)
    }
    @Test(expected = IllegalArgumentException::class) fun gateCannotRequestUnboundedAccess() { AccessGate(accessMinutes = 31) }
}
