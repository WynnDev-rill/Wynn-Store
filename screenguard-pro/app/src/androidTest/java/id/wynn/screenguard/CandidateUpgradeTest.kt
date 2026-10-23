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
    private fun open() {
        val i=InstrumentationRegistry.getInstrumentation()
        i.targetContext.startActivity(i.targetContext.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(ui().wait(Until.hasObject(By.pkg(pkg)),30000))
    }
    private fun find(text: String): UiObject2 {
        repeat(18) {
            val n=ui().findObject(By.pkg(pkg).text(text))
            if(n!=null && n.visibleBounds.height()>0 && n.visibleBounds.centerY()<ui().displayHeight-80)return n
            ui().swipe(ui().displayWidth/2,ui().displayHeight*4/5,ui().displayWidth/2,ui().displayHeight/3,20)
            ui().waitForIdle()
        }
        error("Missing production control: $text")
    }
    @Test fun seedPreviousProductionSettings() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("candidate")=="seed")
        open()
        if(!ui().hasObject(By.pkg(pkg).desc("Pengaturan")))find("Mulai ScreenGuard").click()
        checkNotNull(ui().wait(Until.findObject(By.pkg(pkg).desc("Pengaturan")),30000)).click()
        find("Target waktu layar").click()
        val input=checkNotNull(ui().wait(Until.findObject(By.pkg(pkg).clazz("android.widget.EditText")),10000))
        input.text="347"
        find("Simpan").click()
        assertTrue(ui().wait(Until.hasObject(By.pkg(pkg).text("5j 47m")),10000))
    }
    @Test fun candidateRetainsOnboardingAndSavedGoal() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("candidate")=="verify")
        open()
        checkNotNull(ui().wait(Until.findObject(By.pkg(pkg).desc("Pengaturan")),30000)).click()
        find("Target waktu layar")
        assertTrue("Saved private settings did not survive",ui().hasObject(By.pkg(pkg).text("5j 47m")))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("1.4.0",context.packageManager.getPackageInfo(pkg,0).versionName)
    }
}
