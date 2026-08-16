package com.lastmilebanking.app.data.network.dto

import java.math.BigDecimal

data class RegisterRequestDto(
    val username: String,
    val password: String
)

data class RegisterResponseDto(
    val userId: String?,
    val username: String?,
    val role: String?
)

data class LoginRequestDto(
    val username: String,
    val password: String
)

data class AuthResponseDto(
    val accessToken: String?,
    val tokenType: String?,
    val expiresIn: Long?,
    val userId: String?,
    val username: String?,
    val role: String?
)

data class SyncTransactionRequestDto(
    val transactionId: String,
    val senderId: String,
    val receiverId: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentMode: String,
    val timestamp: String,
    val signature: String?
)

data class SyncTransactionResponseDto(
    val transactionId: String?,
    val status: String?,
    val message: String?
)

data class SettlementResponseDto(
    val transactionId: String?,
    val status: String?,
    val message: String?
)

data class ErrorResponseDto(
    val timestamp: String?,
    val status: Int?,
    val error: String?,
    val message: String?,
    val path: String?
)
