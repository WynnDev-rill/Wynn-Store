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

class GuardMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val graph get() = application as GuardApplication
    private var loop: Job? = null
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!graph.store.state.value.needsMonitoring) { stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("monitor", "Pemantauan ScreenGuard", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun notification(text: String) = NotificationCompat.Builder(this, "monitor").setSmallIcon(R.drawable.ic_guard)
            .setContentTitle("ScreenGuard Pro aktif").setContentText(text).setContentIntent(open)
            .setOngoing(true).setSilent(true).addAction(0, "Kelola / hentikan", open).build()
        ServiceCompat.startForeground(this, 42, notification("Memeriksa batas dan jadwal lokal"),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        if (loop?.isActive != true) loop = scope.launch {
            var lastText = ""
            while (isActive && graph.store.state.value.needsMonitoring) {
                val errors = runCatching { graph.controller.reconcile() }.getOrElse { listOf(it.message ?: "Pemantauan gagal") }
                val text = when {
                    graph.gateway.status.value != ShizukuStatus.READY -> "Aktifkan Shizuku; aturan belum dapat diterapkan"
                    graph.store.state.value.settings.monitorEnabled && !graph.usage.hasAccess() -> "Izin penggunaan diperlukan untuk batas harian"
                    errors.isNotEmpty() -> "${errors.size} aturan perlu diperiksa di aplikasi"
                    else -> "Akses sementara dan aturan lokal berjalan"
                }
                if (text != lastText) {
                    if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this@GuardMonitorService, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) manager.notify(42, notification(text))
                    lastText = text
                }
                delay(if (getSystemService(PowerManager::class.java).isInteractive) 15_000 else 60_000)
            }
            stopSelf()
        }
        return START_STICKY
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object {
        fun start(context: Context): Boolean = runCatching {
            ContextCompat.startForegroundService(context, Intent(context, GuardMonitorService::class.java)); true
        }.getOrDefault(false)
    }
}

object MonitorScheduler {
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("guard-reconcile", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PolicyWorker>(15, TimeUnit.MINUTES).build())
    }
}
class PolicyWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val graph = applicationContext as GuardApplication
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
