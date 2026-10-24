package id.wynn.screenguard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import id.wynn.screenguard.data.*
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class ScreenUiTest {
    @get:Rule val compose=createComposeRule()
    private val a=InstalledApp("id.wynn.guardfixture","Demo One",10120,0,true,false,false,false)
    private val c=a.copy(packageName="com.example.two",label="Demo Two",uid=10121)
    private val today=LocalDate.now()
    private val data=DeviceData(apps=listOf(a,c),days=(6 downTo 0).map {UsageDay(today.minusDays(it.toLong()),mapOf(a.packageName to 120000L,c.packageName to 120000L),screenMs=180000L,unlocks=2)},usageAccess=true,loading=false)
    private fun content(block: @androidx.compose.runtime.Composable ()->Unit) {
        compose.setContent {GuardTheme {Surface(Modifier.fillMaxSize().systemBarsPadding(),color=MaterialTheme.colorScheme.background) {block()}}}
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir,"$name.png"))
    }
    @Test fun emptyCollectionContainsOnlyToolbarAndBottomRightAdd() {
        content {FreezeScreen(GuardState(),DeviceData(loading=false),true,false,{}, {}, {}, {_,_->})}
        compose.onNodeWithText("Belum ada aplikasi").assertDoesNotExist()
        compose.onNodeWithText("Tambah aplikasi").assertDoesNotExist()
        compose.onNodeWithText("Semua").assertDoesNotExist()
        compose.onNodeWithText("Terbuka").assertDoesNotExist()
        val fab=compose.onNodeWithContentDescription("Tambah aplikasi").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(fab.center.x>root.center.x);assertTrue(fab.center.y>root.center.y)
        capture("01-freeze-empty")
    }
    @Test fun frozenStateTracksAndroidAndOpeningRestoresNormalState() {
        var opened=false
        val frozen=a.copy(enabled=false)
        val state=GuardState(rules=mapOf(a.packageName to AppRule(a.packageName,managedFreeze=true,freeze=FreezeMode.DISABLE,ownedDisable=true)))
        content {FreezeScreen(state,data.copy(apps=listOf(frozen)),true,false,{}, {opened=true}, {}, {_,_->})}
        compose.onNode(hasStateDescription("Dibekukan")).assertIsDisplayed()
        capture("02-frozen-icon")
        compose.onNode(hasStateDescription("Dibekukan")).performClick()
        assertTrue(opened)
    }
    @Test fun activeManagedAppRemainsVisibleWithNormalIconAndCanBeFrozen() {
        var frozen=false
        val state=GuardState(rules=mapOf(a.packageName to AppRule(a.packageName,managedFreeze=true)))
        content {FreezeScreen(state,data,true,false,{}, {}, {}, {_,_->},close={frozen=true})}
        compose.onNode(hasStateDescription("Aktif")).assertIsDisplayed()
        compose.onNodeWithContentDescription("Kelola Demo One").performClick()
        compose.onNodeWithText("Bekukan sekarang").performClick()
        assertTrue(frozen)
        capture("03-app-actions")
    }
    @Test fun multiSelectionSurvivesPickerSearch() {
        var added=emptyList<InstalledApp>()
        content {FreezeScreen(GuardState(),data,true,false,{}, {}, {}, {_,_->},addMany={added=it})}
        compose.onNodeWithContentDescription("Tambah aplikasi").performClick()
        compose.onNodeWithText("Demo One").performClick()
        compose.onNodeWithText("Cari aplikasi").performTextInput("Two")
        compose.onNodeWithText("Demo Two").performClick()
        compose.onNodeWithText("Bekukan (2)").performClick()
        assertEquals(setOf(a.packageName,c.packageName),added.map {it.packageName}.toSet())
    }
    @Test fun activitySeparatesScreenFromAppTotalsAndHasNoGoalOrInstructions() {
        content {DashboardScreen(GuardState(),data,true,{}, {}, {})}
        compose.onNodeWithTag("activity-total").assertTextEquals("3 mnt")
        compose.onNodeWithText("Ringkasan hari ini").assertDoesNotExist()
        compose.onNodeWithText("Target harian").assertDoesNotExist()
        capture("04-activity-day")
    }
    @Test fun rankingCarriesSelectedPeriodToAppDetails() {
        var days=0
        content {DashboardScreen(GuardState(),data,true,{}, {}, {},selectPeriodApp={_,_,count->days=count})}
        compose.onNodeWithText("7 hari").performClick()
        compose.onNodeWithTag("activity-total").assertTextEquals("21 mnt")
        capture("05-activity-week")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Demo One"))
        compose.onNodeWithText("Demo One").performClick()
        assertEquals(7,days)
    }
    @Test fun applicationDetailsShowPeriodsSessionsAndExactHourlyValues() {
        content {AppStatsScreen(a,data,{}, {_,d->AppDayDetail(d,listOf(120000L)+List(23){0L},1,120000,true)}, {},today.toString(),7)}
        compose.onNodeWithText("Durasi penggunaan").assertIsDisplayed()
        compose.onAllNodesWithText("14 mnt").onFirst().assertIsDisplayed()
        capture("06-app-statistics")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Rincian per jam"))
        compose.onNodeWithText("24 jam").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("23.00–24.00"))
        compose.onNodeWithText("23.00–24.00").assertIsDisplayed()
    }
}
