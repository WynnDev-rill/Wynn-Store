package id.wynn.arithmemo

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.wynn.arithmemo.data.MemoStore
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun ready() {
        rule.waitUntil(20000) {
            rule.onAllNodesWithText("Sedikit setiap hari.").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        saveDeviceCapture(name)
    }

    private fun shell(command: String) {
        val result =
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(result).use { it.readBytes() }
    }

    @Test
    fun visualVariantsStayUsable() {
        ready()
        try {
            shell("cmd uimode night yes")
            shell("settings put system font_scale 1.6")
            rule.waitUntil(15000) {
                runCatching {
                        val c = rule.activity.resources.configuration
                        c.fontScale > 1.5f &&
                            c.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                                Configuration.UI_MODE_NIGHT_YES
                    }
                    .getOrDefault(false)
            }
            rule.onNodeWithText("Setelan").assertExists()
            capture("dark-large")
            shell("settings put system font_scale 1.0")
            shell("cmd uimode night no")
            rule.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            rule.waitUntil(15000) {
                runCatching {
                        rule.activity.resources.configuration.orientation ==
                            Configuration.ORIENTATION_LANDSCAPE
                    }
                    .getOrDefault(false)
            }
            rule.onNodeWithText("Setelan").assertExists()
            capture("landscape")
        } finally {
            shell("settings put system font_scale 1.0")
            shell("cmd uimode night no")
            rule.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            rule.waitUntil(15000) {
                runCatching {
                        val c = rule.activity.resources.configuration
                        c.orientation == Configuration.ORIENTATION_PORTRAIT && c.fontScale < 1.1f
                    }
                    .getOrDefault(false)
            }
            rule.waitForIdle()
        }
    }

    @Test
    fun navigationFiltersAndFlashcardReveal() {
        ready()
        capture("home")
        rule.onNodeWithText("Hafalan").performClick()
        rule.onNodeWithText("Ruang hafalan").assertIsDisplayed()
        rule.onNodeWithText("Atur pilihan · 1 digit").performClick()
        rule.onNodeWithTag("factList").performScrollToNode(hasText("2 digit"))
        rule.onNodeWithText("2 digit").performClick()
        rule.onNodeWithText("1 digit").performClick()
        rule.onNodeWithTag("factList").performScrollToNode(hasText("Daftar"))
        rule.onNodeWithText("Daftar").performClick()
        rule.onNodeWithTag("factList").assertExists()
        rule.onNodeWithTag("factList").performScrollToNode(hasText("Flashcard"))
        rule.onNodeWithText("Flashcard", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("factList").performScrollToNode(hasText("Balik kartu"))
        rule.onNodeWithText("Balik kartu").performClick()
        rule.onNodeWithTag("revealedAnswer").assertExists()
        capture("flashcard")
        rule.onNodeWithText("Statistik").performClick()
        rule.onNodeWithText("Perkembangan Anda").assertIsDisplayed()
        capture("stats")
        rule.onNodeWithText("Setelan").performClick()
        rule.onNodeWithText("Target harian").assertExists()
        capture("settings")
    }

    @Test
    fun recallAnswerPersistsAndShowsFeedback() {
        ready()
        rule.onNodeWithText("Latihan mengingat").performScrollTo().performClick()
        rule.waitUntil(10000) {
            rule.onAllNodesWithTag("question").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(10000) {
            rule
                .onAllNodes(hasContentDescription("Angka 1") and isEnabled())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        capture("recall")
        val text =
            rule
                .onNodeWithTag("question")
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString("") { it.text }
        val parts = text.split(" ")
        val a = parts[0].toInt()
        val b = parts[2].toInt()
        val answer =
            when (parts[1]) {
                "+" -> a + b
                "−" -> a - b
                "×" -> a * b
                else -> a / b
            }
        for (c in answer.toString()) rule
            .onNodeWithContentDescription("Angka $c")
            .performScrollTo()
            .performClick()
        rule.onNodeWithContentDescription("Periksa jawaban").performScrollTo().performClick()
        rule.waitUntil(10000) {
            rule.onAllNodesWithText("Benar, sudah tersimpan.").fetchSemanticsNodes().isNotEmpty()
        }
        capture("feedback")
        rule.onNodeWithText("Lanjut").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Jeda sesi").performClick()
        rule.onNodeWithText("Lanjutkan sesi").assertExists()
    }
}

internal fun saveDeviceCapture(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    // Compose idleness can precede SurfaceFlinger presenting the new window, especially on API 26.
    instrumentation.waitForIdleSync()
    android.os.SystemClock.sleep(1000)
    val image = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
    File(instrumentation.targetContext.filesDir, "qa-$name.png").outputStream().use {
        check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
    }
}

@RunWith(AndroidJUnit4::class)
class RestorationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completedAnswerAndSessionSurviveForceStop() {
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("Lanjutkan sesi").fetchSemanticsNodes().isNotEmpty()
        }
        val store = MemoStore(rule.activity)
        org.junit.Assert.assertEquals(1, store.read().attempts.size)
        org.junit.Assert.assertEquals(1, store.read().session?.answered)
        store.close()
        rule.onNodeWithText("Lanjutkan sesi").assertExists()
        rule.waitForIdle()
        saveDeviceCapture("relaunch")
    }
}
