package id.wynn.iwatchyou

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

object UpdateManager {
 private const val FEED="https://raw.githubusercontent.com/WynnDev-rill/Wynn-Store/main/iwatchyou/updates/stable.json"
 private const val PREFIX="https://github.com/WynnDev-rill/Wynn-Store/releases/download/iwatchyou-v"
 private const val LIMIT=100_000_000L
 private val checking=AtomicBoolean(false)
 private val downloading=AtomicBoolean(false)
 data class Release(val version: String,val code: Int,val url: String,val sha256: String,val signer: String,val size: Long,val notes: String="Perbaikan dan penyempurnaan IWatchYou.")
 private fun connection(address: String): HttpURLConnection {
  var url=URL(address)
  repeat(6) {
   require(url.protocol=="https" && url.userInfo==null && (url.port==-1 || url.port==443))
   require(url.host in setOf("raw.githubusercontent.com","github.com","release-assets.githubusercontent.com","objects.githubusercontent.com"))
   val c=(url.openConnection() as HttpURLConnection).apply { connectTimeout=12_000; readTimeout=20_000; instanceFollowRedirects=false; setRequestProperty("User-Agent","IWatchYou") }
   val code=c.responseCode
   if(code in listOf(301,302,303,307,308)) {
    val next=c.getHeaderField("Location"); c.disconnect()
    require(next!=null); url=URL(url,next)
   } else { check(code in 200..299) { "Server pembaruan belum tersedia" }; return c }
  }
  error("Terlalu banyak pengalihan unduhan")
 }
 private fun parse(raw: String,context: Context): Release {
  val j=JSONObject(raw)
  require(j.getString("packageName")==context.packageName)
  require(j.getInt("minSdk")<=Build.VERSION.SDK_INT)
  val r=Release(j.getString("versionName"),j.getInt("versionCode"),j.getString("apkUrl"),j.getString("sha256"),j.getString("signerSha256"),j.getLong("sizeBytes"),j.optString("notes").take(2000))
  require(r.version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")) && r.url.startsWith(PREFIX))
  require(r.sha256.matches(Regex("[a-fA-F0-9]{64}")) && r.signer.matches(Regex("[a-fA-F0-9]{64}")))
  require(r.size in 1..LIMIT && r.code>0)
  return r
 }
 fun check(activity: ComponentActivity,reportErrors: Boolean=false,callback: (Release?,String?)->Unit) {
  if(!checking.compareAndSet(false,true)) { if(reportErrors)callback(null,"Pemeriksaan masih berlangsung.");return }
  activity.lifecycleScope.launch {
   try {
    val update=withContext(Dispatchers.IO) {
     val context=activity.applicationContext
     val p=context.getSharedPreferences("updater",Context.MODE_PRIVATE)
     val age=System.currentTimeMillis()-p.getLong("checked",0)
     var raw=if(!reportErrors && age in 0..21_600_000) p.getString("release",null) else null
     if(raw==null) {
      val c=connection(FEED+"?check="+System.currentTimeMillis())
      try { raw=c.inputStream.use { input -> val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(4096); while(true) { val n=input.read(buffer); if(n<0)break; require(out.size()+n<=65_536);out.write(buffer,0,n) };out.toString("UTF-8") } } finally { c.disconnect() }
      // Validate before caching; malformed feeds never become a silent "latest" response.
      parse(raw!!,context)
      p.edit().putString("release",raw).putLong("checked",System.currentTimeMillis()).apply()
     }
     val r=parse(raw!!,context)
     r.takeIf { it.code>context.packageManager.getPackageInfo(context.packageName,0).longVersionCode }
    }
    callback(update,if(update==null && reportErrors)"Sudah menggunakan versi terbaru." else null)
   } catch(e: CancellationException) { throw e }
   catch(_: Exception) { if(reportErrors)callback(null,"Pemeriksaan gagal. Periksa koneksi lalu coba lagi.") }
   finally { checking.set(false) }
  }
 }
 private fun hex(bytes: ByteArray)=bytes.joinToString("") { "%02x".format(it) }
 private fun verify(context: Context,file: File,r: Release) {
  check(file.length()==r.size)
  val hash=MessageDigest.getInstance("SHA-256")
  file.inputStream().use { input -> val b=ByteArray(32768); while(true) { val n=input.read(b); if(n<0)break; hash.update(b,0,n) } }
  check(hex(hash.digest()).equals(r.sha256,true))
  @Suppress("DEPRECATION") val apk=context.packageManager.getPackageArchiveInfo(file.path,PackageManager.GET_SIGNING_CERTIFICATES) ?: error("APK tidak valid")
  @Suppress("DEPRECATION") val old=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_SIGNING_CERTIFICATES)
  check(apk.packageName==context.packageName && apk.longVersionCode==r.code.toLong() && apk.longVersionCode>old.longVersionCode && apk.versionName==r.version)
  val certs=apk.signingInfo?.apkContentsSigners ?: error("Sertifikat tidak ditemukan")
  val installed=old.signingInfo?.apkContentsSigners ?: error("Sertifikat aplikasi tidak ditemukan")
  check(certs.size==1 && installed.size==1)
  check(MessageDigest.isEqual(certs[0].toByteArray(),installed[0].toByteArray()))
  check(hex(MessageDigest.getInstance("SHA-256").digest(certs[0].toByteArray())).equals(r.signer,true))
 }
 private fun json(r: Release)=JSONObject().put("packageName","id.wynn.iwatchyou").put("minSdk",30).put("versionName",r.version).put("versionCode",r.code).put("apkUrl",r.url).put("sha256",r.sha256).put("signerSha256",r.signer).put("sizeBytes",r.size).toString()
 private fun apk(context: Context)=File(File(context.filesDir,"updates").apply { mkdirs() },"candidate.apk")
 fun install(activity: ComponentActivity,r: Release,onResult: (String)->Unit) {
  if(!downloading.compareAndSet(false,true))return
  activity.lifecycleScope.launch {
   try {
    val context=activity.applicationContext
    withContext(Dispatchers.IO) {
     val target=apk(context); val part=File(target.parentFile,"candidate.part")
     val c=connection(r.url)
     try {
      var written=0L
      c.inputStream.use { input -> part.outputStream().use { out ->
       val b=ByteArray(32768)
       while(true) { ensureActive(); val n=input.read(b); if(n<0)break; written+=n; check(written<=r.size && written<=LIMIT); out.write(b,0,n) }
      } }
      verify(context,part,r)
      check(part.renameTo(target))
     } finally { c.disconnect(); part.delete() }
    }
    activity.getSharedPreferences("updater",Context.MODE_PRIVATE).edit().putString("pending",json(r)).apply()
    if(!activity.packageManager.canRequestPackageInstalls()) {
     onResult("Izinkan pemasangan dari IWatchYou. Pembaruan dilanjutkan saat kamu kembali.")
     activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${activity.packageName}")))
    } else launchInstaller(activity,r,onResult)
   } catch(e: CancellationException) { throw e }
   catch(_: Exception) { onResult("Pembaruan gagal diunduh atau diverifikasi. Coba lagi.") }
   finally { downloading.set(false) }
  }
 }
 private fun launchInstaller(activity: ComponentActivity,r: Release,onResult: (String)->Unit) {
  activity.getSharedPreferences("updater",Context.MODE_PRIVATE).edit().remove("pending").apply()
  val uri=FileProvider.getUriForFile(activity,"${activity.packageName}.fileprovider",apk(activity))
  activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
  onResult("Konfirmasi pemasangan pada dialog Android.")
 }
 fun resumeInstall(activity: ComponentActivity,onResult: (String)->Unit) {
  if(downloading.get() || !activity.packageManager.canRequestPackageInstalls())return
  val p=activity.getSharedPreferences("updater",Context.MODE_PRIVATE)
  val raw=p.getString("pending",null) ?: return
  activity.lifecycleScope.launch {
   try {
    val r=withContext(Dispatchers.IO) { parse(raw,activity).also { verify(activity,apk(activity),it) } }
    launchInstaller(activity,r,onResult)
   } catch(e: CancellationException) { throw e }
   catch(_: Exception) { p.edit().remove("pending").apply(); onResult("APK pembaruan perlu diunduh ulang.") }
  }
 }
}
