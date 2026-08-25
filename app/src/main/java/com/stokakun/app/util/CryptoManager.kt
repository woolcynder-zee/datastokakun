package com.stokakun.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts/decrypts account passwords using an AES-GCM key stored in Android Keystore.
 * Ciphertext and IV are stored together in Room; plaintext is not persisted by this class.
 */
object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "stok_akun_password_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Strict path for operations that must distinguish a real password from decryption failure. */
    fun decryptOrNull(stored: String): String? {
        if (stored.isEmpty()) return ""
        return runCatching {
            val combined = Base64.decode(stored, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH_BYTES) return null
            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            )
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrNull()
    }

    /** UI-friendly path that preserves the existing non-crashing behavior. */
    fun decrypt(stored: String): String = decryptOrNull(stored).orEmpty()
}
