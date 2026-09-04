package com.lastmilebanking.app

import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.dto.RegisterRequestDto
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegistrationTest {
    @Test
    fun testRegistration() = runBlocking {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:8080/") // Using localhost for local test execution
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val api = retrofit.create(LastMileApiService::class.java)
        
        val request = RegisterRequestDto(
            username = "9876543210",
            password = "TestPassword123!"
        )
        try {
            val response = api.register(request)
            println("=== REGISTRATION RESULT ===")
            println("HTTP STATUS: ${response.code()}")
            println("HTTP METHOD: POST")
            println("REQUEST URL: ${response.raw().request.url}")
            println("IS SUCCESSFUL: ${response.isSuccessful}")
            println("RESPONSE BODY: ${response.body()}")
            println("ERROR BODY: ${response.errorBody()?.string()}")
            println("===========================")
        } catch (e: Exception) {
            println("=== REGISTRATION EXCEPTION ===")
            println("EXCEPTION: ${e.message}")
            e.printStackTrace()
            println("===========================")
        }
    }
}
