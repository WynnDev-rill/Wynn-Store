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
        val scroll = UiScrollable(UiSelector().scrollable(true)).setAsVerticalList()
        if (!ui.hasObject(By.desc("Pengaturan"))) {
            assertTrue(scroll.scrollIntoView(UiSelector().text("Mulai ScreenGuard")))
            ui.findObject(By.text("Mulai ScreenGuard")).click()
        }
        val settings = checkNotNull(ui.wait(Until.findObject(By.desc("Pengaturan")), 20_000)) { "Production dock not available" }
        settings.click()
        assertTrue(scroll.scrollIntoView(UiSelector().textContains("Periksa update")))
        ui.findObject(By.textContains("Periksa update"))?.click()
        assertTrue("Real stable-feed check did not succeed", ui.wait(Until.hasObject(By.textContains("Kamu menggunakan versi terbaru")), 60_000))
        val file = File(context.getExternalFilesDir(null), "screenshots/15-production-live-update.png")
        file.parentFile!!.mkdirs(); ui.takeScreenshot(file)
    }
}
