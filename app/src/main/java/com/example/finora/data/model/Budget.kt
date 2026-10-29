package com.example.finora.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val monthYear: String,        // "2026-10"
    val category: String,
    val limitAmount: Double
)