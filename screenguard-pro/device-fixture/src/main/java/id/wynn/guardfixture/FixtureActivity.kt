package id.wynn.guardfixture

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.widget.TextView
import java.net.InetSocketAddress
import java.net.Socket

/** Isolated test APK, never included in ScreenGuard's distributed application. */
class FixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "ScreenGuard test target"; textSize = 22f; setPadding(24, 64, 24, 24) })
    }
}
class ProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        Thread {
            val online = runCatching { Socket().use { it.connect(InetSocketAddress("1.1.1.1", 443), 4000) } }.isSuccess
            result.resultData = if (online) "ONLINE" else "BLOCKED"
            result.finish()
        }.start()
    }
}
