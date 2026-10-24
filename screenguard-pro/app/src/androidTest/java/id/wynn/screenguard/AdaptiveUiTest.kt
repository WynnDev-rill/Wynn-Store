package id.wynn.screenguard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
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
    @get:Rule val compose=createComposeRule()
    private fun capture(name:String) {
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        File(dir,"$name.png").outputStream().use {compose.onNodeWithTag("adaptive").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun freezeLargestTextAndDarkModeKeepActionsReachable() {
        val app=InstalledApp("id.wynn.guardfixture","Aplikasi dengan nama yang cukup panjang",10123,0,false,false,false,false)
        val state=GuardState(rules=mapOf(app.packageName to AppRule(app.packageName,managedFreeze=true,freeze=FreezeMode.DISABLE,ownedDisable=true)))
        compose.setContent {val d=LocalDensity.current;GuardTheme(true) {CompositionLocalProvider(LocalDensity provides Density(d.density,2f)) {
            Surface(Modifier.width(320.dp).height(600.dp).testTag("adaptive"),color=MaterialTheme.colorScheme.background) {FreezeScreen(state,DeviceData(apps=listOf(app),loading=false),true,false,{}, {}, {}, {_,_->})}
        }}}
        compose.onNodeWithContentDescription("Tambah aplikasi").assertIsDisplayed()
        compose.onNodeWithContentDescription("Kelola ${app.label}").assertIsDisplayed()
        capture("07-freeze-dark-large")
    }
    @Test fun activityLargeTextKeepsNumbersAndPeriodControlsReadable() {
        val date=java.time.LocalDate.now()
        val data=DeviceData(days=listOf(id.wynn.screenguard.data.UsageDay(date,emptyMap(),screenMs=37800000L)),usageAccess=true,loading=false)
        compose.setContent {val d=LocalDensity.current;GuardTheme(true) {CompositionLocalProvider(LocalDensity provides Density(d.density,1.8f)) {
            Surface(Modifier.width(320.dp).height(600.dp).testTag("adaptive"),color=MaterialTheme.colorScheme.background) {ActivityScreen(data) {_,_,_->}}
        }}}
        compose.onNodeWithText("30 hari").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("activity-total"))
        compose.onNodeWithTag("activity-total").assertTextEquals("10 jam 30 mnt")
        capture("09-activity-dark-large")
    }
    @Test fun landscapeSettingsKeepRecoveryAndUpdateAvailable() {
        compose.setContent {GuardTheme(true) {Surface(Modifier.width(600.dp).height(320.dp).testTag("adaptive"),color=MaterialTheme.colorScheme.background) {
            SettingsScreen(GuardState(),ShizukuStatus.READY,{}, {it()}, {}, {}, {}, {}, {})
        }}}
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Tentang aplikasi"))
        compose.onNodeWithText("Tentang aplikasi").assertIsDisplayed()
        capture("08-settings-landscape")
    }
}
