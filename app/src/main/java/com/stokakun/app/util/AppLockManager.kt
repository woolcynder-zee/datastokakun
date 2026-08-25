package com.stokakun.app.util

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppLockManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isEnabled: Boolean get() = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)
    val remainingLockoutSeconds: Int
        get() {
            val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
            val remaining = (until - System.currentTimeMillis()).coerceAtLeast(0L)
            return (remaining / 1000L).toInt().let { seconds ->
                if (remaining > 0L && seconds == 0) 1 else seconds
            }
        }

    fun setPin(pin: String) {
        require(pin.length in 4..8 && pin.all(Char::isDigit)) { "PIN harus 4–8 digit." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!isEnabled || remainingLockoutSeconds > 0) return false
        val saltText = prefs.getString(KEY_SALT, null) ?: return false
        val hashText = prefs.getString(KEY_HASH, null) ?: return false
        val valid = runCatching {
            val salt = Base64.decode(saltText, Base64.NO_WRAP)
            val expected = Base64.decode(hashText, Base64.NO_WRAP)
            java.security.MessageDigest.isEqual(derive(pin, salt), expected)
        }.getOrDefault(false)
        if (valid) {
            prefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKOUT_UNTIL).apply()
        } else {
            registerFailedAttempt()
        }
        return valid
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    private fun registerFailedAttempt() {
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_MS)
                .apply()
        } else {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
        }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS = "app_security"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MS = 30_000L
    }
}
