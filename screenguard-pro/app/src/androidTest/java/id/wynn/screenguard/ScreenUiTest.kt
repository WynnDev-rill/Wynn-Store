package id.wynn.screenguard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
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
        compose.setContent { GuardTheme { Surface(Modifier.fillMaxSize().systemBarsPadding(), color = MaterialTheme.colorScheme.background) { block() } } }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile!!.mkdirs()
        compose.onAllNodes(isRoot()).onLast().captureToImage()
        val instrument = InstrumentationRegistry.getInstrumentation()
        instrument.waitForIdleSync()
        EmulatorUiRecovery.dismissLauncherDialog(androidx.test.uiautomator.UiDevice.getInstance(instrument))
        androidx.test.uiautomator.UiDevice.getInstance(instrument).waitForIdle()
        androidx.test.uiautomator.UiDevice.getInstance(instrument).takeScreenshot(file)
    }
    @Test fun launcherUsesTheNativeAndroidAdaptiveIcon() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assert.assertEquals(R.mipmap.screenguard_launcher, context.applicationInfo.icon)
        org.junit.Assert.assertTrue(context.packageManager.getApplicationIcon(context.packageName) is android.graphics.drawable.AdaptiveIconDrawable)
    }
    @Test fun dashboardContainsLiveNavigationAndWeeklyChart() {
        var page: Page? = null
        content { DashboardScreen(state, data, true, { page = it }, {}, {}) }
        compose.onNodeWithText("ScreenGuard Pro").assertIsDisplayed()
        compose.onNodeWithText("Tren Mingguan").assertIsDisplayed()
        capture("01-dashboard")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Blokir Internet"))
        compose.onNodeWithText("Blokir Internet").performClick()
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
    @Test fun freezeOnlyShowsSelectedAppsAndPlusAddsNewApps() {
        val chosen = apps[1]
        val manual = state.copy(rules = mapOf(chosen.packageName to AppRule(
            chosen.packageName, freeze = FreezeMode.DISABLE, gate = AccessGate(), ownedDisable = true)))
        var added: InstalledApp? = null
        var opened: InstalledApp? = null
        content { FreezeScreen(manual, data, true, false,
            { added = it }, { opened = it }, {}, { _, _ -> }, {}) }
        compose.onNodeWithText("TikTok").assertIsDisplayed()
        compose.onNodeWithText("YouTube").assertDoesNotExist()
        compose.onNodeWithContentDescription("Kembali").assertDoesNotExist()
        compose.onNodeWithText("TikTok").performClick()
        org.junit.Assert.assertEquals(chosen.packageName, opened?.packageName)
        compose.onNodeWithContentDescription("Tambah aplikasi").performClick()
        compose.onNodeWithText("YouTube").assertExists()
        compose.onNodeWithText("YouTube").performClick()
        org.junit.Assert.assertEquals(apps[0].packageName, added?.packageName)
        capture("04-freeze")
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
