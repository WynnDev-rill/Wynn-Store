package id.wynn.screenguard

import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import id.wynn.screenguard.privilege.ShizukuStatus
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.ui.GuardViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in: run only on an emulator with Shizuku shell service and device-fixture installed. */
@RunWith(AndroidJUnit4::class)
class PrivilegedDeviceTest {
    @Suppress("DEPRECATION") @Test fun shellShizukuActuallyBlocksNetworkSuspendsDisablesAndRestores() = runBlocking {
        val instrument = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("privileged") == "true")
        check(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk")) { "Destructive fixture checks are restricted to emulators" }
        val context = instrument.targetContext
        val graph = context.applicationContext as GuardApplication
        val ui = UiDevice.getInstance(instrument)
        ui.wakeUp()
        ui.executeShellCommand("wm dismiss-keyguard")
        EmulatorUiRecovery.dismissLauncherDialog(ui)
        fun openMain() = context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        openMain()
        var visible = ui.wait(Until.hasObject(By.pkg(context.packageName)), 20_000)
        if (!visible && EmulatorUiRecovery.dismissLauncherDialog(ui)) { openMain(); visible = ui.wait(Until.hasObject(By.pkg(context.packageName)), 15_000) }
        check(visible) { "Main activity did not become visible" }
        ui.waitForIdle()
        withTimeout(20_000) { while (!Shizuku.pingBinder()) delay(200) }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            EmulatorUiRecovery.dismissLauncherDialog(ui)
            instrument.runOnMainSync { graph.gateway.requestPermission() }
            val allow = ui.wait(Until.findObject(By.res("android", "button1").pkg("moe.shizuku.privileged.api")), 25_000)
            checkNotNull(allow) { "Shizuku permission dialog did not appear" }.click()
        }
        graph.gateway.refresh()
        withTimeout(20_000) { while (graph.gateway.status.value != ShizukuStatus.READY) delay(200) }
        assertEquals(2000, Shizuku.getUid()) // Test the no-root path.
        assertTrue(graph.gateway.firewallSupported.value)
        ui.executeShellCommand("cmd appops set ${context.packageName} GET_USAGE_STATS ignore")
        assertFalse(graph.usage.hasAccess())
        graph.gateway.grantOwnUsageAccess()
        assertTrue(graph.usage.hasAccess())
        val pkg = "id.wynn.guardfixture"
        val pm = context.packageManager
        val info = pm.getApplicationInfo(pkg, 0)
        val user = info.uid / 100000
        fun probe(): String? {
            val latch = CountDownLatch(1)
            var result: String? = null
            context.sendOrderedBroadcast(Intent().setComponent(ComponentName(pkg, "$pkg.ProbeReceiver")).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES), null,
                object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { result = resultData; latch.countDown() } }, null, 0, null, null)
            check(latch.await(12, TimeUnit.SECONDS)) { "Fixture probe timed out" }
            return result
        }
        try {
            assertEquals("ONLINE", probe())
            graph.gateway.network(pkg, info.uid, true)
            assertEquals("BLOCKED", probe())
            graph.gateway.network(pkg, info.uid, false)
            assertEquals("ONLINE", probe())
            graph.gateway.freeze(pkg, user, 1, true)
            assertTrue(pm.isPackageSuspended(pkg))
            graph.gateway.freeze(pkg, user, 1, false)
            assertFalse(pm.isPackageSuspended(pkg))
            graph.gateway.freeze(pkg, user, 2, true)
            assertFalse(pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).enabled)
            assertNull("Disabled app must disappear from normal launcher resolution", pm.getLaunchIntentForPackage(pkg))
            graph.gateway.freeze(pkg, user, 2, false)
            assertTrue(pm.getApplicationInfo(pkg, 0).enabled)
            graph.store.settings { it.copy(freezeUtility = true, pinHash = "", pinSalt = "") }
            val vm = GuardViewModel(graph)
            val app = graph.catalog.list().first { it.packageName == pkg }
            vm.addManagedApps(listOf(app))
            withTimeout(15_000) { while (vm.busy.value) delay(100) }
            assertFalse(pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).enabled)
            assertTrue(graph.store.state.value.rules.getValue(pkg).managedFreeze)
            assertNull(graph.store.state.value.rules.getValue(pkg).gate)
            var opened = false
            vm.setManualFreeze(app, false) { opened = true }
            withTimeout(15_000) { while (vm.busy.value) delay(100) }
            assertTrue("Unfreeze failed: ${vm.message.value}", opened)
            assertTrue(pm.getApplicationInfo(pkg, 0).enabled)
            assertTrue(graph.store.state.value.rules.getValue(pkg).managedFreeze)
            assertFalse(graph.store.state.value.needsMonitoring)
            vm.setManualFreeze(app, true)
            withTimeout(15_000) { while (vm.busy.value) delay(100) }
            assertFalse(pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).enabled)
            vm.removeManagedApp(app)
            withTimeout(15_000) { while (vm.busy.value) delay(100) }
            assertTrue(pm.getApplicationInfo(pkg, 0).enabled)
            assertFalse(graph.store.state.value.rules.getValue(pkg).managedFreeze)
        } finally {
            runCatching { graph.controller.restoreAll() }
            runCatching { graph.gateway.scheduleRefreeze(pkg, user, 0) }
            runCatching { graph.gateway.network(pkg, info.uid, false) }
            runCatching { graph.gateway.freeze(pkg, user, 2, false) }
            runCatching { graph.gateway.freeze(pkg, user, 1, false) }
        }
    }
    @Test fun armDaemonTimerForExternalForceStopCheck() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("watchdog") == "true")
        check(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val graph = context.applicationContext as GuardApplication
        graph.gateway.refresh()
        withTimeout(20_000) { while (graph.gateway.status.value != ShizukuStatus.READY) delay(100) }
        val pkg = "id.wynn.guardfixture"
        graph.gateway.freeze(pkg, 0, 2, false)
        graph.gateway.scheduleRefreeze(pkg, 0, android.os.SystemClock.elapsedRealtime() + 10_000)
    }
}
