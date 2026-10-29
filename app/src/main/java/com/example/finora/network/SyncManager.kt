package com.example.finora.network

import android.content.Context
import com.example.finora.data.db.FinoraDatabase
import com.example.finora.data.model.Budget
import com.example.finora.data.model.Transaction
import com.example.finora.util.SessionManager
import java.text.SimpleDateFormat
import java.util.*

object SyncManager {

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Call this on app open — syncs current month expenses + budgets */
    suspend fun syncAll(context: Context) {
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        syncTransactions(context, month)
        syncBudgets(context, month)
    }

    suspend fun syncTransactions(context: Context, month: String) {
        val session = SessionManager(context)
        val bearer  = session.getBearerToken() ?: return
        val userId  = session.getUserId()
        if (userId == -1) return

        val db = FinoraDatabase.getDatabase(context)

        try {
            val response = RetrofitClient.api.getExpenses(bearer, month)
            if (!response.isSuccessful) return

            val expenses = response.body()?.expenses ?: return

            // Convert each API expense → Room Transaction
            val transactions = expenses.mapNotNull { dto ->
                try {
                    // Parse ISO date — handle both with and without milliseconds
                    val cleanDate = dto.date.substringBefore(".").replace("Z", "")
                    val dateMs = isoFmt.parse(cleanDate)?.time ?: System.currentTimeMillis()

                    Transaction(
                        userId   = userId,
                        amount   = dto.amount,
                        category = CategoryMapper.fromApiCategory(dto.category),
                        note     = dto.note,
                        date     = dateMs,
                        type     = "expense",
                        isSynced = true
                    )
                } catch (_: Exception) { null }
            }

            // Refresh synced server data without touching local unsynced offline transactions
            db.transactionDao().deleteSyncedExpensesByMonth(userId, month)
            transactions.forEach { db.transactionDao().insert(it) }

        } catch (_: Exception) {
            // Offline — Room data stays, UI still works
        }
    }

    suspend fun syncBudgets(context: Context, month: String) {
        val session = SessionManager(context)
        val bearer  = session.getBearerToken() ?: return
        val userId  = session.getUserId()
        if (userId == -1) return

        val db = FinoraDatabase.getDatabase(context)

        try {
            val response = RetrofitClient.api.getBudgets(bearer, month)
            if (!response.isSuccessful) return

            val budgets = response.body()?.budgets ?: return

            budgets.forEach { dto ->
                db.budgetDao().insertOrUpdate(
                    Budget(
                        userId      = userId,
                        monthYear   = dto.month,
                        category    = CategoryMapper.fromApiCategory(dto.category),
                        limitAmount = dto.limit
                    )
                )
            }
        } catch (_: Exception) {}
    }
}