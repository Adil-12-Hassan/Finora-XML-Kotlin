package com.example.finora.data.dao

import androidx.room.*
import com.example.finora.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllByUser(userId: Int): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND strftime('%Y-%m', date/1000, 'unixepoch') = :monthYear
        ORDER BY date DESC
    """)
    fun getByMonth(userId: Int, monthYear: String): Flow<List<Transaction>>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE userId = :userId 
        AND type = 'expense'
        AND strftime('%Y-%m', date/1000, 'unixepoch') = :monthYear
    """)
    suspend fun getTotalSpentByMonth(userId: Int, monthYear: String): Double?

    @Query("DELETE FROM transactions WHERE userId = :userId AND strftime('%Y-%m', date/1000, 'unixepoch') = :monthYear")
    suspend fun deleteByUserAndMonth(userId: Int, monthYear: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND type = 'expense' AND isSynced = 1 AND strftime('%Y-%m', date/1000, 'unixepoch') = :monthYear")
    suspend fun deleteSyncedExpensesByMonth(userId: Int, monthYear: String)
}
