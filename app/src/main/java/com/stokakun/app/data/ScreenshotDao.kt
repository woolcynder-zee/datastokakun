package com.stokakun.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots WHERE accountId = :accountId ORDER BY sortOrder ASC")
    fun getForAccount(accountId: Long): Flow<List<ScreenshotEntity>>

    @Query("SELECT COUNT(*) FROM screenshots WHERE accountId = :accountId")
    fun getCountForAccount(accountId: Long): Flow<Int>

    @Query("SELECT * FROM screenshots WHERE accountId = :accountId ORDER BY sortOrder ASC")
    suspend fun getForAccountOnce(accountId: Long): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE accountId IN (:accountIds) ORDER BY accountId ASC, sortOrder ASC")
    suspend fun getForAccountsOnce(accountIds: List<Long>): List<ScreenshotEntity>

    @Insert
    suspend fun insert(screenshot: ScreenshotEntity): Long

    @Insert
    suspend fun insertAll(screenshots: List<ScreenshotEntity>)

    @Delete
    suspend fun delete(screenshot: ScreenshotEntity)

    @Query("DELETE FROM screenshots WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: Long)

    @Query("DELETE FROM screenshots WHERE accountId IN (:accountIds)")
    suspend fun deleteAllForAccounts(accountIds: List<Long>)

    @Query("SELECT * FROM screenshots")
    suspend fun getAllOnce(): List<ScreenshotEntity>
}
