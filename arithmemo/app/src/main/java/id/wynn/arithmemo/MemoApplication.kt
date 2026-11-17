package id.wynn.arithmemo

import android.app.Application
import id.wynn.arithmemo.update.UpdateWorker

class MemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UpdateWorker.schedule(this)
    }
}
