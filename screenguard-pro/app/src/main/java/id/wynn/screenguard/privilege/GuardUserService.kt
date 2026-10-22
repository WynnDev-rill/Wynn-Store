package id.wynn.screenguard.privilege

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import id.wynn.screenguard.domain.PackageSafety
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** Typed Shizuku IPC; no arbitrary shell string or general-purpose command endpoint. */
class GuardUserService(private val context: Context) : IGuardService.Stub() {
    private val pm = context.packageManager
    private fun connectivity(): Any {
        val binder = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "connectivity") as IBinder
        return requireNotNull(Class.forName("android.net.IConnectivityManager\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, binder)) { "Layanan Connectivity tidak tersedia" }
    }
    override fun capabilities(): String = result {
        val svc = connectivity()
        svc.javaClass.getMethod("setUidFirewallRule", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        svc.javaClass.getMethod("setFirewallChainEnabled", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
    }
    @Suppress("DEPRECATION") private fun validate(pkg: String, uid: Int? = null): ApplicationInfo {
        require(PackageSafety.validPackage(pkg)) { "Nama paket tidak valid" }
        val info = pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS)
        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        require(!PackageSafety.protected(pkg, info.uid, context.packageName, setOfNotNull(home))) { "Aplikasi penting dilindungi" }
        require(info.flags and ApplicationInfo.FLAG_PERSISTENT == 0) { "Layanan sistem dilindungi" }
        if (uid != null) {
            require(info.uid == uid) { "UID aplikasi berubah; muat ulang daftar" }
            require(pm.getPackagesForUid(uid)?.size == 1) { "UID digunakan bersama aplikasi lain" }
        }
        return info
    }
    @Synchronized override fun network(packageName: String, uid: Int, blocked: Boolean): String = result {
        validate(packageName, uid)
        val svc = connectivity()
        // Android OEM_DENY_3 (9), as exposed by ConnectivityService / ShizuWall.
        // Never disable/flush the global chain, which could undo another firewall's rules.
        svc.javaClass.getMethod("setUidFirewallRule", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .invoke(svc, 9, uid, if (blocked) 2 else 0)
        if (blocked) svc.javaClass.getMethod("setFirewallChainEnabled", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).invoke(svc, 9, true)
    }
    @Synchronized override fun freeze(packageName: String, userId: Int, mode: Int, enabled: Boolean): String = result {
        val info = validate(packageName)
        require(userId == info.uid / 100000 && userId >= 0) { "Profil Android tidak cocok" }
        require(mode == 1 || mode == 2)
        val action = if (mode == 1) { if (enabled) "suspend" else "unsuspend" } else { if (enabled) "disable-user" else "enable" }
        val output = command(listOf("/system/bin/cmd", "package", action, "--user", userId.toString(), packageName))
        require(!output.contains("Error", true) && !output.contains("Exception", true) && !output.contains("Failed", true)) { output }
        val fresh = pm.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
        val applied = if (mode == 1) fresh.flags and ApplicationInfo.FLAG_SUSPENDED != 0 else !fresh.enabled
        check(applied == enabled) { "Android menolak perubahan: $output" }
    }
    private fun command(args: List<String>): String {
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        var output = ""
        val reader = Thread { output = process.inputStream.bufferedReader().use { it.readText().take(8192) } }.apply { start() }
        if (!process.waitFor(10, TimeUnit.SECONDS)) { process.destroyForcibly(); throw IllegalStateException("Perintah Android melewati batas waktu") }
        reader.join(1000)
        check(process.exitValue() == 0) { output.ifBlank { "Android menolak operasi" } }
        return output
    }
    private fun result(action: () -> Unit): String = try { action(); "OK" } catch (e: Exception) { "ERROR: ${(e.cause ?: e).message ?: e.javaClass.simpleName}" }
    override fun destroy() = exitProcess(0)
}
