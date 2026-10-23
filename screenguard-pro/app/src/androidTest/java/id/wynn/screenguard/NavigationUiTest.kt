package id.wynn.screenguard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import id.wynn.screenguard.ui.*
import id.wynn.screenguard.privilege.ShizukuStatus
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import androidx.test.uiautomator.UiDevice
import id.wynn.screenguard.update.*

class NavigationUiTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrument = InstrumentationRegistry.getInstrumentation()
        val file = File(instrument.targetContext.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile!!.mkdirs()
        // Synchronize the rendered Compose layer before capturing the actual Android window.
        compose.onAllNodes(isRoot()).onLast().captureToImage()
        instrument.waitForIdleSync()
        UiDevice.getInstance(instrument).waitForIdle()
        UiDevice.getInstance(instrument).takeScreenshot(file)
    }
    @Test fun dockPersistsAndRestoresSearchAcrossTabs() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GuardApplication
        runBlocking { app.store.settings { it.copy(onboardingDone = true, pinHash = "", pinSalt = "") } }
        val vm = GuardViewModel(app)
        compose.setContent { ScreenGuardApp(vm) }
        capture("10-floating-dock-home")
        compose.onNodeWithContentDescription("Bekukan").performClick()
        capture("11-floating-dock-usage")
        compose.onNodeWithText("Cari aplikasi").performTextInput("my saved search")
        compose.onNodeWithContentDescription("Jaringan").performClick()
        compose.onNodeWithText("Kontrol Internet").assertIsDisplayed()
        compose.onNodeWithContentDescription("Bekukan").performClick()
        compose.onNodeWithText("my saved search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Beranda").performClick()
        compose.onNodeWithText("ScreenGuard Pro").assertIsDisplayed()
    }
    @Test fun restrictedSettingsHelpExplainsActualSystemAction() {
        compose.setContent { GuardTheme { PermissionsScreen(false, ShizukuStatus.STOPPED, {}, {}, {}, {}) } }
        compose.onNodeWithText("Lihat cara mengizinkan akses").performScrollTo().performClick()
        compose.onNodeWithText("Buka info ScreenGuard Pro").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Panduan resmi Android").assertExists()
        capture("12-restricted-settings-help")
    }
    @Test fun dockSupportsLargeTextAndSelectedSemantics() {
        var selected: Page? = null
        compose.setContent {
            val density = LocalDensity.current
            GuardTheme(dark = true) {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    Box(Modifier.width(320.dp)) { FloatingDock(Page.NETWORK) { selected = it } }
                }
            }
        }
        compose.onNodeWithContentDescription("Jaringan").assertIsSelected()
        compose.onNodeWithContentDescription("Pengaturan").performClick()
        assertEquals(Page.SETTINGS, selected)
        capture("13-dock-large-text-dark")
    }
    @Test fun updateSheetShowsActualProgressAndCancellation() {
        val release = UpdateManifest(3, "1.2.0", "id.wynn.screenguard", 30,
            "https://github.com/WynnDev-rill/Wynn-Store/releases/download/screenguard-v1.2.0/ScreenGuard-Pro.apk", "a".repeat(64), "b".repeat(64), 3_000_000,
            "Peningkatan stabilitas dan pengalaman fokus yang lebih halus.")
        var cancelled = false
        compose.setContent { GuardTheme { UpdateSheet(AppUpdater.State(available = release, progress = .5f, message = "Mengunduh update…"), {}, {}, {}, { cancelled = true }) } }
        compose.onNodeWithText("50%").assertIsDisplayed()
        capture("14-update-sheet")
        compose.onNodeWithText("Batalkan unduhan").performClick()
        assertTrue(cancelled)
    }

}
