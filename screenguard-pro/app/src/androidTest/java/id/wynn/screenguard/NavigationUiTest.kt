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

class NavigationUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun dockPersistsAndRestoresSearchAcrossTabs() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GuardApplication
        runBlocking { app.store.settings { it.copy(onboardingDone = true, pinHash = "", pinSalt = "") } }
        val vm = GuardViewModel(app)
        compose.setContent { ScreenGuardApp(vm) }
        compose.onNodeWithContentDescription("Aplikasi").performClick()
        compose.onNodeWithText("Cari aplikasi").performTextInput("my saved search")
        compose.onNodeWithContentDescription("Internet").performClick()
        compose.onNodeWithText("Kontrol Internet").assertIsDisplayed()
        compose.onNodeWithContentDescription("Aplikasi").performClick()
        compose.onNodeWithText("my saved search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Beranda").performClick()
        compose.onNodeWithText("ScreenGuard Pro").assertIsDisplayed()
    }
    @Test fun restrictedSettingsHelpExplainsActualSystemAction() {
        compose.setContent { GuardTheme { PermissionsScreen(false, ShizukuStatus.STOPPED, {}, {}, {}, {}) } }
        compose.onNodeWithText("Lihat cara mengizinkan akses").performScrollTo().performClick()
        compose.onNodeWithText("Buka info ScreenGuard Pro").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Panduan resmi Android").assertExists()
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
        compose.onNodeWithContentDescription("Internet").assertIsSelected()
        compose.onNodeWithContentDescription("Pengaturan").performClick()
        assertEquals(Page.SETTINGS, selected)
    }
}
