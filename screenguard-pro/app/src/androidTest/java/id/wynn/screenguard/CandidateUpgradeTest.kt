package id.wynn.screenguard

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** UI-seeded settings verify real private data migration, not just package installation. */
class CandidateUpgradeTest {
    private val pkg="id.wynn.screenguard"
    private fun ui()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    @org.junit.Rule @JvmField val diagnostic = object : org.junit.rules.TestWatcher() {
        override fun failed(error: Throwable, description: org.junit.runner.Description) {
            val dir=java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"screenshots").apply { mkdirs() }
            ui().takeScreenshot(java.io.File(dir,"candidate-failure.png"))
            ui().dumpWindowHierarchy(java.io.File(dir,"candidate-failure.xml"))
        }
    }
    private fun open() {
        val i=InstrumentationRegistry.getInstrumentation()
        i.targetContext.startActivity(i.targetContext.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(ui().wait(Until.hasObject(By.pkg(pkg)),30000))
    }
    private fun find(text: String): UiObject2 {
        repeat(18) {
            ui().findObject(By.pkg(pkg).text("Nanti saja"))?.click()
            val n=ui().findObject(By.pkg(pkg).text(text))
            if(n!=null && n.visibleBounds.height()>0 && n.visibleBounds.centerY()<ui().displayHeight-80)return n
            ui().swipe(ui().displayWidth/2,ui().displayHeight*4/5,ui().displayWidth/2,ui().displayHeight/3,20)
            ui().waitForIdle()
        }
        error("Missing production control: $text")
    }
    private fun openSettings() {
        // Once the new release is public, the ORIGINAL 1.3.0 client offers it on startup.
        // Postpone that real dialog before seeding settings for the direct-install migration test.
        repeat(60) {
            ui().findObject(By.pkg(pkg).text("Nanti saja"))?.click()
            val settings=ui().findObject(By.pkg(pkg).desc("Pengaturan"))
            if(settings!=null) { settings.click(); return }
            Thread.sleep(500)
        }
        error("Production Settings unavailable after postponing startup update")
    }
    @Test fun seedPreviousProductionSettings() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("candidate")=="seed")
        open()
        if(!ui().hasObject(By.pkg(pkg).desc("Pengaturan")))find("Mulai ScreenGuard").click()
        openSettings()
        find("Target waktu layar").click()
        val input=checkNotNull(ui().wait(Until.findObject(By.pkg(pkg).clazz("android.widget.EditText")),10000))
        input.text="347"
        find("Simpan").click()
        assertTrue(ui().wait(Until.hasObject(By.pkg(pkg).text("5j 47m")),10000))
    }
    @Test fun candidateRetainsOnboardingAndSavedGoal() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("candidate")=="verify")
        open()
        openSettings()
        find("Target waktu layar")
        assertTrue("Saved private settings did not survive",ui().hasObject(By.pkg(pkg).text("5j 47m")))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("1.4.0",context.packageManager.getPackageInfo(pkg,0).versionName)
    }
}
