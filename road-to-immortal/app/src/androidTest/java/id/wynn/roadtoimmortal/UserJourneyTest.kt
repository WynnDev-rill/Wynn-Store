package id.wynn.roadtoimmortal

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Installed app black-box QA via accessibility tree and device shell. */
@RunWith(AndroidJUnit4::class)
class UserJourneyTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext
    private val pkg = "id.wynn.roadtoimmortal"

    private fun tap(text: String) {
        val n = device.wait(Until.findObject(By.text(text)), 12_000)
        assertNotNull("Missing text: $text", n)
        n!!.click()
        device.waitForIdle()
    }

    private fun desc(text: String) {
        val n = device.wait(Until.findObject(By.desc(text)), 12_000)
        assertNotNull("Missing description: $text", n)
        n!!.click()
        device.waitForIdle()
    }

    private fun shot(name: String) {
        device.waitForIdle()
        val dir = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        assertTrue(device.takeScreenshot(File(dir, "$name.png")))
        device.dumpWindowHierarchy(File(dir, "$name.xml"))
    }

    private fun back() {
        device.pressBack()
        device.waitForIdle()
    }

    private fun search(s: String) {
        device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5000)!!.text = s
        device.waitForIdle()
    }

    @Test
    fun completeJourneyAndAdversarialStates() {
        val intent =
            context.packageManager
                .getLaunchIntentForPackage(pkg)!!
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        assertTrue(device.wait(Until.hasObject(By.text("Setiap bintang berarti.")), 20_000))
        shot("01-onboarding")
        tap("Mulai perjalanan")
        tap("Mythic")
        repeat(5) { desc("Tambah satu bintang") }
        shot("02-rank-picker")
        tap("Selesai")
        assertTrue(device.wait(Until.hasObject(By.text("5 ★")), 5000))
        assertTrue(device.hasObject(By.textContains("bintang lagi")))
        shot("03-road-light")
        tap("Jelajah")
        search("Miya")
        tap("Miya")
        shot("04-hero-build")
        tap("Skill")
        val skill = device.wait(Until.findObject(By.desc("Baca skill")), 5000)
        assertNotNull(skill)
        skill!!.click()
        shot("05-hero-skill")
        tap("Matchup")
        assertTrue(device.wait(Until.hasObject(By.text("Waspadai")), 5000))
        shot("06-matchup")
        back()
        tap("Item")
        search("Antique")
        tap("Antique Cuirass")
        assertTrue(device.wait(Until.hasObject(By.text("Deter")), 5000))
        shot("07-item")
        back()
        tap("Meta")
        shot("08-meta")
        tap("Mythic")
        tap("Legend")
        tap("Ban rate")
        shot("09-meta-ban")
        tap("Draft")
        desc("Tim kamu, pilih hero slot 1")
        search("Miya")
        tap("Miya")
        desc("Tim lawan, pilih hero slot 1")
        search("Miya")
        assertTrue(device.wait(Until.hasObject(By.text("Hero tidak ditemukan")), 5000))
        search("Eudora")
        tap("Eudora")
        shot("10-draft")
        tap("Jelajah")
        tap("Draft")
        assertTrue(device.wait(Until.hasObject(By.desc("Tim kamu, hapus Miya")), 5000))
        tap("Perjalanan")
        desc("Pengaturan")
        tap("Gelap")
        back()
        shot("11-road-dark")
        device.executeShellCommand("settings put system font_scale 1.5")
        device.waitForIdle()
        shot("12-large-text")
        device.executeShellCommand("settings put system font_scale 1.0")
        device.setOrientationLeft()
        device.waitForIdle()
        shot("13-landscape")
        device.setOrientationNatural()
        device.unfreezeRotation()
        device.executeShellCommand("svc wifi disable")
        device.executeShellCommand("svc data disable")
        desc("Pengaturan")
        tap("Data & sumber")
        tap("Perbarui sekarang")
        device.wait(Until.hasObject(By.textStartsWith("Belum bisa memperbarui")), 100_000)
        shot("14-offline")
        back()
        back()
        tap("Jelajah")
        tap("Hero")
        search("")
        assertTrue(
            device.wait(
                Until.hasObject(By.text(java.util.regex.Pattern.compile("[0-9]+ hero"))),
                5000,
            )
        )
        device.executeShellCommand("svc wifi enable")
        device.executeShellCommand("svc data enable")
        File(context.getExternalFilesDir(null), "qa/gfxinfo.txt")
            .writeText(device.executeShellCommand("dumpsys gfxinfo $pkg"))
        File(context.getExternalFilesDir(null), "qa/meminfo.txt")
            .writeText(device.executeShellCommand("dumpsys meminfo $pkg"))
    }
}
