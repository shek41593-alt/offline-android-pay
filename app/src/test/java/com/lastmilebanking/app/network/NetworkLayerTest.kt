package com.lastmilebanking.app.network

import com.google.gson.Gson
import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.dto.SyncTransactionRequestDto
import com.lastmilebanking.app.data.network.dto.SyncTransactionResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal

class NetworkLayerTest {

    private val gson = Gson()

    @Test
    fun `Serialization tests for SyncTransactionRequestDto`() {
        val dto = SyncTransactionRequestDto(
            transactionId = "tx-123",
            senderId = "user-1",
            receiverId = "user-2",
            amount = BigDecimal("100.50"),
            currency = "INR",
            paymentMode = "QR",
            timestamp = "2026-08-15T06:30:00Z",
            signature = "sig123"
        )
        val json = gson.toJson(dto)
        val map = gson.fromJson(json, Map::class.java)
        assertEquals("tx-123", map["transactionId"])
        assertEquals("user-1", map["senderId"])
        assertEquals("user-2", map["receiverId"])
        assertEquals(100.5, map["amount"])
        assertEquals("INR", map["currency"])
        assertEquals("QR", map["paymentMode"])
        assertEquals("2026-08-15T06:30:00Z", map["timestamp"])
        assertEquals("sig123", map["signature"])
    }

    @Test
    fun `Deserialization tests for SyncTransactionResponseDto`() {
        val jsonRECEIVED = """{"transactionId":"tx-123","status":"RECEIVED","message":"OK"}"""
        val response1 = gson.fromJson(jsonRECEIVED, SyncTransactionResponseDto::class.java)
        assertEquals("tx-123", response1.transactionId)
        assertEquals("RECEIVED", response1.status)

        val jsonDUPLICATE = """{"transactionId":"tx-123","status":"DUPLICATE","message":"Already processed"}"""
        val response2 = gson.fromJson(jsonDUPLICATE, SyncTransactionResponseDto::class.java)
        assertEquals("DUPLICATE", response2.status)
        
        val jsonPROCESSING = """{"transactionId":"tx-123","status":"PROCESSING","message":"Pending..."}"""
        val response3 = gson.fromJson(jsonPROCESSING, SyncTransactionResponseDto::class.java)
        assertEquals("PROCESSING", response3.status)
        
        val jsonSETTLED = """{"transactionId":"tx-123","status":"SETTLED","message":""}"""
        val response4 = gson.fromJson(jsonSETTLED, SyncTransactionResponseDto::class.java)
        assertEquals("SETTLED", response4.status)
        
        val jsonFAILED = """{"transactionId":"tx-123","status":"FAILED","message":""}"""
        val response5 = gson.fromJson(jsonFAILED, SyncTransactionResponseDto::class.java)
        assertEquals("FAILED", response5.status)
    }

    @Test
    fun `API URL tests`() = runBlocking {
        var capturedRequest: Request? = null
        
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                val responseJson = "{}"
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseJson.toResponseBody(null))
                    .build()
            }
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.43.215.250:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        val api = retrofit.create(LastMileApiService::class.java)
        
        api.register(com.lastmilebanking.app.data.network.dto.RegisterRequestDto("u", "p"))
        assertEquals("http://10.43.215.250:8080/api/v1/auth/register", capturedRequest?.url.toString())
        assertEquals("POST", capturedRequest?.method)
        
        api.login(com.lastmilebanking.app.data.network.dto.LoginRequestDto("u", "p"))
        assertEquals("http://10.43.215.250:8080/api/v1/auth/login", capturedRequest?.url.toString())
        assertEquals("POST", capturedRequest?.method)
        
        api.syncTransaction(SyncTransactionRequestDto("tx", "s", "r", BigDecimal.TEN, "INR", "QR", "ts", "sig"))
        assertEquals("http://10.43.215.250:8080/api/v1/transactions", capturedRequest?.url.toString())
        assertEquals("POST", capturedRequest?.method)
        
        api.getTransactionStatus("tx-123")
        assertEquals("http://10.43.215.250:8080/api/v1/transactions/tx-123", capturedRequest?.url.toString())
        assertEquals("GET", capturedRequest?.method)
    }
}
