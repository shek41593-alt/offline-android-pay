package com.lastmilebanking.app.data.network.dto

import java.math.BigDecimal

data class CheckUserRequestDto(
    val username: String
)

data class CheckUserResponseDto(
    val status: String
)

data class VerifyOtpRequestDto(
    val username: String,
    val otp: String
)


data class RegisterRequestDto(
    val username: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val mobileNumber: String? = null,
    val email: String? = null,
    val name: String? = null,
    val address: String? = null,
    val dateOfBirth: String? = null,
    val addressLine1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pinCode: String? = null,
    val kycDocumentType: String? = null,
    val kycDocumentNumber: String? = null
)

data class RegisterResponseDto(
    val userId: String?,
    val username: String?,
    val role: String?,
    val publicPaymentId: String?
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
    val clientOperationId: String,
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

data class TransactionDetailResponseDto(
    val transactionId: String?,
    val status: String?,
    val amount: java.math.BigDecimal?,
    val currency: String?,
    val sender: TransactionParticipantDto?,
    val recipient: TransactionParticipantDto?,
    val createdAt: String?,
    val reference: String?,
    val paymentMode: String?
)

data class TransactionParticipantDto(
    val publicPaymentId: String?,
    val name: String?
)

data class ErrorResponseDto(
    val timestamp: String?,
    val status: Int?,
    val error: String?,
    val message: String?,
    val path: String?
)

data class PaymentIdentityDto(
    val userId: String?,
    val publicPaymentId: String?
)

data class UserProfileDto(
    val name: String?,
    val email: String?,
    val phone: String?,
    val address: String?
)

data class FundWalletResponseDto(
    val status: String?,
    val transactionId: String?,
    val walletBalance: java.math.BigDecimal?,
    val message: String?
)



data class RecipientDto(
    val publicPaymentId: String?,
    val name: String?,
    val phone: String?,
    val userId: String?
)

data class DirectPaymentRequestDto(
    val recipientPaymentId: String,
    val amount: java.math.BigDecimal,
    val idempotencyKey: String
)

data class DirectPaymentResponseDto(
    val transactionId: String?,
    val status: String?,
    val message: String?
)

data class WalletBalanceDto(
    val walletId: String?,
    val balance: java.math.BigDecimal,
    val currency: String?
)

data class TransactionDto(
    val transactionId: String?,
    val senderId: String?,
    val receiverId: String?,
    val amount: java.math.BigDecimal?,
    val currency: String?,
    val paymentMode: String?,
    val status: String?,
    val transactionTimestamp: String?
)

data class FundWalletRequestDto(
    val amount: java.math.BigDecimal
)
