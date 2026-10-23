package id.wynn.screenguard.data

import android.app.admin.DevicePolicyManager
import android.telecom.TelecomManager
import android.provider.Telephony
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import id.wynn.screenguard.domain.*

class AppCatalog(private val context: Context) {
    private val pm = context.packageManager
    @Volatile private var cached: List<InstalledApp> = emptyList()
    @Volatile private var cachedAt = 0L
    @Suppress("DEPRECATION") fun list(tracked: Set<String> = emptySet()): List<InstalledApp> {
        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        val ime = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')
        val dialer = runCatching { context.getSystemService(TelecomManager::class.java).defaultDialerPackage }.getOrNull()
        val sms = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()
        val admins = context.getSystemService(DevicePolicyManager::class.java).activeAdmins.orEmpty().map { it.packageName }
        val extra = (admins + listOfNotNull(dialer, sms, home, ime)).toSet()
        val all = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        val counts = all.groupingBy { it.uid }.eachCount()
        val launcherPackages = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_DISABLED_COMPONENTS)
            .map { it.activityInfo.packageName }.toSet()
        val result = all.map { info ->
            InstalledApp(info.packageName, info.loadLabel(pm).toString(), info.uid, info.category, info.enabled,
                info.flags and ApplicationInfo.FLAG_SUSPENDED != 0,
                PackageSafety.protected(info.packageName, info.uid, context.packageName, extra) || info.flags and ApplicationInfo.FLAG_PERSISTENT != 0,
                counts.getOrDefault(info.uid, 0) > 1,
                info.flags and ApplicationInfo.FLAG_SYSTEM != 0, info.packageName in launcherPackages)
        }.sortedBy { it.label.lowercase() }
        cached = result; cachedAt = android.os.SystemClock.elapsedRealtime()
        return result
    }
    /** Monitoring only queries status for tracked packages; no full app/icon scan every 15s. */
    @Suppress("DEPRECATION") fun managed(tracked: Set<String>): List<InstalledApp> {
        val base = if (cached.isEmpty() || android.os.SystemClock.elapsedRealtime() - cachedAt > 300_000 || !cached.map { it.packageName }.containsAll(tracked)) list(tracked) else cached
        return base.filter { it.packageName in tracked }.mapNotNull { app ->
            runCatching {
                val info = pm.getApplicationInfo(app.packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
                app.copy(uid = info.uid, enabled = info.enabled, suspended = info.flags and ApplicationInfo.FLAG_SUSPENDED != 0,
                    sharedUid = pm.getPackagesForUid(info.uid).orEmpty().size > 1)
            }.getOrNull()
        }
    }
}
