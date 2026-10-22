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
        override suspend fun network(pkg: String, uid: Int, blocked: Boolean) { if (failNetwork) error("Firewall unavailable") }
        override suspend fun freeze(pkg: String, userId: Int, mode: Int, enabled: Boolean) { beforeFreeze?.invoke(); calls += mode to enabled }
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
}
