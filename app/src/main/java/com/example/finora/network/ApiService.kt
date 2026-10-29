package com.example.finora.network

import com.example.finora.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>
    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") bearer: String): Response<MessageResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") bearer: String): Response<GetMeResponse>

    @PUT("api/auth/me")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body body: UpdateProfileRequest
    ): Response<AuthResponse>

    @PUT("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") bearer: String,
        @Body body: ChangePasswordRequest
    ): Response<MessageResponse>

    // ── Expenses 
    @GET("api/expenses")
    suspend fun getExpenses(
        @Header("Authorization") bearer: String,
        @Query("month") month: String
    ): Response<ExpenseListResponse>

    @POST("api/expenses")
    suspend fun addExpense(
        @Header("Authorization") bearer: String,
        @Body body: AddExpenseRequest
    ): Response<SingleExpenseResponse>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): Response<MessageResponse>

    // ── Budgets ─
    @GET("api/budgets")
    suspend fun getBudgets(
        @Header("Authorization") bearer: String,
        @Query("month") month: String
    ): Response<BudgetListResponse>

    @POST("api/budgets")
    suspend fun setBudget(
        @Header("Authorization") bearer: String,
        @Body body: SetBudgetRequest
    ): Response<SingleBudgetResponse>

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): Response<MessageResponse>

    @GET("api/version")
    suspend fun checkVersion(): Response<VersionResponse>

}