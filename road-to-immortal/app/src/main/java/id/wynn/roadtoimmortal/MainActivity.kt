package id.wynn.roadtoimmortal
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import id.wynn.roadtoimmortal.ui.ImmortalRoot
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){installSplashScreen();super.onCreate(savedInstanceState);enableEdgeToEdge();setContent {ImmortalRoot(viewModel())}}}
