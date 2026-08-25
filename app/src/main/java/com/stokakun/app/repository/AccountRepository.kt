package com.stokakun.app.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.stokakun.app.data.AccountDao
import com.stokakun.app.data.AccountEntity
import com.stokakun.app.data.AccountStatus
import com.stokakun.app.data.AppDatabase
import com.stokakun.app.data.GameStat
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
    fun getFiltered(status: AccountStatus?, query: String): Flow<List<AccountEntity>> = accountDao.getFiltered(status?.name, query.trim())
    fun getRecent(limit: Int = 5): Flow<List<AccountEntity>> = accountDao.getRecent(limit)
    fun getAccountById(id: Long): Flow<AccountEntity?> = accountDao.getById(id)
    fun getTotalCount(): Flow<Int> = accountDao.getTotalCount()
    fun getCountByStatus(status: AccountStatus): Flow<Int> = accountDao.getCountByStatus(status.name)
    fun getActiveStockValue(): Flow<Long> = accountDao.getActiveStockValue()
    fun getSoldStockValue(): Flow<Long> = accountDao.getSoldStockValue()
    fun getGameStats(): Flow<List<GameStat>> = accountDao.getGameStats()
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
        val filesToDelete = mutableListOf<String>()

        return try {
            // File I/O happens before the Room transaction so slow/large image copies do not hold a DB transaction open.
            val stagedImages = newImageUris.mapIndexed { index, uri ->
                val path = ImageStorageManager.copyImageToLocalStorage(context, uri)
                    ?: throw IllegalArgumentException("Gagal membaca salah satu screenshot.")
                copiedFiles += path
                path
            }

            val accountId = database.withTransaction {
                val duplicate = accountDao.findDuplicate(normalizedGame, normalizedName, normalizedUsername)
                if (duplicate != null && duplicate.id != existingId) {
                    throw IllegalArgumentException("Akun dengan game, nama/ID, dan username yang sama sudah ada.")
                }

                val id = if (existingId == null) {
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
                    screenshotDao.getForAccountOnce(id)
                        .filter { it.id in removedScreenshotIds }
                        .forEach { shot ->
                            filesToDelete += shot.filePath
                            screenshotDao.delete(shot)
                        }
                }

                if (stagedImages.isNotEmpty()) {
                    val nextSortOrder = screenshotDao.getForAccountOnce(id).maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
                    screenshotDao.insertAll(
                        stagedImages.mapIndexed { index, path ->
                            ScreenshotEntity(
                                accountId = id,
                                filePath = path,
                                createdAt = System.currentTimeMillis(),
                                sortOrder = nextSortOrder + index
                            )
                        }
                    )
                }
                id
            }

            ImageStorageManager.deleteFiles(filesToDelete)
            accountId
        } catch (e: Throwable) {
            ImageStorageManager.deleteFiles(copiedFiles)
            throw e
        }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        val filesToDelete = database.withTransaction {
            val shots = screenshotDao.getForAccountOnce(account.id)
            screenshotDao.deleteAllForAccount(account.id)
            accountDao.delete(account)
            shots.map { it.filePath }
        }
        ImageStorageManager.deleteFiles(filesToDelete)
    }

    suspend fun bulkUpdateStatus(ids: List<Long>, status: AccountStatus): Int {
        if (ids.isEmpty()) return 0
        return database.withTransaction { accountDao.updateStatus(ids, status) }
    }

    suspend fun bulkDelete(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        val targetIds = ids.distinct()
        val filesToDelete = database.withTransaction {
            val shots = screenshotDao.getForAccountsOnce(targetIds)
            targetIds.forEach { screenshotDao.deleteAllForAccount(it) }
            accountDao.getByIds(targetIds).forEach { accountDao.delete(it) }
            shots.map { it.filePath }
        }
        ImageStorageManager.deleteFiles(filesToDelete)
        return targetIds.size
    }
}
