package id.wynn.screenguard

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Opt-in against the publicly distributed, privately signed APK, with the live stable feed. */
class ProductionUpdateTest {
    @Test fun publishedApkLaunchesAndChecksLiveStableFeed() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("production") == "true")
        val instrument = InstrumentationRegistry.getInstrumentation()
        val context = instrument.targetContext
        val ui = UiDevice.getInstance(instrument)
        val pkg = "id.wynn.screenguard"
        context.startActivity(context.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(ui.wait(Until.hasObject(By.pkg(pkg)), 20_000))
        fun scrollToText(text: String): UiObject2 {
            repeat(20) {
                val found = ui.findObject(By.pkg(pkg).textContains(text))
                if (found != null && found.visibleBounds.height() > 0 && found.visibleBounds.centerY() < ui.displayHeight - 80) return found
                // Compose exposes virtual accessibility nodes; legacy UiScrollable can stop early.
                // Swipe the real window instead, just as a user scrolls the native screen.
                ui.swipe(ui.displayWidth / 2, ui.displayHeight * 4 / 5, ui.displayWidth / 2, ui.displayHeight / 3, 24)
                ui.waitForIdle()
            }
            val file = File(context.getExternalFilesDir(null), "screenshots/production-test-failure.png")
            file.parentFile!!.mkdirs(); ui.takeScreenshot(file)
            ui.dumpWindowHierarchy(File(context.getExternalFilesDir(null), "production-test-window.xml"))
            error("Production screen did not expose '$text'")
        }
        if (!ui.hasObject(By.pkg(pkg).desc("Pengaturan"))) {
            scrollToText("Mulai ScreenGuard").click()
        }
        val settings = checkNotNull(ui.wait(Until.findObject(By.desc("Pengaturan")), 20_000)) { "Production dock not available" }
        settings.click()
        scrollToText("Periksa update").click()
        assertTrue("Real stable-feed check did not succeed", ui.wait(Until.hasObject(By.textContains("Kamu menggunakan versi terbaru")), 60_000))
        val file = File(context.getExternalFilesDir(null), "screenshots/15-production-live-update.png")
        file.parentFile!!.mkdirs(); ui.takeScreenshot(file)
    }
}
