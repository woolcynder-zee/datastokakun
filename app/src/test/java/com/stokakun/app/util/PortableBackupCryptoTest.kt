package com.stokakun.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PortableBackupCryptoTest {

    @Test
    fun roundTripRestoresPlaintext() {
        val plaintext = "GamePassword-123!"
        val backupPassword = "BackupPass123"

        val token = PortableBackupCrypto.encrypt(plaintext, backupPassword)

        assertEquals(plaintext, PortableBackupCrypto.decrypt(token, backupPassword))
    }

    @Test
    fun encryptionUsesFreshRandomValues() {
        val plaintext = "same-password"
        val backupPassword = "BackupPass123"

        val first = PortableBackupCrypto.encrypt(plaintext, backupPassword)
        val second = PortableBackupCrypto.encrypt(plaintext, backupPassword)

        assertNotEquals(first, second)
        assertEquals(plaintext, PortableBackupCrypto.decrypt(first, backupPassword))
        assertEquals(plaintext, PortableBackupCrypto.decrypt(second, backupPassword))
    }

    @Test
    fun wrongPasswordFails() {
        val token = PortableBackupCrypto.encrypt("secret", "BackupPass123")

        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.decrypt(token, "WrongPass123")
        }
    }

    @Test
    fun corruptedTokenFails() {
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.decrypt("1:not-valid:token", "BackupPass123")
        }
    }

    @Test
    fun shortBackupPasswordIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.encrypt("secret", "1234567")
        }
    }
}
