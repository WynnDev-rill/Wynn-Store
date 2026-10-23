package id.wynn.screenguard

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Real 1.1 production client must detect/download/verify the published 1.2 APK without a manual check. */
class ProductionUpgradeTest {
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
        download.click()
        assertTrue("Downloaded release did not pass the old client's size/hash/package/version/certificate checks",
            ui.wait(Until.hasObject(By.pkg(pkg).text("Pasang update")), 60_000))
        val file = File(context.getExternalFilesDir(null), "screenshots/17-production-upgrade-verified.png")
        file.parentFile!!.mkdirs(); ui.takeScreenshot(file)
    }
}
