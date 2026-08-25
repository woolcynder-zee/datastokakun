package com.stokakun.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE (:status IS NULL OR status = :status) AND (:query = '' OR game LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun getFiltered(status: String?, query: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getById(id: Long): Flow<AccountEntity?>

    @Query("SELECT COUNT(*) FROM accounts")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM accounts WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(price), 0) FROM accounts WHERE status != 'SOLD'")
    fun getActiveStockValue(): Flow<Long>

    @Query("SELECT COALESCE(SUM(price), 0) FROM accounts WHERE status = 'SOLD'")
    fun getSoldStockValue(): Flow<Long>

    @Query("SELECT game, COUNT(*) AS total, SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available, SUM(CASE WHEN status = 'RESERVED' THEN 1 ELSE 0 END) AS reserved, SUM(CASE WHEN status = 'SOLD' THEN 1 ELSE 0 END) AS sold, COALESCE(SUM(CASE WHEN status != 'SOLD' THEN price ELSE 0 END), 0) AS activeValue FROM accounts GROUP BY game ORDER BY total DESC, game COLLATE NOCASE ASC")
    fun getGameStats(): Flow<List<GameStat>>

    @Query("SELECT * FROM accounts WHERE game = :game AND name = :name AND username = :username LIMIT 1")
    suspend fun findDuplicate(game: String, name: String, username: String): AccountEntity?

    @Insert suspend fun insert(account: AccountEntity): Long
    @Update suspend fun update(account: AccountEntity)
    @Delete suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts")
    suspend fun getAllOnce(): List<AccountEntity>

    @Query("UPDATE accounts SET status = :status WHERE id IN (:ids)")
    suspend fun updateStatus(ids: List<Long>, status: AccountStatus): Int

    @Query("SELECT * FROM accounts WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): AccountEntity?
}
