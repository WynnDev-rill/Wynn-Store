package id.wynn.screenguard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import id.wynn.screenguard.data.UsageDay
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class FreezeConvenienceTest {
    @get:Rule val compose = createComposeRule()
    private val a=InstalledApp("com.example.one","Example One",10120,0,true,false,false,false)
    private val b=a.copy(packageName="com.example.two",label="Example Two",uid=10121)
    @Test fun appMenuCanRefreezeAnActiveLease() {
        var closed=false
        val state=GuardState(rules=mapOf(a.packageName to AppRule(a.packageName,gate=AccessGate(),
            overrideUntil=System.currentTimeMillis()+60000,sessionUntilElapsed=android.os.SystemClock.elapsedRealtime()+60000)))
        compose.setContent { GuardTheme { FreezeScreen(state,DeviceData(apps=listOf(a),loading=false),true,false,{}, {}, {}, { _,_-> },close={closed=true}) } }
        compose.onNodeWithContentDescription("Kelola Example One").performClick()
        compose.onNodeWithText("Bekukan sekarang").performClick()
        assertTrue(closed)
    }
    @Test fun addPickerKeepsSelectionAcrossSearchAndAddsBothPackagesTogether() {
        var added=emptyList<InstalledApp>()
        compose.setContent { GuardTheme { FreezeScreen(GuardState(),DeviceData(apps=listOf(a,b),loading=false),true,false,{}, {}, {}, { _,_-> },addMany={added=it}) } }
        compose.onNodeWithContentDescription("Tambah aplikasi").performClick()
        compose.onNodeWithText("Example One").performClick()
        compose.onNodeWithText("Cari aplikasi").performTextInput("Two")
        compose.onNodeWithText("Example Two").performClick()
        compose.onNodeWithText("Bekukan 2 aplikasi").performClick()
        assertEquals(setOf(a.packageName,b.packageName),added.map { it.packageName }.toSet())
    }
    @Test fun statisticsShowsScreenUnionSeparatelyFromAppDurations() {
        val day=UsageDay(LocalDate.now(),mapOf(a.packageName to 120000L,b.packageName to 120000L),screenMs=180000L)
        compose.setContent { GuardTheme { UsageScreen(DeviceData(apps=listOf(a,b),days=listOf(day),usageAccess=true,loading=false),{}) {} } }
        compose.onNodeWithText("3 mnt").assertIsDisplayed()
        compose.onAllNodesWithText("2 mnt").assertCountEquals(2)
    }
}
