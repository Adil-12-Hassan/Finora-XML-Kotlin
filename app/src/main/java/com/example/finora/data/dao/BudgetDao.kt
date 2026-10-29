package com.example.finora.data.dao

import androidx.room.*
import com.example.finora.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: Budget)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear")
    fun getBudgetsByMonth(userId: Int, monthYear: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear AND category = :category LIMIT 1")
    suspend fun getBudgetForCategory(userId: Int, monthYear: String, category: String): Budget?

    @Delete
    suspend fun delete(budget: Budget)
}