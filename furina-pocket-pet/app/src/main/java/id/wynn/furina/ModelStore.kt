package id.wynn.furina

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ModelInfo(val file: File, val name: String, val motions: List<Pair<String,File>>, val expressions: List<Pair<String,File>>)

class ModelStore(private val context: Context) {
 val root = File(context.filesDir,"models").apply { mkdirs() }
 fun fallback(): File {
  val dest = File(root,"whalegirl")
  if (!File(dest,"whalegirl.model3.json").isFile) {
   dest.deleteRecursively(); copyAsset("fallback",dest)
  }
  return File(dest,"whalegirl.model3.json")
 }
 private fun copyAsset(path: String, dest: File) {
  val list = context.assets.list(path) ?: emptyArray()
  if (list.isEmpty()) { dest.parentFile!!.mkdirs(); context.assets.open(path).use { src -> dest.outputStream().use { src.copyTo(it) } } }
  else { dest.mkdirs(); list.forEach { copyAsset("$path/$it",File(dest,it)) } }
 }
 fun selected(c: PetConfig): File = if (c.model.isEmpty()) fallback() else {
  val f = File(c.model).canonicalFile
  require(f.path.startsWith(root.canonicalPath + "/") && f.isFile) { "Model tidak ditemukan. Impor ulang atau pilih model uji." }
  f
 }
 fun list(): List<File> = root.walkTopDown().filter { it.isFile && it.name.endsWith(".model3.json") }.toList()
 fun importZip(uri: Uri): ModelInfo {
  val archive = File(context.cacheDir,"import-${UUID.randomUUID()}.zip")
  val stage = File(root,"stage-${UUID.randomUUID()}")
  try {
   context.contentResolver.openInputStream(uri)!!.use { src -> archive.outputStream().use { out ->
    val buffer=ByteArray(65536); var total=0L
    while(true) { val n=src.read(buffer); if(n<0) break; total+=n; require(total<=300L*1024*1024) { "ZIP maksimal 300 MB" }; out.write(buffer,0,n) }
   } }
   try { archive.inputStream().use { SafeArchive.extract(it,stage) } }
   catch (e: IllegalArgumentException) {
    // A common VTS ZIP uses legacy Chinese file names. Retry without relaxing path/size checks.
    if (e.message?.contains("malformed",true) != true && e.message?.contains("bad entry",true) != true) throw e
    stage.deleteRecursively()
    archive.inputStream().use { SafeArchive.extract(it,stage,charset("GB18030")) }
   }
   val candidates=stage.walkTopDown().filter { it.isFile && it.name.endsWith(".model3.json") }.sortedBy { it.path.length }.toList()
   require(candidates.isNotEmpty()) { "ZIP tidak memiliki .model3.json. Gunakan folder model VTube Studio, bukan .cmo3." }
   candidates.forEach { inspect(it,stage) }
   val relative=candidates.first().relativeTo(stage).path
   val final=File(root,"local-${UUID.randomUUID()}")
   require(stage.renameTo(final)) { "Gagal menyimpan model" }
   return inspect(File(final,relative),final)
  } finally { archive.delete(); stage.deleteRecursively() }
 }
 fun inspect(file: File, boundary: File = file.parentFile!!): ModelInfo {
  require(file.length() <= 2*1024*1024) { "JSON model terlalu besar" }
  val json=JSONObject(file.readText())
  require(json.optInt("Version") == 3) { "Hanya model Cubism 3/4/5 (.model3.json) yang didukung" }
  val refs=json.getJSONObject("FileReferences")
  fun ref(path: String): File {
   val target=SafeArchive.safeChild(file.parentFile!!,path)
   require(target.path.startsWith(boundary.canonicalPath + "/") && target.isFile) { "File model hilang: $path" }
   require(!target.name.endsWith(".json") || target.length()<=8L*1024*1024) { "File JSON terlalu besar" }
   return target
  }
  val moc=ref(refs.getString("Moc"))
  require(moc.length() in 64..64L*1024*1024) { "MOC3 tidak valid atau terlalu besar" }
  require(moc.inputStream().use { String(ByteArray(4).also { bytes -> require(it.read(bytes)==4) },Charsets.US_ASCII) } == "MOC3") { "Header MOC3 tidak valid" }
  val textures=refs.getJSONArray("Textures")
  require(textures.length() in 1..8) { "Model harus memiliki 1–8 tekstur" }
  repeat(textures.length()) { i ->
   val f=ref(textures.getString(i)); val opt=BitmapFactory.Options().apply { inJustDecodeBounds=true }
   BitmapFactory.decodeFile(f.path,opt)
   require(opt.outWidth in 1..16384 && opt.outHeight in 1..16384 && opt.outWidth.toLong()*opt.outHeight<=128L*1024*1024) { "Tekstur model tidak didukung" }
  }
  listOf("Physics","Pose","UserData","DisplayInfo").forEach { key -> refs.optString(key).takeIf { it.isNotEmpty() }?.let { ref(it) } }
  val motions=mutableListOf<Pair<String,File>>()
  refs.optJSONObject("Motions")?.let { groups -> groups.keys().forEach { group ->
   val entries=groups.getJSONArray(group)
   repeat(entries.length()) { i -> motions += "$group · ${i+1}" to ref(entries.getJSONObject(i).getString("File")) }
  } }
  val expressions=mutableListOf<Pair<String,File>>()
  refs.optJSONArray("Expressions")?.let { entries -> repeat(entries.length()) { i ->
   val e=entries.getJSONObject(i); expressions += e.optString("Name","Ekspresi ${i+1}") to ref(e.getString("File"))
  } }
  // VTS hotkeys may refer to standalone files absent from model3.json.
  file.parentFile!!.walkTopDown().filter { it.isFile && (it.name.endsWith(".motion3.json") || it.name.endsWith(".exp3.json")) }.forEach { f ->
   val list=if(f.name.endsWith(".exp3.json")) expressions else motions
   if(list.none { it.second == f }) list += f.name.substringBefore('.') to f
  }
  require(motions.size <= 128 && expressions.size <= 128) { "Terlalu banyak animasi" }
  (motions+expressions).forEach { require(it.second.length()<=8*1024*1024) { "Animasi terlalu besar" } }
  return ModelInfo(file,file.name.removeSuffix(".model3.json"),motions,expressions)
 }
}
