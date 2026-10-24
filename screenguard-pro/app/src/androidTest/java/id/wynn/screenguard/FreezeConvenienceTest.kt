package id.wynn.screenguard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import id.wynn.screenguard.domain.*
import id.wynn.screenguard.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FreezeConvenienceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun unconnectedPickerOffersPermissionWithoutPretendingToFreeze() {
        var permission=false
        compose.setContent {GuardTheme {FreezeScreen(GuardState(),DeviceData(loading=false),false,false,{}, {}, {}, {_,_->},permissions={permission=true})}}
        compose.onNodeWithContentDescription("Tambah aplikasi").performClick()
        compose.onNodeWithText("Hubungkan Shizuku").performClick()
        assertTrue(permission)
    }
}
