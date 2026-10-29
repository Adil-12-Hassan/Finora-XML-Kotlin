package com.example.finora.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val amount: Double,
    val category: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: String = "expense",   // "expense" or "income"
    val isSynced: Boolean = true
)
