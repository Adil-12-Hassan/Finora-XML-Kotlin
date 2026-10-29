package com.example.finora.network.dto

import com.google.gson.annotations.SerializedName

//  Request ─

data class AddExpenseRequest(
    val amount: Double,
    val category: String,   // must be one of web's enum values
    val date: String,       // ISO-8601 "2026-10-25T00:00:00.000Z"
    val note: String = ""
)

//  Response 

data class ExpenseListResponse(
    val status: String,
    val expenses: List<ExpenseDto>?,
    val pagination: PaginationDto?
)

data class SingleExpenseResponse(
    val status: String,
    val expense: ExpenseDto?
)

data class ExpenseDto(
    @SerializedName("_id") val id: String,
    val userId: String,
    val amount: Double,
    val category: String,
    val date: String,
    val note: String
)

data class PaginationDto(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)