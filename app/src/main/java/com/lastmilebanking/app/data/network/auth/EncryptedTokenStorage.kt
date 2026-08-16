package com.lastmilebanking.app.data.network.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedTokenStorage @Inject constructor(
    @ApplicationContext context: Context
) : TokenStorage {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secured_auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val TOKEN_KEY = "jwt_token"

    override fun saveToken(token: String) {
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }

    override fun getToken(): String? {
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    override fun clearToken() {
        sharedPreferences.edit().remove(TOKEN_KEY).apply()
    }

    override fun hasToken(): Boolean {
        return sharedPreferences.contains(TOKEN_KEY)
    }
}
