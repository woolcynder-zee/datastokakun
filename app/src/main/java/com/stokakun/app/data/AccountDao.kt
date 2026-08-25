package com.stokakun.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query(
        """
        SELECT * FROM accounts
        WHERE (:status IS NULL OR status = :status)
        AND (:query = '' OR game LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
        """
    )
    fun getFiltered(status: String?, query: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getById(id: Long): Flow<AccountEntity?>

    @Query("SELECT COUNT(*) FROM accounts")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM accounts WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts")
    suspend fun getAllOnce(): List<AccountEntity>
}
