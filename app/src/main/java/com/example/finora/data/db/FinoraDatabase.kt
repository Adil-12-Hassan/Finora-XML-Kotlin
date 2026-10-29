package com.example.finora.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.finora.data.dao.TransactionDao
import com.example.finora.data.dao.UserDao
import com.example.finora.data.model.Transaction
import com.example.finora.data.model.User
import com.example.finora.data.dao.BudgetDao
import com.example.finora.data.model.Budget

@Database(entities = [User::class, Transaction::class, Budget::class], version = 3, exportSchema = false)
abstract class FinoraDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var INSTANCE: FinoraDatabase? = null

        fun getDatabase(context: Context): FinoraDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FinoraDatabase::class.java,
                    "finora_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
