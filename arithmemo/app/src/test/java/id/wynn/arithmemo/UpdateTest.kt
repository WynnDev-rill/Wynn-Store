package id.wynn.arithmemo

import id.wynn.arithmemo.update.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class UpdateTest {
    private fun manifest() =
        UpdateManifest(
            2,
            "1.0.1",
            "id.wynn.arithmemo",
            26,
            "https://github.com/WynnDev-rill/Wynn-Store/releases/download/arithmemo-v1.0.1/ArithMemo.apk",
            "a".repeat(64),
            "b".repeat(64),
            100,
        )

    @Test
    fun updateOnlyAcceptsNewerTrustedIdentity() {
        val m = manifest()
        assertTrue(m.newerFor("id.wynn.arithmemo", 1, 36, setOf("b".repeat(64))))
        assertFalse(m.newerFor("id.wynn.arithmemo", 2, 36, setOf("b".repeat(64))))
        assertThrows(IllegalArgumentException::class.java) {
            m.newerFor("id.wynn.other", 1, 36, setOf("b".repeat(64)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            m.newerFor("id.wynn.arithmemo", 1, 36, setOf("c".repeat(64)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            m.copy(minSdk = 36).newerFor("id.wynn.arithmemo", 1, 26, setOf("b".repeat(64)))
        }
    }

    @Test
    fun untrustedUrlsAndInvalidMetadataFailClosed() {
        for (url in
            listOf(
                "http://github.com/x.apk",
                "https://evil.com/x.apk",
                "https://github.com/evil/repo/releases/download/arithmemo-v1/x.apk",
                "https://github.com/WynnDev-rill/Wynn-Store/releases/download/arithmemo-v1/../evil.apk",
                manifest().apkUrl + "?token=bad",
            )) {
            assertFalse(url, UpdateManifest.trustedAsset(url))
        }
        assertThrows(IllegalArgumentException::class.java) {
            manifest().copy(sha256 = "bad").validate()
        }
        assertThrows(IllegalArgumentException::class.java) {
            manifest().copy(sizeBytes = 0).validate()
        }
    }

    @Test
    fun transferRejectsTruncationTamperingAndOversize() {
        val data = "signed apk bytes".toByteArray()
        val hash =
            MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
        val output = ByteArrayOutputStream()
        ApkTransfer.copy(ByteArrayInputStream(data), output, data.size.toLong(), hash) {}
        assertArrayEquals(data, output.toByteArray())
        for (size in listOf(data.size - 1, data.size + 1)) assertThrows(
            IllegalArgumentException::class.java
        ) {
            ApkTransfer.copy(
                ByteArrayInputStream(data),
                ByteArrayOutputStream(),
                size.toLong(),
                hash,
            ) {}
        }
        assertThrows(IllegalArgumentException::class.java) {
            ApkTransfer.copy(
                ByteArrayInputStream(data),
                ByteArrayOutputStream(),
                data.size.toLong(),
                "0".repeat(64),
            ) {}
        }
    }
}
