package com.lastmilebanking.app.domain.engines

interface AuthenticationEngine {
    suspend fun login(phoneNumber: String): Result<Boolean>
    suspend fun verifyOtp(otp: String): Result<Boolean>
    suspend fun register(details: Map<String, String>): Result<Boolean>
    suspend fun logout(): Result<Unit>
    fun getSessionToken(): String?
}
