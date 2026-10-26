package id.wynn.furina

import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/** Untrusted archives never get a path outside their new, app-private staging directory. */
object SafeArchive {
 const val MAX_ZIP = 500L * 1024 * 1024
 const val MAX_TOTAL = 2L * 1024 * 1024 * 1024
 const val MAX_FILE = 500L * 1024 * 1024
 fun safeChild(root: File, name: String): File {
  require(name.isNotBlank() && !name.contains('\\') && !name.startsWith('/') && !name.contains(':')) { "Path model tidak aman" }
  val target = File(root, name).canonicalFile
  require(target.path.startsWith(root.canonicalPath + File.separator)) { "Path model keluar dari folder" }
  return target
 }
 fun extract(input: InputStream, root: File, charset: Charset = Charsets.UTF_8) {
  require(root.mkdirs() || root.isDirectory)
  var total = 0L; var count = 0
  ZipInputStream(input, charset).use { zip ->
   while (true) {
    val e = zip.nextEntry ?: break
    require(++count <= 4096) { "Terlalu banyak file" }
    val out = safeChild(root, e.name)
    if (e.isDirectory) { require(!out.isFile); out.mkdirs(); continue }
    require(!out.exists()) { "ZIP berisi path duplikat" }
    require(e.size <= MAX_FILE) { "File ${e.name} melebihi batas 500 MB setelah diekstrak" }
    out.parentFile!!.mkdirs()
    var bytes = 0L
    out.outputStream().buffered().use { stream ->
     val buffer = ByteArray(65536)
     while (true) {
      val n = zip.read(buffer); if (n == -1) break
      bytes += n; total += n
      require(bytes <= MAX_FILE) { "File ${e.name} melebihi batas 500 MB setelah diekstrak" }
      require(total <= MAX_TOTAL) { "Total hasil ekstraksi melebihi batas 2 GB" }
      stream.write(buffer, 0, n)
     }
    }
   }
  }
 }
}
