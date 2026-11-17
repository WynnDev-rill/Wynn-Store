package id.wynn.vocamemo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        rule.onRoot().assertExists()
        rule.waitForIdle()
        Thread.sleep(350)
        val c = rule.activity
        val file = File(c.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile!!.mkdirs()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(file)
    }

    @Test
    fun speedExpirySurvivesPendingGradeAndRotation() {
        val vm = androidx.lifecycle.ViewModelProvider(rule.activity)[VocaModel::class.java]
        runBlocking { vm.store.dao.insert(Memo(word = "clock", meaning = "jam")) }
        rule.waitUntil(10000) { vm.memos.value.any { it.word == "clock" } }
        rule.runOnUiThread {
            vm.start("Tes kecepatan", false, "Semua")
            vm.grade(3)
            repeat(30) { vm.tick(2000) }
        }
        rule.waitUntil(10000) { !vm.busy }
        rule.runOnUiThread { org.junit.Assert.assertTrue(vm.practice!!.finished) }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Sesi selesai.").assertExists()
        rule.runOnUiThread { vm.end() }
    }

    @Test
    fun studyModesAndDirections() {
        val vm = androidx.lifecycle.ViewModelProvider(rule.activity)[VocaModel::class.java]
        runBlocking {
            vm.store.dao.insert(Memo(word = "remember", meaning = "ingat", groupName = "QA modes"))
        }
        rule.waitUntil(10000) { vm.memos.value.any { it.groupName == "QA modes" } }
        val original = vm.memos.value.first { it.groupName == "QA modes" }
        rule.runOnUiThread {
            vm.start("Mengetik", true, "QA modes")
            org.junit.Assert.assertEquals("ingat", vm.practice!!.prompt)
            vm.type(" REMEMBER ")
            vm.checkAnswer()
            org.junit.Assert.assertTrue(vm.practice!!.correct)
            vm.grade(3)
        }
        rule.waitUntil(10000) { !vm.busy }
        rule.runOnUiThread { vm.save(original.copy(favourite = true)) }
        rule.waitUntil(10000) { vm.memos.value.first { it.id == original.id }.favourite }
        runBlocking {
            org.junit.Assert.assertEquals(1, vm.store.dao.memo(original.id)!!.repetitions)
        }
        rule.runOnUiThread {
            org.junit.Assert.assertEquals(1, vm.practice!!.reviewed)
            vm.start("Ulang mekanis", false, "QA modes")
            repeat(100) {
                vm.reveal()
                vm.nextUnscored()
            }
            org.junit.Assert.assertEquals(0, vm.practice!!.reviewed)
            org.junit.Assert.assertFalse(vm.practice!!.finished)
            vm.start("Daftar", true, "QA modes")
            org.junit.Assert.assertEquals("ingat", vm.practice!!.prompt)
            vm.start("Flashcard", false, "QA modes")
            vm.reveal()
            org.junit.Assert.assertEquals("ingat", vm.practice!!.answer)
            vm.end()
        }
    }

    @Test
    fun darkThemeLargeTextKeepsSearchUsable() {
        val vm = androidx.lifecycle.ViewModelProvider(rule.activity)[VocaModel::class.java]
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            rule.runOnUiThread {
                vm.end()
                vm.closeWord()
                vm.page("Kamus")
                vm.theme("Gelap")
            }
            device.executeShellCommand("settings put system font_scale 1.5")
            Thread.sleep(700)
            rule.activityRule.scenario.recreate()
            rule.waitForIdle()
            rule.onNodeWithTag("search").assertIsDisplayed()
            capture("08-dark-large-text")
        } finally {
            device.executeShellCommand("settings put system font_scale 1.0")
            rule.runOnUiThread { vm.theme("Sistem") }
        }
    }

    @Test
    fun lookupSavePracticeAndStatsOffline() {
        val vm = androidx.lifecycle.ViewModelProvider(rule.activity)[VocaModel::class.java]
        runBlocking { vm.store.dao.allMemos().forEach { vm.store.dao.delete(it.id) } }
        rule.runOnUiThread {
            vm.end()
            vm.page("Kamus")
            vm.closeWord()
            vm.search("")
        }
        capture("01-kamus")
        rule.onNodeWithTag("search").performTextInput("apple")
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("apple", useUnmergedTree = true).fetchSemanticsNodes().size >= 2
        }
        rule.onAllNodesWithText("apple").onLast().performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("Simpan").fetchSemanticsNodes().isNotEmpty()
        }
        capture("02-senses")
        rule.onAllNodesWithText("Simpan").onFirst().performClick()
        rule.onNodeWithText("Memo", useUnmergedTree = true).performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("apel").fetchSemanticsNodes().isNotEmpty() }
        capture("03-memo")
        rule.onNodeWithText("Latihan", useUnmergedTree = true).performClick()
        capture("04-latihan")
        rule.onNodeWithText("Mulai pengulangan adaptif").performClick()
        rule.onNodeWithText("Tampilkan jawaban").assertExists().performClick()
        rule.onNodeWithText("Ingat", substring = false).performClick()
        rule.waitUntil(10000) {
            rule.onAllNodesWithText("Sesi selesai.").fetchSemanticsNodes().isNotEmpty()
        }
        capture("05-complete")
        rule.onNodeWithText("Kembali ke Latihan").performClick()
        rule.onNodeWithText("Statistik", useUnmergedTree = true).performClick()
        capture("06-statistik")
        rule.onNodeWithText("Jawaban sepanjang waktu").assertExists()
        rule.onNodeWithContentDescription("Buka setelan").performClick()
        capture("07-settings")
    }
}
