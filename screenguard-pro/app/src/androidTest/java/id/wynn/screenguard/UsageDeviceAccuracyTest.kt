package id.wynn.screenguard

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Measures real Android UsageEvents after a controlled foreground interval, with no mocks. */
class UsageDeviceAccuracyTest {
    @Test fun fixtureDurationAgreesWithMeasuredForegroundInterval() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("accuracy")=="true")
        val instrument=InstrumentationRegistry.getInstrumentation()
        val context=instrument.targetContext
        val graph=context.applicationContext as GuardApplication
        val ui=UiDevice.getInstance(instrument)
        val pkg="id.wynn.guardfixture"
        ui.executeShellCommand("pm enable --user 0 $pkg")
        ui.executeShellCommand("appops set ${context.packageName} GET_USAGE_STATS allow")
        ui.pressHome();Thread.sleep(500)
        graph.usage.invalidate()
        val before=graph.usage.day().apps[pkg] ?: 0
        ui.executeShellCommand("am start -W -n $pkg/.FixtureActivity")
        val start=android.os.SystemClock.elapsedRealtime()
        Thread.sleep(3000)
        ui.pressHome()
        val elapsed=android.os.SystemClock.elapsedRealtime()-start
        Thread.sleep(1000)
        graph.usage.invalidate()
        val after=graph.usage.day()
        val measured=(after.apps[pkg] ?: 0)-before
        println("Usage accuracy: measured=$measured ms, foreground wait=$elapsed ms")
        assertTrue("Fixture was not recorded",measured>=2500)
        assertTrue("Foreground duration exceeds measured interval tolerance",measured<=elapsed+2000)
        assertTrue(after.available)
        assertTrue(graph.usage.details(pkg,java.time.LocalDate.now()).hourly.sum() >= measured)
    }
}
