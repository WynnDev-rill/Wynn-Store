package id.wynn.screenguard

import android.app.Application
import android.content.Context
import android.content.pm.*
import id.wynn.screenguard.data.*
import id.wynn.screenguard.domain.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RecoveryTest {
    private val pkg = "com.example.guardtarget"
    private fun context(disabled: Boolean = false, suspended: Boolean = false): Context {
        val context = RuntimeEnvironment.getApplication() as Context
        File(context.filesDir, "guard-state.json").delete()
        Shadows.shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = pkg
            applicationInfo = ApplicationInfo().apply {
                packageName = pkg; uid = 10123; enabled = !disabled; nonLocalizedLabel = "Fixture"
                if (suspended) flags = flags or ApplicationInfo.FLAG_SUSPENDED
            }
        })
        return context
    }
    private class Backend(val beforeFreeze: (() -> Unit)? = null) : PrivilegedBackend {
        val calls = mutableListOf<Pair<Int, Boolean>>()
        var failNetwork = false
        var failTimer = false
        val operations = mutableListOf<String>()
        override suspend fun scheduleRefreeze(pkg: String, userId: Int, deadlineElapsed: Long) { operations += if (deadlineElapsed == 0L) "cancel" else "arm"; if (failTimer) error("Timer unavailable") }
        override suspend fun network(pkg: String, uid: Int, blocked: Boolean) { if (failNetwork) error("Firewall unavailable") }
        override suspend fun freeze(pkg: String, userId: Int, mode: Int, enabled: Boolean) { beforeFreeze?.invoke(); operations += if (enabled) "disable" else "enable"; calls += mode to enabled }
    }
    @Test fun firewallFailureCannotPreventRestoringDisabledApp() = runTest {
        val context = context(disabled = true)
        val store = GuardStore(context)
        store.rule(pkg) { it.copy(freeze = FreezeMode.DISABLE, ownedDisable = true, networkBlocked = true, appliedNetwork = true) }
        val backend = Backend().apply { failNetwork = true }
        val controller = PolicyController(store, AppCatalog(context), UsageRepository(context), backend)
        val errors = controller.restoreAll()
        assertTrue(errors.any { it.contains("Firewall unavailable") })
        assertTrue(backend.calls.contains(2 to false))
        assertFalse(store.state.value.rules.getValue(pkg).ownedDisable)
    }
    @Test fun externalSuspensionIsNeverReleased() = runTest {
        val context = context(suspended = true)
        val store = GuardStore(context); store.rule(pkg) { it }
        val backend = Backend()
        PolicyController(store, AppCatalog(context), UsageRepository(context), backend).restoreAll()
        assertTrue(backend.calls.isEmpty())
    }
    @Test fun ownershipIsJournaledBeforeCallingAndroid() = runTest {
        val context = context()
        val store = GuardStore(context); store.rule(pkg) { it.copy(freeze = FreezeMode.SUSPEND) }
        val backend = Backend { assertTrue(store.state.value.rules.getValue(pkg).ownedSuspension) }
        val errors = PolicyController(store, AppCatalog(context), UsageRepository(context), backend).reconcile()
        assertTrue(errors.isEmpty()); assertEquals(listOf(1 to true), backend.calls)
    }
    @Test fun failureToArmTimerCannotEnableClosedApp() = runTest {
        val context = context(disabled = true)
        val store = GuardStore(context)
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(1))
        val wall = System.currentTimeMillis(); val elapsed = android.os.SystemClock.elapsedRealtime()
        store.rule(pkg) { it.copy(gate = AccessGate(), freeze = FreezeMode.DISABLE, ownedDisable = true,
            overrideUntil = wall + 300_000, sessionStartedWall = wall, sessionStartedElapsed = elapsed,
            sessionUntilElapsed = elapsed + 300_000, sessionBoot = UsageRepository(context).bootCount()) }
        val backend = Backend().apply { failTimer = true }
        val errors = PolicyController(store, AppCatalog(context), UsageRepository(context), backend).reconcile()
        assertTrue(errors.any { it.contains("Timer unavailable") })
        assertEquals("arm", backend.operations.first())
        assertFalse(backend.calls.contains(2 to false))
        assertTrue(store.state.value.rules.getValue(pkg).ownedDisable)
    }
    @Test fun restoreCancelsDaemonLeaseBeforeEnablingAndReleasesOwnership() = runTest {
        val context = context(disabled = true)
        val store = GuardStore(context)
        store.rule(pkg) { it.copy(gate = AccessGate(), freeze = FreezeMode.DISABLE, ownedDisable = true) }
        val backend = Backend()
        assertTrue(PolicyController(store, AppCatalog(context), UsageRepository(context), backend).restoreAll().isEmpty())
        assertEquals(listOf("cancel", "enable"), backend.operations)
        assertNull(store.state.value.rules.getValue(pkg).gate)
        assertFalse(store.state.value.rules.getValue(pkg).ownedDisable)
    }

}
