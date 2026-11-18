package id.wynn.vocamemo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseSmokeTest {
    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun node(selector: BySelector): UiObject2 =
        checkNotNull(device.wait(Until.findObject(selector), 20000)) {
            "Visible control: $selector"
        }

    // Invoked by the device script against the installed 1.0.0 compatibility fixture.
    @Test
    fun seedLegacyMemo() {
        if (InstrumentationRegistry.getArguments().getString("upgradePhase") != "seed") return
        lookupAndSave()
        node(By.text("Latihan")).click()
        node(By.text("Mulai pengulangan adaptif")).click()
        node(By.text("Tampilkan jawaban")).click()
        node(By.text("Ingat")).click()
        node(By.text("Sesi selesai."))
    }

    @Test
    fun productionLookupAndRetainedMemoOffline() {
        node(By.text("Semua Kata"))
        openTab("Memo", "Memo Anda")
        node(By.text("apel"))
        openTab("Kamus", "Semua Kata")
        val input = node(By.clazz("android.widget.EditText"))
        input.text = "light"
        node(By.clazz("android.widget.TextView").text("light")).click()
        node(By.desc("Dengarkan pengucapan")).click()
        node(By.text("kata sifat"))
    }

    private fun openTab(label: String, heading: String) {
        val selector = By.text(heading).pkg("id.wynn.vocamemo")
        repeat(3) {
            if (device.hasObject(selector)) return
            val labelNode = node(By.text(label).pkg("id.wynn.vocamemo"))
            val target = generateSequence(labelNode) { it.parent }.firstOrNull { it.isClickable }
            (target ?: labelNode).click()
            if (device.wait(Until.hasObject(selector), 4000)) return
        }
        node(selector)
    }

    private fun lookupAndSave() {
        node(By.clazz("android.widget.EditText")).text = "apple"
        node(By.clazz("android.widget.TextView").text("apple")).click()
        node(By.text("Simpan")).click()
        node(By.text("Memo")).click()
        assertTrue(device.wait(Until.hasObject(By.text("apel")), 15000))
    }
}
