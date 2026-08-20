package com.lastmilebanking.app.di

import android.util.Log
import com.lastmilebanking.app.BuildConfig
import com.lastmilebanking.app.data.network.api.LastMileApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://10.76.75.250:8080/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: com.lastmilebanking.app.data.network.auth.AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val safeLogger = object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    if (message.contains("Bearer", ignoreCase = true) ||
                        message.contains("\"password\"", ignoreCase = true) ||
                        message.contains("\"signature\"", ignoreCase = true) ||
                        message.contains("accessToken", ignoreCase = true) ||
                        message.contains("Authorization", ignoreCase = true)) {
                        Log.d("OkHttp", "[REDACTED]")
                    } else {
                        Log.d("OkHttp", message)
                    }
                }
            }
            val loggingInterceptor = HttpLoggingInterceptor(safeLogger).apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLastMileApiService(retrofit: Retrofit): LastMileApiService {
        return retrofit.create(LastMileApiService::class.java)
    }
}
