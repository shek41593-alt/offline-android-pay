package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.auth.SessionManager
import com.lastmilebanking.app.data.network.auth.TokenStorage
import com.lastmilebanking.app.data.network.dto.LoginRequestDto
import com.lastmilebanking.app.data.network.dto.RegisterRequestDto
import javax.inject.Inject
import javax.inject.Singleton

import io.appwrite.services.Account
import io.appwrite.exceptions.AppwriteException

@Singleton
class AuthenticationRepository @Inject constructor(
    private val api: LastMileApiService,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val appwriteAccount: Account
) {
    suspend fun login(phoneNumber: String, otp: String): Boolean {
        return try {
            val response = api.login(LoginRequestDto(username = phoneNumber, password = otp))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()?.accessToken
                if (!token.isNullOrEmpty()) {
                    tokenStorage.saveToken(token)
                    userRepository.seedDemoUserIfNeeded()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            if (com.lastmilebanking.app.BuildConfig.DEV_AUTH_FALLBACK_ENABLED) {
                if (phoneNumber == "9876543210" && otp == "123456") {
                    tokenStorage.saveToken("LOCAL_DEV_OFFLINE_SESSION")
                    userRepository.seedDemoUserIfNeeded()
                    return true
                }
            }
            false
        }
    }

    suspend fun register(
        firstName: String, lastName: String, 
        mobileNumber: String, password: String, 
        email: String, dob: String, 
        addressLine: String, city: String, 
        state: String, pinCode: String
    ): Boolean {
        return try {
            val request = RegisterRequestDto(
                username = mobileNumber,
                firstName = firstName,
                lastName = lastName,
                mobileNumber = mobileNumber,
                password = password,
                email = email,
                dateOfBirth = dob, // If the backend requires a specific format, we pass it dynamically
                addressLine1 = addressLine,
                city = city,
                state = state,
                pinCode = pinCode,
                kycDocumentType = "AADHAAR",
                kycDocumentNumber = "000000000000"
            )
            val response = api.register(request)
            if (response.isSuccessful && response.body() != null) {
                true
            } else {
                false
            }
        } catch (e: Exception) {
            if (com.lastmilebanking.app.BuildConfig.DEV_AUTH_FALLBACK_ENABLED) {
                return true
            }
            false
        }
    }

    suspend fun logout() {
        try {
            appwriteAccount.deleteSession("current")
        } catch (e: Exception) {
            // Ignore failure if offline or already erased
        }
        sessionManager.logout()
        tokenStorage.clearToken()
    }

    suspend fun isValidSession(): Boolean {
        if (!tokenStorage.hasToken()) {
            return false
        }
        if (com.lastmilebanking.app.BuildConfig.DEV_AUTH_FALLBACK_ENABLED) {
            if (tokenStorage.getToken() == "LOCAL_DEV_OFFLINE_SESSION") {
                return true
            }
        }
        
        return try {
            val session = appwriteAccount.getSession("current")
            session.current
        } catch (e: AppwriteException) {
            // If the error code implies network issue, we might want to default to true for offline mode.
            // But AppwriteException has codes. 401 is unauthorized (invalid session).
            if (e.code == 401) {
                tokenStorage.clearToken()
                false
            } else {
                // If it's a network error (e.g. 0 or unreachable) allow offline fallback since we have a JWT
                true
            }
        } catch (e: Exception) {
            true // Allow offline
        }
    }

    fun isAuthenticated(): Boolean {
        return tokenStorage.hasToken()
    }
}
