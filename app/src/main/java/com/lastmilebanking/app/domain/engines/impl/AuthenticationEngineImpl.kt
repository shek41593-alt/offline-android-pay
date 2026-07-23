package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.data.repository.UserRepository
import com.lastmilebanking.app.domain.engines.AuthenticationEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationEngineImpl @Inject constructor(
    private val userRepository: UserRepository
) : AuthenticationEngine {

    override suspend fun login(phoneNumber: String): Result<Boolean> {
        return try {
            val user = userRepository.getUserByPhone(phoneNumber)
            if (user != null) {
                // In production, we'd send an OTP here.
                Result.success(true)
            } else {
                Result.failure(Exception("User not found with phone: $phoneNumber"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(otp: String): Result<Boolean> {
        // For testing/MVP, accept any OTP with 4 or more digits
        return if (otp.isNotEmpty() && otp.length >= 4) {
             Result.success(true)
        } else {
             Result.failure(Exception("Invalid OTP"))
        }
    }

    override suspend fun register(details: Map<String, String>): Result<Boolean> {
        return try {
            val phone = details["phoneNumber"] ?: return Result.failure(Exception("Phone required"))
            val name = details["name"] ?: return Result.failure(Exception("Name required"))
            
            val user = userRepository.getUserByPhone(phone)
            if (user != null) {
                return Result.failure(Exception("User already exists"))
            }

            userRepository.createUser(
                name = name,
                phoneNumber = phone,
                accountNumber = details["accountNumber"] ?: "1000000000",
                ifscCode = details["ifscCode"] ?: "LMB0001",
                bankName = details["bankName"] ?: "Last Mile Bank"
            )
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        // Clear active session/DataStore
        return Result.success(Unit)
    }

    override fun getSessionToken(): String? {
        // Return dummy token for MVP
        return "LMB-SIMULATED-TOKEN-V1"
    }
}
