package com.lastmilebanking.app.data.network.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("/api/v1/auth/login") || 
            path.contains("/api/v1/auth/register") || 
            path.contains("/api/v1/health")) {
            val response = chain.proceed(originalRequest)
            checkResponseFor401(response)
            return response
        }

        val token = tokenStorage.getToken()
        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(newRequest)
        checkResponseFor401(response)
        return response
    }

    private fun checkResponseFor401(response: Response) {
        if (response.code == 401) {
            sessionManager.handleUnauthorized()
        }
    }
}
