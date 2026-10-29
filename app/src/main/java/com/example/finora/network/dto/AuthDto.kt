package com.example.finora.network.dto

import com.google.gson.annotations.SerializedName

//  Request bodies 

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class UpdateProfileRequest(
    val name: String? = null,
    val currency: String? = null,
    val monthlyBudget: Float? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

//  Response bodies ──

data class AuthResponse(
    val status: String,
    val message: String,
    val token: String?,
    val user: UserDto?
)

data class UserDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val email: String,
    val currency: String,
    val monthlyBudget: Float
)

data class GetMeResponse(
    val status: String,
    val user: UserDto?
)

data class MessageResponse(
    val status: String,
    val message: String
)