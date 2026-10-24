package id.wynn.screenguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import id.wynn.screenguard.service.GuardMonitorService
import id.wynn.screenguard.ui.*

class MainActivity : ComponentActivity() {
    private val viewModel: GuardViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ScreenGuardApp(viewModel) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) { delay(15_000); viewModel.refresh() }
            }
        }
    }
    override fun onResume() {
        super.onResume(); viewModel.recover(); viewModel.checkUpdates()
        if ((application as GuardApplication).store.state.value.needsMonitoring) GuardMonitorService.start(this)
    }
    override fun onStop() { viewModel.invalidateSession(); super.onStop() }
}
