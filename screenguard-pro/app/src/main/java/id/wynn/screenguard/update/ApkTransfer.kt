package id.wynn.screenguard.update

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/** Streaming verification; never buffers an APK in memory. Does not close caller-owned streams. */
internal object ApkTransfer {
    fun copy(input: InputStream, output: OutputStream, expectedBytes: Long, expectedSha256: String, progress: (Long) -> Unit) {
        require(expectedBytes in 1..UpdateManifest.MAX_APK_SIZE)
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32 * 1024)
        var count = 0L
        progress(0)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            count += n
            require(count <= expectedBytes) { "Ukuran unduhan tidak sesuai" }
            digest.update(buffer, 0, n); output.write(buffer, 0, n)
            progress(count)
        }
        require(count == expectedBytes) { "Unduhan belum lengkap" }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        require(actual == expectedSha256.lowercase()) { "Checksum APK berbeda; unduhan dibatalkan" }
    }
}
