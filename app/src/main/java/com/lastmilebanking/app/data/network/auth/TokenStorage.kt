package com.lastmilebanking.app.data.network.auth

interface TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
    fun hasToken(): Boolean
}
