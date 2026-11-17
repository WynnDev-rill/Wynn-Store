package id.wynn.vocamemo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object UpdatePolicy {
    const val FEED =
        "https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/vocamemo/updates/stable.json"
    const val MAX_BYTES = 100L * 1024 * 1024

    fun trusted(url: String): Boolean =
        runCatching {
                val u = URI(url)
                u.scheme == "https" &&
                    u.host == "github.com" &&
                    u.port == -1 &&
                    u.userInfo == null &&
                    u.query == null &&
                    u.fragment == null &&
                    u.rawPath.matches(
                        Regex(
                            "/WynnDev-rill/Wynn-Store/releases/download/vocamemo-v[0-9]+\\.[0-9]+\\.[0-9]+/VocaMemo\\.apk"
                        )
                    )
            }
            .getOrDefault(false)
}

data class Release(
    val versionCode: Long,
    val versionName: String,
    val packageName: String,
    val minSdk: Int,
    val apkUrl: String,
    val sha256: String,
    val signerSha256: String,
    val sizeBytes: Long,
    val notes: String,
) {
    fun validate() {
        require(versionCode > 0 && versionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) {
            "Versi update tidak valid"
        }
        require(
            packageName == "id.wynn.vocamemo" &&
                minSdk in 26..100 &&
                sizeBytes in 1..UpdatePolicy.MAX_BYTES
        ) {
            "Identitas update tidak valid"
        }
        require(UpdatePolicy.trusted(apkUrl)) { "Sumber update bukan rilis resmi" }
        require(
            sha256.matches(Regex("[a-f0-9]{64}")) && signerSha256.matches(Regex("[a-f0-9]{64}"))
        ) {
            "Checksum tidak valid"
        }
        require(notes.length <= 8000)
    }

    companion object {
        fun parse(s: String): Release {
            val j = JSONObject(s)
            return Release(
                    j.getLong("versionCode"),
                    j.getString("versionName"),
                    j.getString("packageName"),
                    j.getInt("minSdk"),
                    j.getString("apkUrl"),
                    j.getString("sha256"),
                    j.getString("signerSha256"),
                    j.getLong("sizeBytes"),
                    j.optString("notes"),
                )
                .also { it.validate() }
        }
    }
}

class Updates(private val context: Context) {
    @Suppress("DEPRECATION")
    private fun certificates(path: String? = null): Set<String> {
        val pm = context.packageManager
        val flags =
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES
        val info =
            if (path == null) pm.getPackageInfo(context.packageName, flags)
            else pm.getPackageArchiveInfo(path, flags) ?: error("APK tidak valid")
        val signatures =
            if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners
            else info.signatures
        return signatures.orEmpty().map { sha(it.toByteArray()) }.toSet()
    }

    suspend fun check(): Release? =
        withContext(Dispatchers.IO) {
            val text =
                connection(UpdatePolicy.FEED).let { c ->
                    try {
                        require(c.responseCode == 200) {
                            "Server update belum tersedia (${c.responseCode})"
                        }
                        c.inputStream.use { input ->
                            val bytes = input.readLimited(65536)
                            require(bytes.size <= 65536)
                            bytes.toString(Charsets.UTF_8)
                        }
                    } finally {
                        c.disconnect()
                    }
                }
            val r = Release.parse(text)
            if (r.versionCode <= BuildConfig.VERSION_CODE) return@withContext null
            require(context.packageName == r.packageName) { "Update hanya untuk versi release" }
            require(r.minSdk <= Build.VERSION.SDK_INT) {
                "Update membutuhkan Android API ${r.minSdk}"
            }
            require(r.signerSha256 in certificates()) { "Sertifikat signing update berbeda" }
            r
        }

    suspend fun download(r: Release): File =
        withContext(Dispatchers.IO) {
            r.validate()
            require(r.signerSha256 in certificates())
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val temp = File(dir, "VocaMemo.part")
            val output = File(dir, "VocaMemo.apk")
            try {
                val c = connection(r.apkUrl)
                try {
                    require(c.responseCode == 200) { "Unduhan gagal (${c.responseCode})" }
                    c.inputStream.use { input ->
                        temp.outputStream().use { out ->
                            val buffer = ByteArray(32768)
                            var total = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                total += n
                                require(total <= r.sizeBytes && total <= UpdatePolicy.MAX_BYTES) {
                                    "Ukuran APK berbeda"
                                }
                                out.write(buffer, 0, n)
                            }
                        }
                    }
                } finally {
                    c.disconnect()
                }
                require(temp.length() == r.sizeBytes && shaFile(temp) == r.sha256) {
                    "Checksum APK tidak cocok"
                }
                @Suppress("DEPRECATION")
                val info =
                    context.packageManager.getPackageArchiveInfo(temp.path, 0) ?: error("APK rusak")
                require(
                    info.packageName == r.packageName &&
                        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info) ==
                            r.versionCode
                ) {
                    "Versi paket berbeda"
                }
                require(certificates(temp.path) == setOf(r.signerSha256)) {
                    "Sertifikat APK berbeda"
                }
                output.delete()
                check(temp.renameTo(output))
                output
            } finally {
                temp.delete()
            }
        }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    companion object {
        fun schedule(c: Context) {
            val request =
                PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .build()
            WorkManager.getInstance(c)
                .enqueueUniquePeriodicWork(
                    "vocamemo-updates",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        }

        private fun connection(url: String) =
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "VocaMemo/${BuildConfig.VERSION_NAME}")
            }

        private fun sha(bytes: ByteArray) =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it)
            }

        private fun shaFile(f: File): String {
            val d = MessageDigest.getInstance("SHA-256")
            f.inputStream().use {
                val b = ByteArray(32768)
                while (true) {
                    val n = it.read(b)
                    if (n < 0) break
                    d.update(b, 0, n)
                }
            }
            return d.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        try {
            val r = Updates(applicationContext).check()
            if (r != null) {
                val c = applicationContext
                val n = c.getSystemService(NotificationManager::class.java)
                n.createNotificationChannel(
                    NotificationChannel(
                        "updates",
                        "Pembaruan VocaMemo",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                )
                if (
                    Build.VERSION.SDK_INT < 33 ||
                        ContextCompat.checkSelfPermission(
                            c,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = Intent(c, MainActivity::class.java).putExtra("updates", true)
                    val pending =
                        PendingIntent.getActivity(
                            c,
                            1,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    n.notify(
                        1001,
                        NotificationCompat.Builder(c, "updates")
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setContentTitle("VocaMemo ${r.versionName} tersedia")
                            .setContentText("Ketuk untuk melihat dan memasang pembaruan.")
                            .setContentIntent(pending)
                            .setAutoCancel(true)
                            .build(),
                    )
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
}
