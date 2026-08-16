package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.auth.SessionManager
import com.lastmilebanking.app.data.network.auth.TokenStorage
import com.lastmilebanking.app.data.network.dto.LoginRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationRepository @Inject constructor(
    private val api: LastMileApiService,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository
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
            false
        }
    }

    fun logout() {
        sessionManager.logout()
    }

    fun isAuthenticated(): Boolean {
        return tokenStorage.hasToken()
    }
}
