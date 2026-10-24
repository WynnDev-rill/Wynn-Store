package id.wynn.iwatchyou

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object UpdateManager {
 private const val FEED = "https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/iwatchyou/updates/stable.json"
 data class Release(val version: String, val code: Int, val url: String, val sha256: String, val signer: String, val size: Long)
 private fun connection(url: String): HttpURLConnection {
  require(url.startsWith("https://"))
  return (URL(url).openConnection() as HttpURLConnection).apply {
   connectTimeout=12000; readTimeout=20000; instanceFollowRedirects=true
  }
 }
 fun check(activity: Activity, reportErrors: Boolean=false, callback:(Release?,String?)->Unit) {
  Thread {
   try {
    val p = activity.getSharedPreferences("updater",Activity.MODE_PRIVATE)
    if (!reportErrors && System.currentTimeMillis()-p.getLong("checked",0) < 21600000L) return@Thread
    val c=connection(FEED)
    val data=c.inputStream.bufferedReader().use { it.readText() }
    c.disconnect()
    val json=JSONObject(data)
    val release=Release(json.getString("versionName"),json.getInt("versionCode"),json.optString("apkUrl"),json.optString("sha256"),json.optString("signerSha256"),json.optLong("sizeBytes"))
    p.edit().putLong("checked",System.currentTimeMillis()).apply()
    val current=activity.packageManager.getPackageInfo(activity.packageName,0).longVersionCode
    val update=if (release.code>current && release.url.startsWith("https://github.com/WynnDev-rill/Wynn-Store/releases/download/") && release.sha256.length==64 && release.signer.length==64) release else null
    activity.runOnUiThread { callback(update,if (update==null && reportErrors) "Sudah menggunakan versi terbaru." else null) }
   } catch(e:Exception) {
    if (reportErrors) activity.runOnUiThread { callback(null,"Pemeriksaan pembaruan gagal: ${e.message}") }
   }
  }.start()
 }
 private fun digest(bytes:ByteArray):String=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
 fun install(activity: Activity, r:Release, onResult:(String)->Unit) {
  Thread {
   try {
    val directory=File(activity.filesDir,"updates").apply { mkdirs() }
    val file=File(directory,"IWatchYou-${r.version}.apk")
    val sha=MessageDigest.getInstance("SHA-256")
    val c=connection(r.url)
    var written=0L
    c.inputStream.use { input ->
     file.outputStream().use { output ->
      val buf=ByteArray(32*1024)
      while(true) {
       val n=input.read(buf)
       if(n<0) break
       written+=n
       if(written>100_000_000L) error("APK melebihi batas ukuran")
       sha.update(buf,0,n)
       output.write(buf,0,n)
      }
     }
    }
    c.disconnect()
    check(written>0 && written == r.size) { "Ukuran APK tidak sesuai" }
    check(sha.digest().joinToString("") { "%02x".format(it) }.equals(r.sha256,true)) { "Checksum APK tidak cocok" }
    val pm=activity.packageManager
    @Suppress("DEPRECATION")
    val apk=pm.getPackageArchiveInfo(file.absolutePath,android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
    @Suppress("DEPRECATION")
    val installed=pm.getPackageInfo(activity.packageName,android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
    check(apk?.packageName==activity.packageName) { "Identitas aplikasi tidak cocok" }
    check((apk?.longVersionCode ?: 0)>installed.longVersionCode) { "Versi APK tidak lebih baru" }
    val apkCert=apk?.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()?.let(::digest)
    val oldCert=installed.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()?.let(::digest)
    check(apkCert!=null && apkCert.equals(r.signer,true) && apkCert.equals(oldCert,true)) { "Sertifikat penandatangan tidak cocok" }
    activity.runOnUiThread {
     if (Build.VERSION.SDK_INT>=26 && !pm.canRequestPackageInstalls()) {
      onResult("Izinkan instalasi dari IWatchYou, kemudian ulangi pembaruan.")
      activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${activity.packageName}")))
     } else {
      val uri=FileProvider.getUriForFile(activity,"${activity.packageName}.fileprovider",file)
      activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
       setDataAndType(uri,"application/vnd.android.package-archive")
       flags=Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
      })
      onResult("Konfirmasi pemasangan pada dialog Android.")
     }
    }
   } catch(e:Exception) { activity.runOnUiThread { onResult("Gagal mengunduh pembaruan: ${e.message}") } }
  }.start()
 }
}
