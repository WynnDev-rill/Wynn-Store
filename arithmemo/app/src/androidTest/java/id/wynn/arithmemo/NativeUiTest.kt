package id.wynn.arithmemo

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Test
    fun navigationFiltersAndFlashcardReveal() {
        ready()
        rule.onNodeWithText("Hafalan").performClick()
        rule.onNodeWithText("Ruang hafalan").assertIsDisplayed()
        rule.onNodeWithText("Daftar").performClick()
        rule.onNodeWithTag("factList").assertExists()
        rule.onNodeWithText("Flashcard", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Balik kartu").performScrollTo().performClick()
        rule.onNodeWithTag("revealedAnswer").assertExists()
        rule.onNodeWithText("Statistik").performClick()
        rule.onNodeWithText("Perkembangan Anda").assertIsDisplayed()
        rule.onNodeWithText("Pengaturan").performClick()
        rule.onNodeWithText("Target harian").assertExists()
    }

    @Test
    fun recallAnswerPersistsAndShowsFeedback() {
        ready()
        rule.onNodeWithText("Latihan mengingat").performScrollTo().performClick()
        rule.waitUntil(10000) {
            rule.onAllNodesWithTag("question").fetchSemanticsNodes().isNotEmpty()
        }
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
        rule.onNodeWithText("Lanjut").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Jeda sesi").performClick()
        rule.onNodeWithText("Lanjutkan sesi").assertExists()
    }
}
