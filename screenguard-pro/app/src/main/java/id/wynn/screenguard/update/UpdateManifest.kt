package id.wynn.screenguard.update

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class UpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val packageName: String,
    val minSdk: Int,
    val apkUrl: String,
    val sha256: String,
    val signerSha256: String,
    val sizeBytes: Long,
    val notes: String = ""
) {
    fun validate() {
        require(versionCode > 0 && versionName.length in 1..80) { "Versi update tidak valid" }
        require(minSdk in 30..100 && sizeBytes in 1..MAX_APK_SIZE) { "Ukuran atau versi Android tidak valid" }
        require(sha256.matches(Regex("[a-fA-F0-9]{64}")) && signerSha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Checksum update tidak valid" }
        require(trustedAsset(apkUrl)) { "Sumber APK bukan rilis resmi ScreenGuard" }
        require(packageName.matches(Regex("id\\.wynn\\.screenguard(\\.preview)?"))) { "Identitas paket tidak valid" }
        require(notes.length <= 8000) { "Catatan rilis terlalu panjang" }
    }
    fun newerFor(installedPackage: String, installedVersion: Long, sdk: Int, installedSigners: Set<String>): Boolean {
        validate()
        require(packageName == installedPackage) { "Paket update berbeda; pemasangan ditolak" }
        if (versionCode <= installedVersion) return false
        require(minSdk <= sdk) { "Update membutuhkan Android API $minSdk" }
        require(signerSha256.lowercase() in installedSigners) { "Kunci rilis berbeda dari APK ini. Update langsung tidak dapat dipasang; lihat panduan migrasi di kode sumber." }
        return true
    }
    companion object {
        const val MAX_APK_SIZE = 100L * 1024 * 1024
        fun trustedAsset(url: String): Boolean = runCatching {
            val uri = URI(url)
            val release = uri.host == "github.com" && uri.rawPath.startsWith("/WynnDev-rill/Wynn-Store/releases/download/screenguard-")
            val bootstrap = uri.host == "raw.githubusercontent.com" && uri.rawPath.matches(Regex("/WynnDev-rill/Wynn-Store/main/screenguard-pro/distribution/ScreenGuard-Pro-[0-9]+\\.[0-9]+\\.[0-9]+\\.apk"))
            uri.scheme == "https" && uri.port == -1 && uri.userInfo == null && (release || bootstrap) &&
                !uri.rawPath.contains("..") && uri.fragment == null && uri.query == null
        }.getOrDefault(false)
    }
}
