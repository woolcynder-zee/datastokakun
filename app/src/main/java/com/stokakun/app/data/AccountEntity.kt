package com.stokakun.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountStatus {
    AVAILABLE, RESERVED, SOLD
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val game: String,
    val name: String,
    val price: Long,
    val status: AccountStatus,
    val username: String,
    // Stored encrypted (Android Keystore AES-GCM), never logged, never shown in lists.
    val passwordEncrypted: String,
    val notes: String,
    val createdAt: Long
)
