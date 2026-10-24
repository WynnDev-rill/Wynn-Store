package id.wynn.screenguard

import id.wynn.screenguard.domain.*
import id.wynn.screenguard.data.*
import java.time.LocalDate
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class FreezeUtilityTest {
    @Test fun migrationKeepsManualLocksAndJournalsButRetiresEnforcement() {
        val old=GuardState(GuardSettings(monitorEnabled=true,pinHash="old"),mapOf(
            "com.test.manual" to AppRule("com.test.manual",gate=AccessGate(),freeze=FreezeMode.DISABLE,ownedDisable=true,networkBlocked=true,appliedNetwork=true),
            "com.test.auto" to AppRule("com.test.auto",gate=AccessGate(automaticOnly=true),dailyLimitMinutes=5,ownedDisable=true)))
        val n=FreezeUtilityMigration.migrate(old)
        val a=n.rules.getValue("com.test.manual");val b=n.rules.getValue("com.test.auto")
        assertTrue(a.managedFreeze);assertTrue(a.ownedDisable);assertEquals(FreezeMode.DISABLE,a.freeze)
        assertFalse(a.networkBlocked);assertTrue(a.pendingNetwork);assertTrue(a.appliedNetwork)
        assertNull(a.gate);assertFalse(b.managedFreeze);assertTrue(b.ownedDisable);assertEquals(FreezeMode.NONE,b.freeze)
        assertEquals(0,b.dailyLimitMinutes);assertFalse(n.needsMonitoring);assertFalse(n.settings.hasPin)
        assertEquals(n,FreezeUtilityMigration.migrate(n))
    }
    @Test fun upgradeKeepsOnlyAProvenLiveLeaseOpenAndNeverRevivesARebootedLease() {
        val wall=1000000L
        val rule=AppRule("com.test.app",gate=AccessGate(),freeze=FreezeMode.DISABLE,ownedDisable=true,
            sessionStartedWall=wall-1000,sessionStartedElapsed=1000,sessionUntilElapsed=61000,sessionBoot=4,overrideUntil=wall+59000)
        val old=GuardState(rules=mapOf(rule.packageName to rule))
        assertEquals(FreezeMode.NONE,FreezeUtilityMigration.migrate(old,wall,2000,4).rules.getValue(rule.packageName).freeze)
        assertEquals(FreezeMode.DISABLE,FreezeUtilityMigration.migrate(old,wall,2000,5).rules.getValue(rule.packageName).freeze)
        assertEquals(FreezeMode.DISABLE,FreezeUtilityMigration.migrate(old,wall,70000,4).rules.getValue(rule.packageName).freeze)
    }
    @Test fun unfreezingNeverRearmsAChallengeOrDailyLimit() {
        val rule=AppRule("com.test.app",managedFreeze=true,dailyLimitMinutes=1,gate=AccessGate())
        val s=GuardSettings(freezeUtility=true,monitorEnabled=true)
        assertFalse(PolicyEngine.decide(rule,s,999999999,ZonedDateTime.now()).blocked)
        assertTrue(PolicyEngine.decide(rule.copy(freeze=FreezeMode.DISABLE),s,0,ZonedDateTime.now()).blocked)
        val app=InstalledApp(rule.packageName,"Demo",10123,0,true,false,false,false)
        assertEquals(listOf(app),FreezeCatalog.selected(GuardState(s,mapOf(rule.packageName to rule)),listOf(app)))
        assertFalse(FreezeCatalog.canAdd(app,rule))
    }
    @Test fun periodUsesBoundedAvailableDaysAndKeepsAppTotalIndependent() {
        val today=LocalDate.of(2026,10,24)
        val data=listOf(UsageDay(today,mapOf("a" to 120000L,"b" to 120000L),screenMs=180000L),UsageDay(today.minusDays(1),mapOf("a" to 999999L),available=false),UsageDay(today.minusDays(8),mapOf("a" to 999999L)))
        val s=ActivitySummary.of(data,today,7)
        assertEquals(180000L,s.screenMs);assertEquals(240000L,s.appMs);assertEquals(1,s.available.size);assertTrue(s.partial)
        assertEquals(120000L,s.apps["a"])
    }
}
