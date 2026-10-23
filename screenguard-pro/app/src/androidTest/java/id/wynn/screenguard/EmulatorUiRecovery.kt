package id.wynn.screenguard

import android.os.Build
import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

/** Only the emulator's unrelated launcher ANR is recoverable. Never dismiss a ScreenGuard failure. */
object EmulatorUiRecovery {
    fun dismissLauncherDialog(ui: UiDevice): Boolean {
        if (!Build.FINGERPRINT.contains("generic") && !Build.MODEL.contains("sdk")) return false
        val title = ui.findObject(By.res("android", "alertTitle"))?.text ?: return false
        if (title != "Pixel Launcher isn't responding") return false
        val close = ui.findObject(By.res("android", "aerr_close")) ?: return false
        Log.w("ScreenGuardDeviceQA", "Closing unrelated emulator dialog: $title")
        close.click()
        ui.waitForIdle()
        return true
    }
}
