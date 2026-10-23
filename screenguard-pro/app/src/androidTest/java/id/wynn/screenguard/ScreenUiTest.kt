package id.wynn.screenguard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.wynn.screenguard.data.UsageDay
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuStatus
import id.wynn.screenguard.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/** Deterministic fixtures are confined to androidTest; production never injects sample usage. */
@RunWith(AndroidJUnit4::class)
class ScreenUiTest {
    @get:Rule val compose = createComposeRule()
    private val apps = listOf("YouTube", "TikTok", "Instagram", "Chrome", "WhatsApp").mapIndexed { i, label -> InstalledApp("test.fixture.app$i", label, 10000 + i, if (i == 1) 4 else 2, true, false, false, false) }
    private val usage = listOf(88, 72, 52, 36, 28).mapIndexed { i, m -> apps[i].packageName to m * 60_000L }.toMap()
    private val data = DeviceData(apps, (6 downTo 0).map { d -> UsageDay(LocalDate.now().minusDays(d.toLong()), usage.mapValues { (_, v) -> v * (7 - d).toLong() / 7 }) }, true, false)
    private val state = GuardState(GuardSettings(onboardingDone = true), apps.associate { it.packageName to AppRule(it.packageName, dailyLimitMinutes = 60, windows = listOf(TimeWindow())) })
    private fun content(block: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { GuardTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { block() } } }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun dashboardContainsLiveNavigationAndWeeklyChart() {
        var page: Page? = null
        content { DashboardScreen(state, data, true, { page = it }, {}, {}) }
        compose.onNodeWithText("ScreenGuard Pro").assertIsDisplayed()
        compose.onNodeWithText("Tren Mingguan").assertIsDisplayed()
        capture("01-dashboard")
        compose.onNodeWithText("Blokir Internet").performScrollTo().performClick()
        org.junit.Assert.assertEquals(Page.NETWORK, page)
    }
    @Test fun usageRangeAndSearchReallyFilter() {
        content { UsageScreen(data, {}) {} }
        compose.onNodeWithText("7 hari").performClick()
        compose.onNodeWithText("Cari aplikasi").performTextInput("TikTok")
        compose.onNode(hasText("TikTok") and !hasSetTextAction()).assertExists()
        compose.onNodeWithText("YouTube").assertDoesNotExist()
        capture("02-usage")
    }
    @Test fun networkCannotPretendToWorkWithoutShizuku() {
        content { NetworkScreen(state, data, ShizukuStatus.STOPPED, false, false, {}, { _, _ -> error("Must be disabled") }, {}) }
        compose.onNodeWithContentDescription("Blokir internet YouTube").assertIsNotEnabled()
        capture("03-network")
    }
    @Test fun freezeSearchAndDownloadSystemFiltersReallyWork() {
        val filtered = data.copy(apps = apps.mapIndexed { i, app -> app.copy(system = i == 0) })
        var selected: InstalledApp? = null
        content { FreezeScreen(state, filtered, true, false, {}, {}, { selected = it }) }
        capture("04-freeze")
        compose.onNodeWithText("Unduhan").performClick()
        compose.onNodeWithText("YouTube").assertDoesNotExist()
        compose.onNodeWithText("TikTok").performClick()
        org.junit.Assert.assertEquals(apps[1].packageName, selected?.packageName)
        compose.onNodeWithText("Sistem").performClick()
        compose.onNodeWithText("YouTube").assertExists()
        compose.onNodeWithText("TikTok").assertDoesNotExist()
    }
    @Test fun waitChallengeCannotUnlockEarly() {
        content { UnlockScreen(apps[0], AccessGate(mode = GateMode.WAIT), MathChallenge(17, 8, 2), 0, 10, null, false, {}, {}, {}) }
        compose.onNodeWithText("Buka 5 menit").assertIsNotEnabled()
        capture("05-unlock")
    }
    @Test fun combinedChallengeShowsMathOnlyAfterWaiting() {
        val remaining = androidx.compose.runtime.mutableIntStateOf(5)
        content { UnlockScreen(apps[0], AccessGate(), MathChallenge(17, 8, 2), 1, remaining.intValue, null, false, {}, {}, {}) }
        compose.onNodeWithText("Masukkan jawaban").assertDoesNotExist()
        compose.runOnIdle { remaining.intValue = 0 }
        compose.onNodeWithText("Selesaikan soal 2 dari 2.").assertExists()
        compose.onNodeWithText("Masukkan jawaban").assertExists()
        capture("06-combined-math")
    }
    @Test fun weeklyChartChangesTheSelectedDayAndAppDurations() {
        content { DashboardScreen(state, data, true, {}, {}, {}) }
        val date = data.days.first().date
        compose.onNodeWithContentDescription("$date: ${duration(data.days.first().totalMs)}").performClick().assertIsSelected()
        compose.onNodeWithText("Penggunaan ${dateLabel(date)}").assertExists()
    }
    @Test fun applicationStatisticsExposeRealDetailFieldsAndSelectedDay() {
        content { AppStatsScreen(apps[0], data, {}, { _, date -> id.wynn.screenguard.data.AppDayDetail(date, List(24) { if (it == 12) 600_000L else 0 }, 3, 600_000, true) }, {}) }
        compose.onNodeWithText("Statistik aplikasi").assertExists()
        compose.onNodeWithText("Penggunaan per jam").assertExists()
        compose.onNodeWithText("3 kali").assertExists()
        capture("08-app-statistics")
    }
    @Test fun gateEditorWrapsChoicesAndAllowsAllThreeModes() {
        content { GateEditor(apps[0], AppRule(apps[0].packageName), true, {}, {}, {}, {}, {}) }
        compose.onNodeWithText("Tunggu").performClick()
        compose.onNodeWithText("Jumlah soal").assertDoesNotExist()
        compose.onNodeWithText("Matematika").performClick()
        compose.onNodeWithText("Jumlah soal").assertExists()
        compose.onNodeWithText("Kombinasi").performClick()
        capture("09-gate-editor")
    }
    @Test fun settingsHasSecurityAndRecovery() {
        content { SettingsScreen(state, ShizukuStatus.READY, {}, { it() }, {}, {}, {}, {}, {}) }
        compose.onNodeWithText("PIN Orang Tua").assertIsDisplayed()
        compose.onNodeWithText("Pulihkan semua aplikasi").assertExists()
        capture("07-settings")
    }
}
