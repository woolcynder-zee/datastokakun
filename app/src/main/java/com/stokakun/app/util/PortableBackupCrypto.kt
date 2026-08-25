package com.stokakun.app.util

import android.util.Base64
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Password-based encryption for backup portability across devices. */
object PortableBackupCrypto {
    private const val VERSION = "1"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val ITERATIONS = 210_000

    fun encrypt(plaintext: String, password: String): String {
        require(password.length >= 8) { "Password backup minimal 8 karakter." }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return listOf(
            VERSION,
            enc(salt),
            enc(iv),
            enc(ciphertext)
        ).joinToString(":")
    }

    fun decrypt(token: String, password: String): String {
        require(password.length >= 8) { "Password backup minimal 8 karakter." }
        val parts = token.split(":")
        require(parts.size == 4 && parts[0] == VERSION) { "Format password backup tidak valid." }
        val salt = dec(parts[1])
        val iv = dec(parts[2])
        val ciphertext = dec(parts[3])
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        return try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            throw IllegalArgumentException("Password backup salah atau data backup rusak.")
        }
    }

    fun fingerprintsMatch(first: String, second: String): Boolean =
        MessageDigest.isEqual(first.toByteArray(), second.toByteArray())

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private fun enc(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    private fun dec(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)
}
