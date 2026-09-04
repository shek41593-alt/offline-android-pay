package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.auth.SessionManager
import com.lastmilebanking.app.data.network.auth.TokenStorage
import com.lastmilebanking.app.data.network.dto.LoginRequestDto
import com.lastmilebanking.app.data.network.dto.RegisterRequestDto
import android.util.Log
import java.net.ConnectException
import java.net.SocketTimeoutException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthenticationRepository @Inject constructor(
    private val api: LastMileApiService,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val database: com.lastmilebanking.app.data.local.LastMileDatabase
) {
    suspend fun checkUser(phoneNumber: String): Boolean {
        // We will throw exceptions for network/server errors so ViewModel can catch them
        val response = api.checkUser(com.lastmilebanking.app.data.network.dto.CheckUserRequestDto(username = phoneNumber))
        if (response.isSuccessful) {
            return response.body()?.status == "EXISTING"
        } else {
            throw retrofit2.HttpException(response)
        }
    }

    suspend fun verifyOtp(phoneNumber: String, otp: String): Boolean {
        return try {
            val response = api.verifyOtp(com.lastmilebanking.app.data.network.dto.VerifyOtpRequestDto(username = phoneNumber, otp = otp))
            handleAuthResponse(response, phoneNumber)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Verify OTP unexpected exception: ${e.message}", e)
            false
        }
    }

    suspend fun login(phoneNumber: String, password: String): Boolean {
        return try {
            val response = api.login(LoginRequestDto(username = phoneNumber, password = password))
            handleAuthResponse(response, phoneNumber)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login unexpected exception: ${e.message}", e)
            false
        }
    }

    private suspend fun handleAuthResponse(response: retrofit2.Response<com.lastmilebanking.app.data.network.dto.AuthResponseDto>, phoneNumber: String): Boolean {
        return if (response.isSuccessful && response.body() != null) {
            val token = response.body()?.accessToken
                if (!token.isNullOrEmpty()) {
                    tokenStorage.saveToken(token)
                    response.body()?.userId?.let { userId ->
                        userRepository.setActiveUser(userId)
                    }
                    
                    // Fetch Payment Identity to ensure local cache is updated on login
                    try {
                        val identityResponse = api.getPaymentIdentity()
                        val profileResponse = api.getProfile()
                        
                        if (identityResponse.isSuccessful) {
                            val identityUserId = identityResponse.body()?.userId
                            val paymentId = identityResponse.body()?.publicPaymentId
                            
                            val profileName = profileResponse.body()?.name ?: response.body()?.username ?: "User"
                            val profilePhone = profileResponse.body()?.phone ?: phoneNumber
                            
                            if (identityUserId != null && paymentId != null) {
                                // Update or recreate the user entity in Room to restore session state if missing
                                val existingUser = userRepository.getUserById(identityUserId)
                                if (existingUser == null) {
                                    userRepository.createUser(
                                        userId = identityUserId,
                                        name = profileName,
                                        phoneNumber = profilePhone,
                                        accountNumber = "12345678901234",
                                        ifscCode = "SBIN0001234",
                                        bankName = "State Bank",
                                        publicPaymentId = paymentId
                                    )
                                } else {
                                    // Reactivate the user here just in case they were logged out but not cleared
                                    userRepository.setActiveUser(identityUserId)
                                }
                            }
                        }
                    } catch(e: Exception) {
                        Log.e("AuthRepository", "Failed to get payment identity during login", e)
                    }
                    true
                } else {
                    false
                }
            } else {
                Log.e("AuthRepository", "Auth failed: ${response.code()} ${response.errorBody()?.string()}")
                false
            }
    }

    suspend fun register(
        firstName: String, lastName: String, 
        mobileNumber: String, password: String, 
        email: String, dob: String, 
        addressLine: String, city: String, 
        state: String, pinCode: String
    ): RegistrationResult {
        return try {
            val fullName = "$firstName $lastName".trim()
            val fullAddress = listOf(addressLine, city, state, pinCode).filter { it.isNotBlank() }.joinToString(", ")
            val request = RegisterRequestDto(
                username = mobileNumber,
                password = password,
                email = email,
                name = fullName,
                address = fullAddress
            )
            val response = api.register(request)
            if (response.isSuccessful && response.body() != null) {
                val backendUserId = response.body()?.userId
                val paymentId = response.body()?.publicPaymentId
                if (backendUserId != null) {
                    userRepository.createUser(
                        userId = backendUserId,
                        name = "$firstName $lastName".trim(),
                        phoneNumber = mobileNumber,
                        accountNumber = "12345678901234",
                        ifscCode = "SBIN0001234",
                        bankName = "State Bank of India",
                        publicPaymentId = paymentId
                    )
                }
                RegistrationResult.Success
            } else {
                Log.e("AuthRepository", "Register failed: ${response.code()} ${response.errorBody()?.string()}")
                when (response.code()) {
                    409 -> RegistrationResult.Conflict
                    400 -> RegistrationResult.ValidationError("Invalid registration details.") // Parse safely if needed
                    in 500..599 -> RegistrationResult.ServerError
                    else -> RegistrationResult.NetworkError
                }
            }
        } catch (e: ConnectException) {
            Log.e("AuthRepository", "Register connection refused/failed. Server unreachable.", e)
            RegistrationResult.NetworkError
        } catch (e: SocketTimeoutException) {
            Log.e("AuthRepository", "Register request timed out.", e)
            RegistrationResult.NetworkError
        } catch (e: HttpException) {
            Log.e("AuthRepository", "Register HTTP Exception: ${e.code()} ${e.response()?.errorBody()?.string()}", e)
            RegistrationResult.NetworkError
        } catch (e: Exception) {
            Log.e("AuthRepository", "Register unexpected exception: ${e.message}", e)
            RegistrationResult.ServerError
        }
    }

    suspend fun logout() {
        sessionManager.logout()
        tokenStorage.clearToken()
        userRepository.clearActiveUser()
        
        // Clear cached wallet/history data on logout for absolute isolation
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    suspend fun isValidSession(): Boolean {
        if (!tokenStorage.hasToken()) {
            return false
        }
        
        
        return true
    }

    fun isAuthenticated(): Boolean {
        return tokenStorage.hasToken()
    }
}

sealed class RegistrationResult {
    object Success : RegistrationResult()
    object Conflict : RegistrationResult()
    data class ValidationError(val message: String) : RegistrationResult()
    object ServerError : RegistrationResult()
    object NetworkError : RegistrationResult()
}
