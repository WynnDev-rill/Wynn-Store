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
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
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
        compose.onNodeWithText("TikTok").assertExists()
        compose.onNodeWithText("YouTube").assertDoesNotExist()
        capture("02-usage")
    }
    @Test fun networkCannotPretendToWorkWithoutShizuku() {
        content { NetworkScreen(state, data, ShizukuStatus.STOPPED, false, false, {}, { _, _ -> error("Must be disabled") }, {}) }
        compose.onNodeWithContentDescription("Blokir internet YouTube").assertIsNotEnabled()
        capture("03-network")
    }
    @Test fun freezeHasRealModeChoice() {
        content { FreezeScreen(state, data, true, false, {}, { _, _ -> }, {}) }
        capture("04-freeze")
        compose.onAllNodesWithText("Bekukan").onFirst().performClick()
        compose.onNodeWithText("Tangguhkan").assertIsDisplayed()
        compose.onNodeWithText("Nonaktifkan").assertIsDisplayed()
    }
    @Test fun waitChallengeCannotUnlockEarly() {
        content { UnlockScreen(apps[0], GuardSettings(unlockMode = UnlockMode.WAIT), MathChallenge(17, 8, 2), 10, null, false, {}, {}, {}) }
        compose.onAllNodesWithText("Buka Aplikasi").filterToOne(hasClickAction()).assertIsNotEnabled()
        capture("05-unlock")
    }
    @Test fun scheduleShowsSavedLimitsAndMonitoring() {
        content { ScheduleScreen(state, apps, {}, {}, {}, {}, {}) }
        compose.onNodeWithText("Pemantauan otomatis").assertIsDisplayed()
        compose.onNodeWithText("YouTube").assertExists()
        capture("06-schedule")
    }
    @Test fun settingsHasSecurityAndRecovery() {
        content { SettingsScreen(state, ShizukuStatus.READY, {}, { it() }, {}, {}, {}, {}, {}) }
        compose.onNodeWithText("PIN Orang Tua").assertIsDisplayed()
        compose.onNodeWithText("Pulihkan semua aplikasi").assertExists()
        capture("07-settings")
    }
}
