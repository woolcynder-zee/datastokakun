package com.stokakun.app.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.stokakun.app.data.AccountDao
import com.stokakun.app.data.AccountEntity
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.data.AppDatabase
import com.stokakun.app.data.ScreenshotDao
import com.stokakun.app.data.ScreenshotEntity
import com.stokakun.app.util.CryptoManager
import com.stokakun.app.util.ImageStorageManager
import kotlinx.coroutines.flow.Flow

class AccountRepository(
    private val context: Context,
    private val accountDao: AccountDao,
    private val screenshotDao: ScreenshotDao,
    private val database: AppDatabase
) {

    fun getFiltered(status: AccountStatus?, query: String): Flow<List<AccountEntity>> =
        accountDao.getFiltered(status?.name, query.trim())

    fun getRecent(limit: Int = 5): Flow<List<AccountEntity>> = accountDao.getRecent(limit)

    fun getAccountById(id: Long): Flow<AccountEntity?> = accountDao.getById(id)

    fun getTotalCount(): Flow<Int> = accountDao.getTotalCount()

    fun getCountByStatus(status: AccountStatus): Flow<Int> = accountDao.getCountByStatus(status.name)

    fun getActiveStockValue(): Flow<Long> = accountDao.getActiveStockValue()

    fun getScreenshots(accountId: Long): Flow<List<ScreenshotEntity>> = screenshotDao.getForAccount(accountId)

    fun getScreenshotCount(accountId: Long): Flow<Int> = screenshotDao.getCountForAccount(accountId)

    fun decryptPassword(encrypted: String): String = CryptoManager.decrypt(encrypted)

    suspend fun saveAccount(
        existingId: Long?,
        originalCreatedAt: Long?,
        game: String,
        name: String,
        price: Long,
        status: AccountStatus,
        username: String,
        plainPassword: String,
        passwordEncryptedOverride: String?,
        notes: String,
        newImageUris: List<Uri>,
        removedScreenshotIds: List<Long>
    ): Long {
        require(game.isNotBlank()) { "Game wajib diisi." }
        require(name.isNotBlank()) { "Nama / ID stok wajib diisi." }
        require(price >= 0) { "Harga tidak boleh negatif." }

        val normalizedGame = game.trim()
        val normalizedName = name.trim()
        val normalizedUsername = username.trim()
        val encryptedPassword = passwordEncryptedOverride ?: CryptoManager.encrypt(plainPassword)
        val copiedFiles = mutableListOf<String>()

        try {
            return database.withTransaction {
                val duplicate = accountDao.findDuplicate(normalizedGame, normalizedName, normalizedUsername)
                if (duplicate != null && duplicate.id != existingId) {
                    throw IllegalArgumentException("Akun dengan game, nama/ID, dan username yang sama sudah ada.")
                }

                val accountId = if (existingId == null) {
                    accountDao.insert(
                        AccountEntity(
                            game = normalizedGame,
                            name = normalizedName,
                            price = price,
                            status = status,
                            username = normalizedUsername,
                            passwordEncrypted = encryptedPassword,
                            notes = notes.trim(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    accountDao.update(
                        AccountEntity(
                            id = existingId,
                            game = normalizedGame,
                            name = normalizedName,
                            price = price,
                            status = status,
                            username = normalizedUsername,
                            passwordEncrypted = encryptedPassword,
                            notes = notes.trim(),
                            createdAt = originalCreatedAt ?: System.currentTimeMillis()
                        )
                    )
                    existingId
                }

                if (removedScreenshotIds.isNotEmpty()) {
                    val all = screenshotDao.getForAccountOnce(accountId)
                    all.filter { it.id in removedScreenshotIds }.forEach { shot ->
                        ImageStorageManager.deleteFile(shot.filePath)
                        screenshotDao.delete(shot)
                    }
                }

                if (newImageUris.isNotEmpty()) {
                    val nextSortOrder = screenshotDao.getForAccountOnce(accountId)
                        .maxOfOrNull { it.sortOrder }
                        ?.plus(1) ?: 0

                    val newEntities = newImageUris.mapIndexedNotNull { index, uri ->
                        val path = ImageStorageManager.copyImageToLocalStorage(context, uri)
                            ?: throw IllegalArgumentException("Gagal membaca salah satu screenshot.")
                        copiedFiles += path
                        ScreenshotEntity(
                            accountId = accountId,
                            filePath = path,
                            createdAt = System.currentTimeMillis(),
                            sortOrder = nextSortOrder + index
                        )
                    }
                    screenshotDao.insertAll(newEntities)
                }

                accountId
            }
        } catch (e: Throwable) {
            // Room rolls back database writes; remove files that were copied before a failure.
            ImageStorageManager.deleteFiles(copiedFiles)
            throw e
        }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        database.withTransaction {
            val shots = screenshotDao.getForAccountOnce(account.id)
            ImageStorageManager.deleteFiles(shots.map { it.filePath })
            screenshotDao.deleteAllForAccount(account.id)
            accountDao.delete(account)
        }
    }
}
