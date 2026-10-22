package id.wynn.screenguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import id.wynn.screenguard.service.GuardMonitorService
import id.wynn.screenguard.ui.*

class MainActivity : ComponentActivity() {
    private val viewModel: GuardViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ScreenGuardApp(viewModel) }
    }
    override fun onResume() {
        super.onResume(); viewModel.refresh()
        if ((application as GuardApplication).store.state.value.settings.monitorEnabled) GuardMonitorService.start(this)
    }
    override fun onStop() { viewModel.invalidateSession(); super.onStop() }
}
