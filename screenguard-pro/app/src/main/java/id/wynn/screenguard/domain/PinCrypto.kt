package id.wynn.screenguard.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinCrypto {
    fun valid(pin: String) = pin.length == 6 && pin.all { it in '0'..'9' }
    fun create(pin: String): Pair<String, String> {
        require(valid(pin))
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        return Base64.getEncoder().encodeToString(salt) to derive(pin, salt)
    }
    fun verify(pin: String, salt: String, hash: String): Boolean = runCatching {
        valid(pin) && MessageDigest.isEqual(derive(pin, Base64.getDecoder().decode(salt)).toByteArray(), hash.toByteArray())
    }.getOrDefault(false)
    private fun derive(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try { Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded) }
        finally { spec.clearPassword() }
    }
}

data class MathChallenge(val a: Int, val b: Int, val c: Int) {
    val question: String get() = "$a + $b × $c = ?"
    fun accepts(value: String) = value.trim().toIntOrNull() == a + b * c
    companion object {
        fun next(): MathChallenge = SecureRandom().let { MathChallenge(it.nextInt(30) + 10, it.nextInt(9) + 2, it.nextInt(6) + 2) }
    }
}
