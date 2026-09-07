package id.wynn.roadtoimmortal

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import java.io.File
import java.util.regex.Pattern
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

/** Drives the installable, optimized consumer APK from an independent process. */
@RunWith(AndroidJUnit4::class)
class UserJourneyTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext
    private val pkg = "id.wynn.roadtoimmortal"
    private val output
        get() = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }

    @get:Rule
    val evidence =
        object : TestWatcher() {
            override fun failed(e: Throwable, description: Description) {
                shot("failure-${description.methodName}")
            }

            override fun finished(description: Description) {
                File(output, "gfxinfo-${description.methodName}.txt")
                    .writeText(device.executeShellCommand("dumpsys gfxinfo $pkg"))
                File(output, "meminfo-${description.methodName}.txt")
                    .writeText(device.executeShellCommand("dumpsys meminfo $pkg"))
                device.executeShellCommand("settings put system font_scale 1.0")
                device.executeShellCommand("svc wifi enable")
                device.executeShellCommand("svc data enable")
                device.setOrientationNatural()
                device.unfreezeRotation()
            }
        }

    @Before
    fun freshConsumerInstallState() {
        Configurator.getInstance().setWaitForIdleTimeout(1500)
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        device.executeShellCommand("svc wifi enable")
        device.executeShellCommand("svc data enable")
        awaitNetwork(true)
        // This host is isolated, so clearing the consumer APK does not kill tests.
        assertTrue(device.executeShellCommand("pm clear $pkg").contains("Success"))
        device.executeShellCommand("am start -W -n $pkg/.MainActivity")
        assertTrue(
            "Home did not become accessible",
            device.wait(Until.hasObject(By.text("Setiap bintang berarti.")), 60_000),
        )
    }

    private fun tap(text: String) {
        // Search text and a result can have the same name. Select the actual label.
        val selector =
            By.text(text).pkg(pkg).clazz(Pattern.compile("(?!android\\.widget\\.EditText$).*"))
        val n = device.wait(Until.findObject(selector), 12_000)
        assertNotNull("Missing text: $text", n)
        n!!.click()
        device.waitForIdle()
        SystemClock.sleep(180)
    }

    private fun desc(text: String) {
        val n = device.wait(Until.findObject(By.desc(text).pkg(pkg)), 12_000)
        assertNotNull("Missing description: $text", n)
        n!!.click()
        device.waitForIdle()
        SystemClock.sleep(180)
    }

    private fun shot(name: String) {
        device.waitForIdle()
        SystemClock.sleep(300)
        assertTrue(device.takeScreenshot(File(output, "$name.png")))
        device.dumpWindowHierarchy(File(output, "$name.xml"))
        android.util.Log.i("ImmortalQA", "Captured $name")
    }

    private fun back() {
        device.pressBack()
        device.waitForIdle()
        SystemClock.sleep(180)
    }

    private fun search(s: String) {
        val n = device.wait(Until.findObject(By.clazz("android.widget.EditText").pkg(pkg)), 5000)
        assertNotNull("Search field missing", n)
        n!!.text = s
        device.waitForIdle()
        SystemClock.sleep(250)
    }

    private fun reveal(text: String) {
        if (!device.hasObject(By.text(text))) {
            device
                .findObject(By.scrollable(true).pkg(pkg))
                ?.scrollUntil(Direction.DOWN, Until.findObject(By.text(text)))
        }
        assertTrue("Cannot reveal $text", device.wait(Until.hasObject(By.text(text)), 5000))
    }

    private fun awaitNetwork(available: Boolean) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val deadline = SystemClock.elapsedRealtime() + 30_000
        fun ready(): Boolean {
            val network = manager.activeNetwork
            return if (!available) network == null
            else
                manager
                    .getNetworkCapabilities(network)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
        while (!ready() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        File(output, "network-${if (available) "online" else "offline"}.txt")
            .writeText(device.executeShellCommand("dumpsys connectivity"))
        assertTrue(
            "Android did not reach the requested network state: available=$available",
            ready(),
        )
    }

    @Test
    fun roadRankThemeAndAdaptiveText() {
        shot("01-onboarding")
        tap("Mulai perjalanan")
        tap("Mythic")
        repeat(5) { desc("Tambah satu bintang") }
        assertTrue(device.wait(Until.hasObject(By.text("5 ★")), 5000))
        shot("02-rank-picker")
        tap("Selesai")
        assertTrue(device.wait(Until.hasObject(By.text("5 ★")), 5000))
        assertTrue(device.wait(Until.hasObject(By.textContains("95 bintang lagi")), 5000))
        shot("03-road-light")
        desc("Pengaturan")
        tap("Gelap")
        back()
        shot("11-road-dark")
        device.executeShellCommand("settings put system font_scale 1.5")
        SystemClock.sleep(1000)
        assertTrue(device.wait(Until.hasObject(By.text("Setiap bintang berarti.")), 5000))
        shot("12-large-text")
        device.executeShellCommand("settings put system font_scale 1.0")
        device.setOrientationLeft()
        SystemClock.sleep(1000)
        shot("13-landscape")
        device.setOrientationNatural()
        device.unfreezeRotation()
        // Values survive a process restart, without any progress history.
        device.executeShellCommand("am force-stop $pkg")
        device.executeShellCommand("am start -W -n $pkg/.MainActivity")
        assertTrue(device.wait(Until.hasObject(By.text("5 ★")), 15_000))
    }

    @Test
    fun heroItemsEmblemsSpellsAndMatchup() {
        tap("Jelajah")
        assertTrue(device.wait(Until.hasObject(By.text(Pattern.compile("[0-9]+ hero"))), 5000))
        SystemClock.sleep(1500)
        shot("04a-atlas")
        device.setOrientationLeft()
        SystemClock.sleep(800)
        search("Miya")
        // The shorter viewport must let the user scroll past the search/filter header.
        device
            .findObject(By.scrollable(true).pkg(pkg))
            ?.scrollUntil(
                Direction.DOWN,
                Until.findObject(By.text("Miya").clazz("android.widget.TextView")),
            )
        assertTrue(
            device.wait(Until.hasObject(By.text("Miya").clazz("android.widget.TextView")), 5000)
        )
        shot("04b-atlas-landscape")
        device.setOrientationNatural()
        device.unfreezeRotation()
        search("Miya")
        tap("Miya")
        reveal("Siap masuk ranked")
        assertTrue(device.wait(Until.hasObject(By.text("Siap masuk ranked")), 5000))
        shot("04-hero-build")
        tap("Skill")
        desc("Baca skill")
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
        tap("Emblem")
        SystemClock.sleep(1000)
        shot("07a-emblems")
        tap("Spell")
        SystemClock.sleep(1000)
        shot("07b-spells")
    }

    @Test
    fun draftMetaOnlineAndOfflineCatalog() {
        tap("Meta")
        shot("08-meta")
        tap("Mythic")
        tap("Legend")
        tap("Ban rate")
        shot("09-meta-ban")
        tap("Tim")
        tap("Hero kamu")
        search("Miya")
        tap("Miya")
        tap("Lawan (opsional)")
        search("Miya")
        assertTrue(device.wait(Until.hasObject(By.text("Hero tidak ditemukan")), 5000))
        search("Eudora")
        tap("Eudora")
        reveal("Pilihan 1")
        shot("10-tim-duo")
        tap("Jelajah")
        tap("Tim")
        assertTrue(device.wait(Until.hasObject(By.desc("Hapus Hero kamu")), 5000))
        tap("Perjalanan")
        desc("Pengaturan")
        tap("Data & sumber")
        // Verify a real network refresh, then exercise the same action offline.
        assertTrue(device.wait(Until.hasObject(By.text("Perbarui sekarang")), 90_000))
        tap("Perbarui sekarang")
        SystemClock.sleep(1000)
        assertTrue(device.wait(Until.hasObject(By.text("Perbarui sekarang")), 90_000))
        assertFalse(
            "Public online catalog must refresh successfully",
            device.hasObject(By.textStartsWith("Belum bisa memperbarui")),
        )
        shot("14a-online-sources")
        device.executeShellCommand("svc wifi disable")
        device.executeShellCommand("svc data disable")
        // Radio commands return before ConnectivityService finishes disconnecting.
        // Confirm there is no default network before exercising offline refresh.
        awaitNetwork(false)
        tap("Perbarui sekarang")
        assertTrue(
            "Offline refresh must retain catalog and report unavailable network",
            device.wait(Until.hasObject(By.textStartsWith("Belum bisa memperbarui")), 10_000),
        )
        shot("14-offline")
        back()
        back()
        tap("Jelajah")
        assertTrue(device.wait(Until.hasObject(By.text(Pattern.compile("[0-9]+ hero"))), 5000))
        shot("15-offline-atlas")
        device.executeShellCommand("am force-stop $pkg")
        device.executeShellCommand("am start -W -n $pkg/.MainActivity")
        tap("Jelajah")
        assertTrue(device.wait(Until.hasObject(By.text(Pattern.compile("[0-9]+ hero"))), 5000))
        shot("16-offline-restart")
        device.executeShellCommand("svc wifi enable")
        device.executeShellCommand("svc data enable")
        awaitNetwork(true)
        tap("Coba lagi")
        assertTrue(device.wait(Until.gone(By.text("Mode tersimpan · pembaruan tertunda")), 90_000))
        shot("17-online-recovery")
    }

    @Test
    fun tierPoolReorderMoveRestartAndParty() {
        tap("Tier List")
        tap("Gold")
        search("Miya")
        device.pressBack() // keyboard
        reveal("Miya")
        shot("18-tier-gold")
        desc("Rancang Miya")
        tap("Gold")
        tap("Jelajah")
        search("Layla")
        device.pressBack()
        desc("Rancang Layla")
        tap("Gold")
        tap("Perjalanan")
        reveal("Rancangan Hero")
        tap("Atur")
        tap("Gold")
        assertTrue(device.wait(Until.hasObject(By.text("Gold · 2/10 hero")), 5000))
        desc("Naikkan Layla")
        shot("19-pool-reordered")
        // First selected row now belongs to Layla; move that row to another lane.
        val move = device.findObjects(By.text("Pindah lane")).first()
        move.click()
        tap("Mid · 0/10")
        tap("Mid")
        assertTrue(device.wait(Until.hasObject(By.text("Mid · 1/10 hero")), 5000))
        shot("20-pool-moved")
        device.executeShellCommand("am force-stop $pkg")
        device.executeShellCommand("am start -W -n $pkg/.MainActivity")
        reveal("Rancangan Hero")
        tap("Atur")
        tap("Mid")
        assertTrue(device.wait(Until.hasObject(By.text("Mid · 1/10 hero")), 5000))
        desc("Hapus Layla dari Mid")
        assertTrue(device.wait(Until.hasObject(By.text("Mid · 0/10 hero")), 5000))
        back()
        tap("Tim")
        tap("Hero kamu")
        search("Miya")
        tap("Miya")
        tap("Trio")
        reveal("Pilihan 1")
        shot("21-tim-trio")
        device.findObject(By.scrollable(true).pkg(pkg))?.fling(Direction.UP)
        tap("Squad")
        reveal("Pilihan 1")
        shot("22-tim-squad")
        tap("Jelajah")
        tap("Counter")
        shot("23-counter-items")
    }
}
