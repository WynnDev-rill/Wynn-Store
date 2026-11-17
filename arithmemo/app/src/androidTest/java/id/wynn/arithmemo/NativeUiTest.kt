package id.wynn.arithmemo

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
        File(instrumentation.targetContext.filesDir, "qa-$name.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
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
