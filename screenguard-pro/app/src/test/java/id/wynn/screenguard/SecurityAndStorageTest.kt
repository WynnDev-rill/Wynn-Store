package id.wynn.screenguard

import android.content.Context
import id.wynn.screenguard.data.GuardStore
import id.wynn.screenguard.domain.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class SecurityAndStorageTest {
    @Test fun pinUsesIndependentSaltAndRejectsIncorrectOrMalformedValues() {
        val (salt, hash) = PinCrypto.create("123456")
        assertTrue(PinCrypto.verify("123456", salt, hash))
        assertFalse(PinCrypto.verify("123457", salt, hash))
        assertFalse(PinCrypto.verify("123456", "corrupt!", hash))
        assertNotEquals(salt, PinCrypto.create("123456").first)
        assertFalse(hash.contains("123456"))
    }
    @Test fun challengeUsesMultiplicationBeforeAddition() { val c = MathChallenge(17, 8, 2); assertTrue(c.accepts("33")); assertFalse(c.accepts("50")); assertFalse(c.accepts("")) }
    @Test fun pinValidationIsExactlySixAsciiDigits() { assertTrue(PinCrypto.valid("012345")); assertFalse(PinCrypto.valid("1234")); assertFalse(PinCrypto.valid("abcdef")) }
    @Test fun savedRulesAndOwnershipSurviveRecreation() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        File(context.filesDir, "guard-state.json").delete()
        val first = GuardStore(context)
        first.rule("com.example.app") { it.copy(freeze = FreezeMode.DISABLE, ownedDisable = true, windows = listOf(TimeWindow(days = setOf(1, 2)))) }
        first.settings { it.copy(monitorEnabled = true) }
        val recreated = GuardStore(context)
        assertTrue(recreated.state.value.settings.monitorEnabled)
        assertTrue(recreated.state.value.rules.getValue("com.example.app").ownedDisable)
        assertEquals(setOf(1, 2), recreated.state.value.rules.getValue("com.example.app").windows.first().days)
    }
    @Test fun originalProductionJsonMigratesWithoutLosingDisableOwnership() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        val file = File(context.filesDir, "guard-state.json")
        file.writeText("""{"settings":{"onboardingDone":true},"rules":{"com.example.target":{"packageName":"com.example.target","freeze":"DISABLE","ownedDisable":true,"networkBlocked":true}}}""")
        val store = GuardStore(context)
        val rule = store.state.value.rules.getValue("com.example.target")
        assertNotNull(rule.gate); assertTrue(rule.ownedDisable); assertTrue(rule.networkBlocked)
        store.settings { it.copy(darkMode = true) }
        val recreated = GuardStore(context)
        assertEquals(2, recreated.state.value.schemaVersion)
        assertEquals(rule, recreated.state.value.rules.getValue("com.example.target"))
        assertTrue(recreated.state.value.settings.darkMode)
    }
    @Test fun version15StateRestoresLegacyFeaturesWithoutRefreezingOpenApps() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        val file = File(context.filesDir, "guard-state.json")
        file.writeText("""{"schemaVersion":3,"settings":{"freezeUtility":true,"onboardingDone":true,"darkMode":true},"rules":{"com.example.closed":{"packageName":"com.example.closed","managedFreeze":true,"freeze":"DISABLE","ownedDisable":true,"appliedNetwork":true,"pendingNetwork":true},"com.example.open":{"packageName":"com.example.open","managedFreeze":true}}}""")
        val store = GuardStore(context)
        assertNull(store.loadError)
        val state = store.state.value
        assertEquals(2, state.schemaVersion)
        assertTrue(state.settings.onboardingDone); assertTrue(state.settings.darkMode)
        val closed = state.rules.getValue("com.example.closed")
        assertNotNull(closed.gate); assertTrue(closed.ownedDisable)
        assertTrue(closed.appliedNetwork); assertTrue(closed.pendingNetwork)
        val open = state.rules.getValue("com.example.open")
        assertTrue(open.managedFreeze); assertNull(open.gate)
        assertFalse(PolicyEngine.decide(open, state.settings, 0, java.time.ZonedDateTime.now()).blocked)
        val apps = listOf(InstalledApp(open.packageName,"Open",11001,0,true,false,false,false),
            InstalledApp(closed.packageName,"Closed",11002,0,false,false,false,false))
        assertEquals(2, FreezeCatalog.selected(state,apps).size)
        store.settings { it.copy(dailyGoalMinutes = 347) }
        val recreated = GuardStore(context).state.value
        assertEquals(347,recreated.settings.dailyGoalMinutes)
        assertEquals(state.rules,recreated.rules)
    }
    @Test fun corruptStateIsReportedAndNeverOverwrittenSilently() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        val file = File(context.filesDir, "guard-state.json"); file.writeText("damaged")
        val store = GuardStore(context)
        assertNotNull(store.loadError)
        val result = runCatching { store.settings { it.copy(onboardingDone = true) } }
        assertTrue(result.isFailure); assertEquals("damaged", file.readText())
    }
}
