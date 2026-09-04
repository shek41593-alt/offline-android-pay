package com.lastmilebanking.app.data.network.api

import com.lastmilebanking.app.data.network.dto.AuthResponseDto
import com.lastmilebanking.app.data.network.dto.LoginRequestDto
import com.lastmilebanking.app.data.network.dto.RegisterRequestDto
import com.lastmilebanking.app.data.network.dto.RegisterResponseDto
import com.lastmilebanking.app.data.network.dto.SyncTransactionRequestDto
import com.lastmilebanking.app.data.network.dto.DirectPaymentRequestDto
import com.lastmilebanking.app.data.network.dto.DirectPaymentResponseDto
import com.lastmilebanking.app.data.network.dto.PaymentIdentityDto
import com.lastmilebanking.app.data.network.dto.RecipientDto
import com.lastmilebanking.app.data.network.dto.WalletBalanceDto
import com.lastmilebanking.app.data.network.dto.SyncTransactionResponseDto
import com.lastmilebanking.app.data.network.dto.VerifyOtpRequestDto
import com.lastmilebanking.app.data.network.dto.CheckUserRequestDto
import com.lastmilebanking.app.data.network.dto.CheckUserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Header

interface LastMileApiService {
    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("/api/v1/auth/check-user")
    suspend fun checkUser(@Body request: CheckUserRequestDto): Response<CheckUserResponseDto>

    @POST("/api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequestDto): Response<AuthResponseDto>

    @POST("/api/v1/wallet/sync")
    suspend fun syncTransaction(@Body request: SyncTransactionRequestDto): Response<SyncTransactionResponseDto>

    @POST("/api/v1/wallet/fund")
    suspend fun fundWallet(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: com.lastmilebanking.app.data.network.dto.FundWalletRequestDto
    ): Response<com.lastmilebanking.app.data.network.dto.FundWalletResponseDto>

    @GET("/api/v1/transactions/{transactionId}")
    suspend fun getTransactionStatus(@Path("transactionId") transactionId: String): Response<SyncTransactionResponseDto>

    @GET("/api/v1/transactions/{transactionId}")
    suspend fun getTransactionDetail(@Path("transactionId") transactionId: String): Response<com.lastmilebanking.app.data.network.dto.TransactionDetailResponseDto>

    @GET("/api/v1/health")
    suspend fun checkHealth(): Response<okhttp3.ResponseBody>

    @GET("/api/v1/profile/payment-identity")
    suspend fun getPaymentIdentity(): Response<PaymentIdentityDto>

    @GET("/api/v1/profile")
    suspend fun getProfile(): Response<com.lastmilebanking.app.data.network.dto.UserProfileDto>

    @GET("/api/v1/payment-users/{publicPaymentId}")
    suspend fun resolveRecipient(@Path("publicPaymentId") publicPaymentId: String): Response<RecipientDto>

    @POST("/api/v1/payments")
    suspend fun processPayment(@Body request: DirectPaymentRequestDto): Response<DirectPaymentResponseDto>

    @GET("/api/v1/wallet/balance")
    suspend fun getWalletBalance(): Response<WalletBalanceDto>
    
    @GET("/api/v1/transactions")
    suspend fun getUserTransactions(): Response<List<com.lastmilebanking.app.data.network.dto.TransactionDto>>
}
