package com.example.finora.network.dto

import com.google.gson.annotations.SerializedName

//  Request ─

data class SetBudgetRequest(
    val category: String,
    val limit: Double,
    val month: String   // "YYYY-MM"
)

//  Response 

data class BudgetListResponse(
    val status: String,
    val budgets: List<BudgetDto>?,
    val month: String?
)

data class SingleBudgetResponse(
    val status: String,
    val budget: BudgetDto?
)

data class BudgetDto(
    @SerializedName("_id") val id: String,
    val category: String,
    val limit: Double,
    val month: String,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percentage: Int = 0
)