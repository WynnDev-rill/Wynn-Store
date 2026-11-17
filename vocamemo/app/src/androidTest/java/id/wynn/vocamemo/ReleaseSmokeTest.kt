package id.wynn.vocamemo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseSmokeTest {
    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun productionLookupMemoAndTtsOffline() {
        val d = device
        d.executeShellCommand("am force-stop id.wynn.vocamemo")
        d.executeShellCommand("am start -n id.wynn.vocamemo/.MainActivity")
        val input = d.wait(Until.findObject(By.clazz("android.widget.EditText")), 15000)
        assertNotNull("Release search control", input)
        input.text = "apple"
        assertTrue(
            d.wait(Until.hasObject(By.text("apel")), 15000) ||
                d.wait(Until.hasObject(By.textContains("arti / konteks")), 10000)
        )
        d.findObjects(By.text("apple")).last().click()
        val save = d.wait(Until.findObject(By.text("Simpan")), 15000)
        assertNotNull("Release senses", save)
        d.findObject(By.desc("Dengarkan pengucapan"))?.click()
        save.click()
        d.findObject(By.text("Memo")).click()
        assertTrue("Saved production memo", d.wait(Until.hasObject(By.text("apel")), 10000))
        d.executeShellCommand("am force-stop id.wynn.vocamemo")
        d.executeShellCommand("am start -n id.wynn.vocamemo/.MainActivity")
        assertNotNull(d.wait(Until.findObject(By.text("Memo")), 15000))
        d.findObject(By.text("Memo")).click()
        assertTrue("Memo after process restart", d.wait(Until.hasObject(By.text("apel")), 10000))
    }
}
