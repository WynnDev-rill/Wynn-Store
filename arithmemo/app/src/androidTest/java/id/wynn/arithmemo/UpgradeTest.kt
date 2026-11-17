package id.wynn.arithmemo

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import id.wynn.arithmemo.core.*
import id.wynn.arithmemo.data.MemoStore
import id.wynn.arithmemo.update.*
import java.util.regex.Pattern
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs only with an explicitly supplied public release manifest against a signed lower-version QA
 * baseline.
 */
@RunWith(AndroidJUnit4::class)
class UpgradeTest {
    @Test
    fun verifiedDownloadOpensAndroidInstallerAndRetainsStudyData() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val encoded = args.getString("updateManifest")
        assumeTrue("Production upgrade fixture not requested", encoded != null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("id.wynn.arithmemo", context.packageName)
        val manifest =
            Json.decodeFromString<UpdateManifest>(
                String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
            )
        manifest.validate()
        assertTrue(manifest.versionCode > BuildConfig.VERSION_CODE)
        val store = MemoStore(context)
        val fact = Fact(7, Operation.MULTIPLY, 8)
        val session =
            StudySession(
                mode = StudyMode.RECALL,
                cards = listOf(fact, fact),
                index = 1,
                answered = 1,
                correct = 1,
            )
        store.record(
            Attempt("upgrade-proof", fact, true, 1200, Rating.GOOD, System.currentTimeMillis()),
            session,
        )
        assertEquals(1, store.read().attempts.size)
        store.close()
        val updater = AppUpdater(context)
        updater.state.value = AppUpdater.State(available = manifest)
        updater.download()
        assertTrue(updater.state.value.message, updater.state.value.ready)
        val intent = updater.install()
        assertNotNull(updater.state.value.message, intent)
        assertEquals(Intent.ACTION_VIEW, intent!!.action)
        assertEquals("content", intent.data?.scheme)
        // Keep a foreground activity and the instrumentation alive until Android has parsed the
        // URI.
        val activity = ActivityScenario.launch(MainActivity::class.java)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(
            "Baseline activity did not become visible",
            device.wait(Until.hasObject(By.text("Sedikit setiap hari.")), 15000),
        )
        activity.onActivity { it.startActivity(intent) }
        assertTrue(
            "Android installer did not parse the verified APK",
            device.wait(Until.hasObject(By.text(Pattern.compile("(?i)update|install"))), 15000),
        )
    }
}
