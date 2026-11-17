package id.wynn.arithmemo.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import id.wynn.arithmemo.MainActivity
import id.wynn.arithmemo.R
import java.util.concurrent.TimeUnit

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val updater = AppUpdater(applicationContext)
        if (!updater.automatic.value) return Result.success()
        updater.check()
        val s = updater.state.value
        if (s.error) return if (runAttemptCount < 2) Result.retry() else Result.success()
        val release = s.available ?: return Result.success()
        val prefs = applicationContext.getSharedPreferences("updates", Context.MODE_PRIVATE)
        if (prefs.getLong("notified", 0) >= release.versionCode) return Result.success()
        val granted =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        if (
            granted && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        ) {
            applicationContext
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        "updates",
                        "Pembaruan aplikasi",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                )
            val intent =
                Intent(applicationContext, MainActivity::class.java).putExtra("updates", true)
            val pending =
                PendingIntent.getActivity(
                    applicationContext,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val notification =
                NotificationCompat.Builder(applicationContext, "updates")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("ArithMemo ${release.versionName} tersedia")
                    .setContentText("Ketuk untuk memeriksa dan memasang pembaruan.")
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .build()
            try {
                NotificationManagerCompat.from(applicationContext).notify(1001, notification)
                prefs.edit().putLong("notified", release.versionCode).apply()
            } catch (_: SecurityException) {
                /* Permission may be revoked between check and notify. */
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val enabled =
                context
                    .getSharedPreferences("updates", Context.MODE_PRIVATE)
                    .getBoolean("automatic", true)
            val manager = WorkManager.getInstance(context)
            if (!enabled) {
                manager.cancelUniqueWork("arithmemo-updates")
                return
            }
            val work =
                PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .build()
            manager.enqueueUniquePeriodicWork(
                "arithmemo-updates",
                ExistingPeriodicWorkPolicy.KEEP,
                work,
            )
        }
    }
}
