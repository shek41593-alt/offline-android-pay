package com.lastmilebanking.app.network

import com.lastmilebanking.app.data.network.auth.AuthInterceptor
import com.lastmilebanking.app.data.network.auth.SessionManager
import com.lastmilebanking.app.data.network.auth.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class FakeTokenStorage : TokenStorage {
    private var token: String? = null
    
    override fun saveToken(token: String) {
        this.token = token
    }
    
    override fun getToken(): String? {
        return token
    }
    
    override fun clearToken() {
        token = null
    }

    override fun hasToken(): Boolean {
        return !token.isNullOrEmpty()
    }
}

class AuthInterceptorTest {
    
    private lateinit var tokenStorage: TokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var authInterceptor: AuthInterceptor
    
    @Before
    fun setup() {
        tokenStorage = FakeTokenStorage()
        sessionManager = SessionManager(tokenStorage)
        authInterceptor = AuthInterceptor(tokenStorage, sessionManager)
    }

    @Test
    fun `Test saveToken, getToken, clearToken, missingToken, replacementToken`() {
        assertNull(tokenStorage.getToken())
        assertFalse(tokenStorage.hasToken())

        tokenStorage.saveToken("token1")
        assertEquals("token1", tokenStorage.getToken())
        assertTrue(tokenStorage.hasToken())

        tokenStorage.saveToken("token2")
        assertEquals("token2", tokenStorage.getToken())
        assertTrue(tokenStorage.hasToken())

        tokenStorage.clearToken()
        assertNull(tokenStorage.getToken())
        assertFalse(tokenStorage.hasToken())
    }

    @Test
    fun `Test missing token`() {
        tokenStorage.clearToken()
        
        var capturedRequest: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody(null))
                    .build()
            }
            .build()
            
        val req = Request.Builder().url("http://localhost/api/v1/protected").build()
        client.newCall(req).execute()
        
        assertNull(capturedRequest?.header("Authorization"))
    }
    
    @Test
    fun `Test valid token`() {
        tokenStorage.saveToken("valid-token-123")
        
        var capturedRequest: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody(null))
                    .build()
            }
            .build()
            
        val req = Request.Builder().url("http://localhost/api/v1/protected").build()
        client.newCall(req).execute()
        
        assertEquals("Bearer valid-token-123", capturedRequest?.header("Authorization"))
    }
    
    @Test
    fun `Test public endpoints do not get token`() {
        tokenStorage.saveToken("valid-token-123")
        
        var capturedRequest: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody(null))
                    .build()
            }
            .build()
            
        val req = Request.Builder().url("http://localhost/api/v1/auth/login").build()
        client.newCall(req).execute()
        
        assertNull(capturedRequest?.header("Authorization"))
    }
    
    @Test
    fun `Test 401 triggers unauthorized handling`() {
        tokenStorage.saveToken("temp-token")
        
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("{}".toResponseBody(null))
                    .build()
            }
            .build()
            
        val req = Request.Builder().url("http://localhost/api/v1/protected").build()
        client.newCall(req).execute()
        
        // Assert token is cleared due to 401 handling
        assertNull(tokenStorage.getToken())
    }

    @Test
    fun `Test 403 returns error but does not clear token`() {
        tokenStorage.saveToken("restricted-token")
        
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(403)
                    .message("Forbidden")
                    .body("{}".toResponseBody(null))
                    .build()
            }
            .build()
            
        val req = Request.Builder().url("http://localhost/api/v1/protected").build()
        client.newCall(req).execute()
        
        // Assert token remains since it's an authorization failure, not authentication failure
        assertEquals("restricted-token", tokenStorage.getToken())
    }
}
