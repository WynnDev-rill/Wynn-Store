package id.wynn.screenguard.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import id.wynn.screenguard.*
import id.wynn.screenguard.R
import id.wynn.screenguard.privilege.ShizukuStatus
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/** Retained class name for safely stopping a pre-1.5 service during upgrade. */
class GuardMonitorService : Service() {
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { stopSelf(); return START_NOT_STICKY }
    companion object {
        const val CHANNEL_ID = "monitor-quiet"
        fun start(context: Context): Boolean = false
    }
}

object MonitorScheduler {
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("guard-reconcile", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<PolicyWorker>(1, TimeUnit.HOURS).build())
    }
}
class PolicyWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val graph = applicationContext as GuardApplication
        // Persist daily/hourly records before Android expires its event history.
        if (graph.usage.hasAccess()) runCatching { graph.usage.range(2) }
        if (graph.store.state.value.rules.isEmpty()) return Result.success()
        return try {
            graph.gateway.refresh()
            val errors = graph.controller.reconcile(forceNetwork = true)
            if (errors.isEmpty()) Result.success() else Result.retry()
        } catch (_: Exception) { Result.retry() }
    }
}
class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        val graph = context.applicationContext as GuardApplication
        MonitorScheduler.schedule(context)
        if (graph.store.state.value.needsMonitoring) GuardMonitorService.start(context)
        // On stock non-root Android Shizuku must be started again by the user after boot.
        // Do not silently claim protection when the privileged process is absent.
        WorkManager.getInstance(context).enqueueUniqueWork("guard-restore", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<PolicyWorker>().build())
    }
}
