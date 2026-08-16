package com.lastmilebanking.app.di

import com.lastmilebanking.app.data.network.auth.EncryptedTokenStorage
import com.lastmilebanking.app.data.network.auth.TokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindTokenStorage(impl: EncryptedTokenStorage): TokenStorage
}
