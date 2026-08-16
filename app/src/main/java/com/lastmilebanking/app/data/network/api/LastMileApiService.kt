package com.lastmilebanking.app.data.network.api

import com.lastmilebanking.app.data.network.dto.AuthResponseDto
import com.lastmilebanking.app.data.network.dto.LoginRequestDto
import com.lastmilebanking.app.data.network.dto.RegisterRequestDto
import com.lastmilebanking.app.data.network.dto.RegisterResponseDto
import com.lastmilebanking.app.data.network.dto.SyncTransactionRequestDto
import com.lastmilebanking.app.data.network.dto.SyncTransactionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LastMileApiService {
    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<RegisterResponseDto>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("/api/v1/transactions")
    suspend fun syncTransaction(@Body request: SyncTransactionRequestDto): Response<SyncTransactionResponseDto>

    @GET("/api/v1/transactions/{transactionId}")
    suspend fun getTransactionStatus(@Path("transactionId") transactionId: String): Response<SyncTransactionResponseDto>

    @GET("/api/v1/health")
    suspend fun checkHealth(): Response<okhttp3.ResponseBody>
}
