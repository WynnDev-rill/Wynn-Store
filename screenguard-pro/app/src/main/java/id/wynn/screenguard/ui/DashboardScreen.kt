package id.wynn.screenguard.ui

import androidx.compose.runtime.Composable
import id.wynn.screenguard.domain.*
import java.time.LocalDate

@Composable fun DashboardScreen(state: GuardState,device: DeviceData,connected: Boolean,navigate: (Page)->Unit,
    selectApp: (InstalledApp)->Unit,permissions: ()->Unit,selectDayApp: (InstalledApp,LocalDate)->Unit={app,_->selectApp(app)},
    refresh: ()->Unit={},selectPeriodApp: ((InstalledApp,LocalDate,Int)->Unit)?=null) {
    ActivityScreen(device,refresh=refresh,permissions=permissions) {app,date,count ->
        if(selectPeriodApp!=null) selectPeriodApp(app,date,count) else selectDayApp(app,date)
    }
}
