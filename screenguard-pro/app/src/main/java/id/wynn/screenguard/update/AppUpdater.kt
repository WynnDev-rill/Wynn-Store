package id.wynn.screenguard.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import id.wynn.screenguard.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** No usage or device identifiers are transmitted. Only a public static release manifest is fetched. */
class AppUpdater(private val context: Context) {
    data class State(val checking: Boolean = false, val available: UpdateManifest? = null,
        val progress: Float? = null, val ready: Boolean = false, val message: String = "Belum diperiksa",
        val lastChecked: Long = 0, val error: Boolean = false)
    val state = MutableStateFlow(State())
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    val automatic = MutableStateFlow(prefs.getBoolean("automatic", true))
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val folder get() = File(context.cacheDir, "updates").apply { mkdirs() }
    private val apk get() = File(folder, "update.apk")
    fun automatic(enabled: Boolean) { prefs.edit().putBoolean("automatic", enabled).apply(); automatic.value = enabled }
    suspend fun check() {
        if (!mutex.tryLock()) return
        try {
            state.value = state.value.copy(checking = true, error = false, message = "Memeriksa versi terbaru…")
            val next = withContext(Dispatchers.IO) {
                val channel = if (BuildConfig.APPLICATION_ID.endsWith(".preview")) "preview" else "stable"
                val url = "https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/screenguard-pro/updates/$channel.json"
                open(url).useConnection { connection ->
                    if (connection.responseCode == 404) return@useConnection null
                    check(connection.responseCode == 200) { "Server update merespons ${connection.responseCode}" }
                    val bytes = connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (output.size() <= 32_768) { val n = input.read(buffer); if (n < 0) break; output.write(buffer, 0, n) }
                        output.toByteArray()
                    }
                    require(bytes.size <= 32_768) { "Metadata update terlalu besar" }
                    json.decodeFromString<UpdateManifest>(bytes.toString(Charsets.UTF_8)).also { it.validate() }
                }
            }
            if (next == null) {
                state.value = State(message = "Belum ada rilis untuk saluran ini", lastChecked = System.currentTimeMillis())
                return
            }
            val signers = withContext(Dispatchers.IO) { installedSigners() }
            val newer = next.newerFor(context.packageName, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT, signers)
            val sameDownload = state.value.available == next && state.value.ready && apk.exists()
            state.value = State(available = next.takeIf { newer }, ready = sameDownload && newer,
                message = if (newer) "Versi ${next.versionName} tersedia" else "Kamu menggunakan versi terbaru",
                lastChecked = System.currentTimeMillis())
        } catch (e: CancellationException) { state.value = state.value.copy(checking = false); throw e }
        catch (e: Exception) { state.value = state.value.copy(checking = false, message = e.message ?: "Tidak dapat memeriksa update. Periksa koneksi internet.", error = true) }
        finally { mutex.unlock() }
    }
    suspend fun download() = mutex.withLock {
        val next = state.value.available ?: return@withLock
        state.value = state.value.copy(progress = 0f, ready = false, error = false, message = "Mengunduh update…")
        val partial = File(folder, "update.part")
        try {
            withContext(Dispatchers.IO) {
                apk.delete(); partial.delete()
                open(next.apkUrl).useConnection { connection ->
                    require(connection.responseCode == 200) { "Unduhan gagal (${connection.responseCode})" }
                    val digest = MessageDigest.getInstance("SHA-256")
                    var count = 0L
                    connection.inputStream.use { input -> partial.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer); if (n < 0) break
                            count += n
                            require(count <= next.sizeBytes && count <= UpdateManifest.MAX_APK_SIZE) { "Ukuran unduhan tidak sesuai" }
                            digest.update(buffer, 0, n); output.write(buffer, 0, n)
                            state.value = state.value.copy(progress = count.toFloat() / next.sizeBytes)
                        }
                    } }
                    require(count == next.sizeBytes) { "Unduhan belum lengkap" }
                    require(hex(digest.digest()) == next.sha256.lowercase()) { "Checksum APK berbeda; unduhan dibatalkan" }
                }
                require(partial.renameTo(apk)) { "Tidak dapat menyimpan update" }
                verifyApk(next)
            }
            state.value = state.value.copy(progress = null, ready = true, message = "Update terverifikasi dan siap dipasang")
        } catch (e: CancellationException) {
            partial.delete(); apk.delete()
            state.value = state.value.copy(progress = null, ready = false, message = "Unduhan dibatalkan")
            throw e
        } catch (e: Exception) {
            partial.delete(); apk.delete()
            state.value = state.value.copy(progress = null, ready = false, error = true, message = e.message ?: "Unduhan gagal")
        }
    }
    /** Called only after an explicit user tap. Android owns the final install confirmation. */
    suspend fun install(): Intent? {
        val next = state.value.available ?: return null
        return try {
            withContext(Dispatchers.IO) { verifyApk(next) }
            if (!context.packageManager.canRequestPackageInstalls()) {
                state.value = state.value.copy(message = "Izinkan pemasangan dari ScreenGuard, lalu kembali dan ketuk Pasang update.")
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            state.value = state.value.copy(ready = false, error = true, message = e.message ?: "Pemasangan tidak tersedia")
            null
        }
    }
    @Suppress("DEPRECATION")
    private fun verifyApk(next: UpdateManifest) {
        require(apk.isFile && apk.length() == next.sizeBytes) { "APK update tidak ditemukan; unduh kembali" }
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().use { input -> val buffer = ByteArray(32 * 1024); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        require(hex(digest.digest()) == next.sha256.lowercase()) { "Integritas APK gagal diverifikasi" }
        val info = context.packageManager.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("APK tidak valid")
        require(info.packageName == context.packageName && info.longVersionCode == next.versionCode && info.longVersionCode > BuildConfig.VERSION_CODE) { "Identitas atau versi APK tidak sesuai" }
        val signers = info.signingInfo?.apkContentsSigners?.map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }?.toSet().orEmpty()
        require(signers.isNotEmpty() && signers == installedSigners() && next.signerSha256.lowercase() in signers) { "Tanda tangan APK berbeda; pemasangan ditolak" }
    }
    @Suppress("DEPRECATION")
    private fun installedSigners(): Set<String> = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        .signingInfo?.apkContentsSigners?.map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }?.toSet().orEmpty()
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    /** HTTPS only; GitHub release CDN redirects are constrained and capped. */
    private fun open(address: String): HttpURLConnection {
        var current = URL(address)
        repeat(6) {
            require(current.protocol == "https" && current.userInfo == null && current.port == -1) { "Koneksi update tidak aman" }
            require(current.host in setOf("raw.githubusercontent.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")) { "Host update tidak diizinkan" }
            val connection = (current.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000; readTimeout = 20_000; instanceFollowRedirects = false
                setRequestProperty("User-Agent", "ScreenGuard-Pro/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                if (connection.responseCode in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location") ?: error("Redirect tanpa tujuan")
                    current = URL(current, location); connection.disconnect()
                } else return connection
            } catch (e: Exception) { connection.disconnect(); throw e }
        }
        throw IOException("Terlalu banyak redirect update")
    }
    private inline fun <T> HttpURLConnection.useConnection(block: (HttpURLConnection) -> T): T = try { block(this) } finally { disconnect() }
}
