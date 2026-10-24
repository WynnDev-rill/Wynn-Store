package id.wynn.screenguard.ui

import androidx.compose.runtime.Composable
import id.wynn.screenguard.domain.InstalledApp
import java.time.LocalDate

@Composable fun UsageScreen(device: DeviceData,back: ()->Unit,freeze: (()->Unit)?=null,selected: (InstalledApp)->Unit) {
    ActivityScreen(device,back=back) {app,_,_->selected(app)}
}
