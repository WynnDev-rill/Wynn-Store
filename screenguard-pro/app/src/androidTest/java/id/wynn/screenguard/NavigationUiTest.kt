package id.wynn.screenguard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import id.wynn.screenguard.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NavigationUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun dockNavigatesOnlyFreezeActivityAndSettings() {
        val graph=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as GuardApplication
        runBlocking {graph.store.settings {it.copy(onboardingDone=true,pinHash="",pinSalt="")}}
        val vm=GuardViewModel(graph)
        compose.setContent {ScreenGuardApp(vm)}
        compose.onNodeWithContentDescription("Tambah aplikasi").assertIsDisplayed()
        compose.onNodeWithContentDescription("Jaringan").assertDoesNotExist()
        compose.onNodeWithContentDescription("Aktivitas").performClick()
        compose.onNodeWithText("Waktu layar").assertExists()
        compose.onNodeWithContentDescription("Pengaturan").performClick()
        compose.onNodeWithText("Periksa update").assertExists()
        compose.onNodeWithText("PIN Orang Tua").assertDoesNotExist()
        compose.onNodeWithText("Target waktu layar").assertDoesNotExist()
    }
    @Test fun dockSupportsLargeTextAndSelectedSemantics() {
        var selected:Page?=null
        compose.setContent {val d=LocalDensity.current;CompositionLocalProvider(LocalDensity provides Density(d.density,2f)) {
            GuardTheme(true) {FloatingDock(Page.FREEZE) {selected=it}}
        }}
        compose.onNodeWithContentDescription("Bekukan").assertIsSelected()
        compose.onNodeWithContentDescription("Aktivitas").performClick()
        assertEquals(Page.HOME,selected)
        compose.onNodeWithContentDescription("Pengaturan").assertIsDisplayed()
    }
}
