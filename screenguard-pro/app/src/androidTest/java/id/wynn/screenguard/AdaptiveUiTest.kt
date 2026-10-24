package id.wynn.screenguard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.privilege.ShizukuStatus
import id.wynn.screenguard.ui.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AdaptiveUiTest {
    @get:Rule val compose = createComposeRule()
    private val state = GuardState(GuardSettings(onboardingDone = true, darkMode = true))
    private fun capture(name: String) {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "screenshots")
        root.mkdirs()
        File(root, "$name.png").outputStream().use { compose.onNodeWithTag("adaptive").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun smallPhoneAndLargeTextKeepQuickActionsReachable() {
        var opened: Page? = null
        compose.setContent {
            val density = LocalDensity.current
            GuardTheme(dark = true) {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                    Surface(Modifier.width(320.dp).height(600.dp).testTag("adaptive"), color = MaterialTheme.colorScheme.background) {
                        DashboardScreen(state, DeviceData(usageAccess = true, loading = false), true, { opened = it }, {}, {})
                    }
                }
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Blokir Internet"))
        compose.onNodeWithText("Blokir Internet").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Bekukan Aplikasi"))
        compose.onNodeWithText("Bekukan Aplikasi").assertIsDisplayed()
        capture("08-small-dark-large-text")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Blokir Internet"))
        compose.onNodeWithText("Blokir Internet").performClick()
        org.junit.Assert.assertEquals(Page.NETWORK, opened)
    }
    @Test fun freezeGridKeepsLongNamesReadableAtLargestText() {
        val target = InstalledApp("test.fixture.longlabel", "Aplikasi dengan nama yang cukup panjang", 10123, 0, false, false, false, false)
        val selected = state.copy(rules = mapOf(target.packageName to AppRule(target.packageName,
            gate = AccessGate(), freeze = FreezeMode.DISABLE, ownedDisable = true)))
        compose.setContent {
            val density = LocalDensity.current
            GuardTheme(dark = true) {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    Surface(Modifier.width(320.dp).height(600.dp).testTag("adaptive"), color = MaterialTheme.colorScheme.background) {
                        FreezeScreen(selected, DeviceData(apps = listOf(target), usageAccess = true, loading = false),
                            true, false, {}, {}, {}, { _, _ -> }, {})
                    }
                }
            }
        }
        compose.onNodeWithText(target.label).assertIsDisplayed()
        capture("16-freeze-largest-text-dark")
    }
    @Test fun landscapeSettingsRemainScrollable() {
        compose.setContent { GuardTheme(dark = true) {
            Surface(Modifier.width(600.dp).height(320.dp).testTag("adaptive"), color = MaterialTheme.colorScheme.background) {
                SettingsScreen(state, ShizukuStatus.READY, {}, { it() }, {}, {}, {}, {}, {})
            }
        } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Tentang Aplikasi"))
        compose.onNodeWithText("Tentang Aplikasi").assertIsDisplayed()
        capture("09-landscape-dark")
    }
}
