package id.wynn.screenguard

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Runs the original production client against the real public feed and Android package installer. */
class ProductionUpgradeTest {
    @org.junit.Rule @JvmField val diagnostic = object : org.junit.rules.TestWatcher() {
        override fun failed(error: Throwable, description: org.junit.runner.Description) {
            val i = InstrumentationRegistry.getInstrumentation()
            val ui = UiDevice.getInstance(i)
            val dir = File(i.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
            ui.takeScreenshot(File(dir, "upgrade-failure.png"))
            ui.dumpWindowHierarchy(File(dir, "upgrade-failure.xml"))
        }
    }
    @Test fun previousProductionAutomaticallyDetectsAndVerifiesTheNewRelease() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("upgrade") == "true")
        val instrument = InstrumentationRegistry.getInstrumentation()
        val context = instrument.targetContext
        val ui = UiDevice.getInstance(instrument)
        val pkg = "id.wynn.screenguard"
        val expected = checkNotNull(InstrumentationRegistry.getArguments().getString("releaseVersion"))
        context.startActivity(context.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(ui.wait(Until.hasObject(By.pkg(pkg)), 20_000))
        // First install may still be on onboarding. An available update is offered immediately after completion.
        if (!ui.hasObject(By.pkg(pkg).desc("Pengaturan"))) {
            var started = false
            repeat(20) {
                if (!started) {
                    val button = ui.findObject(By.pkg(pkg).textContains("Mulai ScreenGuard"))
                    if (button != null && button.visibleBounds.height() > 0 && button.visibleBounds.centerY() < ui.displayHeight - 80) {
                        button.click(); started = true
                    } else {
                        ui.swipe(ui.displayWidth / 2, ui.displayHeight * 4 / 5, ui.displayWidth / 2, ui.displayHeight / 3, 24)
                        ui.waitForIdle()
                    }
                }
            }
            assertTrue("Could not complete production onboarding", started)
        }
        assertTrue("Opening the old APK did not automatically offer the real new release",
            ui.wait(Until.hasObject(By.pkg(pkg).text("ScreenGuard Pro $expected")), 60_000))
        val download = checkNotNull(ui.wait(Until.findObject(By.pkg(pkg).textContains("Unduh update")), 10_000)) { "No real download action" }
        val before = context.packageManager.getPackageInfo(pkg, 0)
        val beforeUid = context.packageManager.getApplicationInfo(pkg, 0).uid
        download.click()
        assertTrue("Downloaded release did not pass the old client's size/hash/package/version/certificate checks",
            ui.wait(Until.hasObject(By.pkg(pkg).text("Pasang update")), 60_000))
        val file = File(context.getExternalFilesDir(null), "screenshots/17-production-upgrade-verified.png")
        file.parentFile!!.mkdirs(); ui.takeScreenshot(file)
        if (InstrumentationRegistry.getArguments().getString("installUpdate") == "true") {
            ui.findObject(By.pkg(pkg).text("Pasang update")).click()
            val install = ui.wait(Until.findObject(By.res("android", "button1")), 30_000)
            assertNotNull("Android installer confirmation was not shown", install)
            assertTrue("Confirmation must belong to the system package installer", install!!.applicationPackage.contains("packageinstaller"))
            install.click()
            val deadline = android.os.SystemClock.elapsedRealtime() + 60_000
            while (context.packageManager.getPackageInfo(pkg, 0).versionName != expected && android.os.SystemClock.elapsedRealtime() < deadline) {
                Thread.sleep(500)
            }
            val after = context.packageManager.getPackageInfo(pkg, 0)
            assertEquals("Android installer did not apply the update", expected, after.versionName)
            assertEquals("Update must retain original install identity", before.firstInstallTime, after.firstInstallTime)
            assertEquals("Update must retain UID and app data sandbox", beforeUid, context.packageManager.getApplicationInfo(pkg, 0).uid)
            context.startActivity(context.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue("Previously completed onboarding data must survive update", ui.wait(Until.hasObject(By.pkg(pkg).desc("Pengaturan")), 30_000))
            ui.takeScreenshot(File(file.parentFile, "18-production-installed-in-place.png"))
        }
    }
}
