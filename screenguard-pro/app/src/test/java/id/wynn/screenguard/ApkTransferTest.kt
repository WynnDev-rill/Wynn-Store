package id.wynn.screenguard

import id.wynn.screenguard.update.ApkTransfer
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class ApkTransferTest {
    private val bytes = ByteArray(100_000) { (it % 251).toByte() }
    private fun sha(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
    @Test fun validDownloadStreamsAndReportsActualProgress() {
        val output = ByteArrayOutputStream(); val counts = mutableListOf<Long>()
        ApkTransfer.copy(bytes.inputStream(), output, bytes.size.toLong(), sha(bytes)) { counts += it }
        assertArrayEquals(bytes, output.toByteArray()); assertEquals(0L, counts.first()); assertEquals(100_000L, counts.last())
        assertTrue(counts.zipWithNext().all { (a, b) -> b > a })
    }
    @Test fun corruptedDownloadRejected() { assertThrows(IllegalArgumentException::class.java) {
        ApkTransfer.copy(bytes.copyOf().apply { this[5] = 0 }.inputStream(), ByteArrayOutputStream(), bytes.size.toLong(), sha(bytes)) {}
    } }
    @Test fun truncatedDownloadRejected() { assertThrows(IllegalArgumentException::class.java) {
        ApkTransfer.copy(bytes.copyOf(500).inputStream(), ByteArrayOutputStream(), bytes.size.toLong(), sha(bytes)) {}
    } }
    @Test fun oversizedResponseStopsBeforeWritingExtraBytes() {
        val output = ByteArrayOutputStream()
        assertThrows(IllegalArgumentException::class.java) { ApkTransfer.copy(bytes.inputStream(), output, 100, sha(bytes)) {} }
        assertEquals(0, output.size())
    }
    @Test fun cancellationInterruptsTransfer() {
        val output = ByteArrayOutputStream()
        assertThrows(CancellationException::class.java) { ApkTransfer.copy(bytes.inputStream(), output, bytes.size.toLong(), sha(bytes)) { count -> if (count >= 32_768) throw CancellationException() } }
        assertTrue(output.size() < bytes.size)
    }
    @Test fun emptyAdvertisedFileRejected() { assertThrows(IllegalArgumentException::class.java) {
        ApkTransfer.copy(byteArrayOf().inputStream(), ByteArrayOutputStream(), 0, sha(byteArrayOf())) {}
    } }
}
