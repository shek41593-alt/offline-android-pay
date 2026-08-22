package com.lastmilebanking.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.appwrite.Client
import io.appwrite.services.Account
import javax.inject.Singleton

object AppwriteConfig {
    const val PROJECT_ID = "6a8984d00013474ae520"
    const val PROJECT_NAME = "Last Mile Banking"
    const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
}

@Module
@InstallIn(SingletonComponent::class)
object AppwriteModule {

    @Provides
    @Singleton
    fun provideAppwriteClient(@ApplicationContext context: Context): Client {
        return Client(context)
            .setEndpoint(AppwriteConfig.ENDPOINT)
            .setProject(AppwriteConfig.PROJECT_ID)
    }

    @Provides
    @Singleton
    fun provideAppwriteAccount(client: Client): Account {
        return Account(client)
    }
}
