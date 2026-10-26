package id.wynn.furina

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val version: String,val page: String)
data class ReleaseCheck(val update: AppUpdate?, val published: Boolean)
object UpdateChecker {
 fun versionCode(value: String): List<Int> = value.removePrefix("furina-v").split('.').map { it.toIntOrNull() ?: 0 }
 fun newer(a: String,b: String): Boolean {
  val x=versionCode(a); val y=versionCode(b)
  for(i in 0 until maxOf(x.size,y.size)) {
   val av=x.getOrElse(i){0}; val bv=y.getOrElse(i){0}; if(av!=bv) return av>bv
  }
  return false
 }
 fun check(): ReleaseCheck {
  val c=URL("https://api.github.com/repos/WynnDev-rill/Wynn-Store/releases?per_page=30").openConnection() as HttpURLConnection
  try {
   c.connectTimeout=8000; c.readTimeout=8000
   c.setRequestProperty("Accept","application/vnd.github+json"); c.setRequestProperty("User-Agent","FurinaPocketPet/${BuildConfig.VERSION_NAME}")
   require(c.responseCode==200) { "GitHub belum dapat dihubungi (${c.responseCode})" }
   val bytes=c.inputStream.use { input ->
    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true) { val n=input.read(buffer); if(n<0) break; require(out.size()+n<=2*1024*1024) { "Respons GitHub terlalu besar" };out.write(buffer,0,n) };out.toByteArray()
   }
   return parse(String(bytes),BuildConfig.VERSION_NAME)
  } finally { c.disconnect() }
 }
 fun parse(json: String,current: String): ReleaseCheck {
   val releases=JSONArray(json)
   var published=false
   var latest: AppUpdate?=null
   repeat(releases.length()) { i ->
    val r=releases.getJSONObject(i); val tag=r.optString("tag_name")
    if(tag.matches(Regex("furina-v[0-9]+\\.[0-9]+\\.[0-9]+")) && !r.optBoolean("draft") && !r.optBoolean("prerelease")) {
     val assets=r.optJSONArray("assets") ?: return@repeat
     val hasApk=(0 until assets.length()).any { assets.getJSONObject(it).optString("name").startsWith("Furina-Pocket-Pet-") && assets.getJSONObject(it).optString("name").endsWith(".apk") }
     if(hasApk) published=true
     if(hasApk && newer(tag,current) && (latest==null || newer(tag,latest!!.version)))
      latest=AppUpdate(tag.removePrefix("furina-v"),"https://github.com/WynnDev-rill/Wynn-Store/releases/tag/$tag")
    }
   }
   return ReleaseCheck(latest,published)
 }
}
