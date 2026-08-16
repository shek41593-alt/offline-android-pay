package com.lastmilebanking.app.e2e

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lastmilebanking.app.data.local.LastMileDatabase
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.auth.AuthInterceptor
import com.lastmilebanking.app.data.network.auth.SessionManager
import com.lastmilebanking.app.data.network.auth.TokenStorage
import com.lastmilebanking.app.data.network.dto.LoginRequestDto
import com.lastmilebanking.app.data.network.dto.RegisterRequestDto
import com.lastmilebanking.app.domain.connectivity.ConnectivityObserver
import com.lastmilebanking.app.domain.engines.impl.SynchronizationEngineImpl
import com.lastmilebanking.app.domain.models.TransactionStatus
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OfflineSyncE2ETest {

    private lateinit var db: LastMileDatabase
    private lateinit var api: LastMileApiService
    private lateinit var engine: SynchronizationEngineImpl
    private lateinit var tokenStorage: TokenStorage
    
    private val BASE_URL = "http://10.43.215.250:8080/"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LastMileDatabase::class.java).build()
        
        tokenStorage = object : TokenStorage {
            var savedToken: String? = null
            override fun saveToken(t: String) { savedToken = t }
            override fun getToken(): String? = savedToken
            override fun clearToken() { savedToken = null }
            override fun hasToken(): Boolean = savedToken != null
        }
        
        val sessionManager = SessionManager(tokenStorage)
        val interceptor = AuthInterceptor(tokenStorage, sessionManager)
        
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastMileApiService::class.java)
            
        val connectivity = object : ConnectivityObserver {
            override fun isNetworkAvailable(): Boolean = true
            // If observe() needs to exist we will implement it but the old tests say it doesn't.
        }
        
        engine = SynchronizationEngineImpl(context, db.transactionDao(), connectivity, api)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testEndToEndOfflineSync() = runBlocking {
        // 1. Authenticate User A
        val userA = "usera_${UUID.randomUUID()}".take(20)
        api.register(RegisterRequestDto(userA, "password123"))
        val loginResponse = api.login(LoginRequestDto(userA, "password123"))
        assertTrue("Login failed from test user", loginResponse.isSuccessful)
        val jwt = loginResponse.body()!!.accessToken!!
        val senderUserId = loginResponse.body()!!.userId!!
        tokenStorage.saveToken(jwt)
        
        val userB = "userb_${UUID.randomUUID()}".take(20)
        val receiverRegister = api.register(RegisterRequestDto(userB, "password123"))
        val receiverUserId = receiverRegister.body()?.userId ?: userB

        // 3. Create Offline Payment
        val txId = "E2E-TEST-${UUID.randomUUID()}"
        val transaction = TransactionEntity(
            transactionId = txId,
            walletId = "wallet_1",
            senderId = senderUserId,
            receiverId = receiverUserId,
            receiverName = "User B",
            amount = 100.0,
            currency = "INR",
            transactionType = "SEND",
            paymentMode = "QR",
            status = TransactionStatus.PENDING_SYNC.name,
            isSynced = false
        )
        
        db.transactionDao().insertTransaction(transaction)
        
        // 4. Offline State Verify
        var localTx = db.transactionDao().getTransactionById(txId)
        assertEquals(TransactionStatus.PENDING_SYNC.name, localTx?.status)
        assertEquals(false, localTx?.isSynced)
        
        // 7. Trigger Sync
        engine.uploadPendingTransactions()
        
        // 13. Verify Android Response update
        localTx = db.transactionDao().getTransactionById(txId)
        assertTrue("Expected SYNCED but was ${localTx?.status}", localTx?.isSynced ?: false)
        assertTrue(localTx?.status == "SYNCED" || localTx?.status == "COMPLETED")
        
        // TEST 2. Exact Retry
        // Mutate back to pending to simulate worker retry
        db.transactionDao().updateTransaction(localTx!!.copy(isSynced = false, status = TransactionStatus.PENDING_SYNC.name))
        engine.uploadPendingTransactions()
        
        localTx = db.transactionDao().getTransactionById(txId)
        assertTrue("Duplicate Retry should succeed", localTx?.isSynced ?: false)
        
        // TEST 3. Idempotency Conflict
        val conflictTxId = "E2E-TEST-${UUID.randomUUID()}"
        val conflictTx = transaction.copy(transactionId = conflictTxId, amount = 200.0)
        db.transactionDao().insertTransaction(conflictTx)
        engine.uploadPendingTransactions()
        
        val mutatedTx = conflictTx.copy(amount = 250.0, isSynced = false, status = TransactionStatus.PENDING_SYNC.name)
        db.transactionDao().updateTransaction(mutatedTx)
        engine.uploadPendingTransactions()
        // the engine marks 409 as synced according to Phase 16.5 design, so
        val conflictResult = db.transactionDao().getTransactionById(conflictTxId)
        assertTrue(conflictResult?.isSynced ?: false)
        
        // TEST 6. Multiple Offline Payments
        val ms1 = "TX-A-${UUID.randomUUID()}"
        val ms2 = "TX-B-${UUID.randomUUID()}"
        db.transactionDao().insertTransaction(transaction.copy(transactionId = ms1, status = TransactionStatus.PENDING_SYNC.name, isSynced = false))
        db.transactionDao().insertTransaction(transaction.copy(transactionId = ms2, status = TransactionStatus.PENDING_SYNC.name, isSynced = false))
        engine.uploadPendingTransactions()
        assertTrue(db.transactionDao().getTransactionById(ms1)?.isSynced ?: false)
        assertTrue(db.transactionDao().getTransactionById(ms2)?.isSynced ?: false)
        
        // TEST 9. Invalid JWT Handling
        val badTxId = "TX-BAD-${UUID.randomUUID()}"
        db.transactionDao().insertTransaction(transaction.copy(transactionId = badTxId, status = TransactionStatus.PENDING_SYNC.name, isSynced = false))
        tokenStorage.saveToken("invalid.jwt.token")
        
        engine.uploadPendingTransactions()
        val badTx = db.transactionDao().getTransactionById(badTxId)
        assertEquals("Invalid JWT should cause 401 and revert to PENDING_SYNC", TransactionStatus.PENDING_SYNC.name, badTx?.status)
    }
}
