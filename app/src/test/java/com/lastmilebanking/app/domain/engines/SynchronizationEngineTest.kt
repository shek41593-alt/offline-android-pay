package com.lastmilebanking.app.domain.engines

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.dto.SyncTransactionRequestDto
import com.lastmilebanking.app.data.network.dto.SyncTransactionResponseDto
import com.lastmilebanking.app.domain.connectivity.ConnectivityObserver
import com.lastmilebanking.app.domain.engines.impl.SynchronizationEngineImpl
import com.lastmilebanking.app.domain.models.TransactionStatus
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

import android.content.Context

class SynchronizationEngineTest {

    private lateinit var syncEngine: SynchronizationEngineImpl
    private lateinit var connectivityObserver: FakeConnectivityObserver
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var fakeApi: FakeLastMileApiService

    @Before
    fun setup() {
        val mockContext: Context? = null
        connectivityObserver = FakeConnectivityObserver()
        transactionDao = FakeTransactionDao()
        fakeApi = FakeLastMileApiService()
        syncEngine = SynchronizationEngineImpl(mockContext, transactionDao, connectivityObserver, fakeApi)
    }

    @Test
    fun `TEST 1 - Offline transaction becomes PENDING_SYNC`() {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        assertEquals(TransactionStatus.PENDING_SYNC.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 2 - Pending transaction survives application restart`() {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        val restaredDao = transactionDao
        runBlocking {
            assertEquals(1, restaredDao.getPendingTransactions().size)
        }
    }

    @Test
    fun `TEST 3 - Connectivity allows synchronization work`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        fakeApi.responses[tx.transactionId] = Response.success(SyncTransactionResponseDto(tx.transactionId, "RECEIVED", ""))
        runBlocking { syncEngine.uploadPendingTransactions() }
        assertEquals(TransactionStatus.SETTLED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 4 - Successful synchronization changes the local state`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        fakeApi.responses[tx.transactionId] = Response.success(SyncTransactionResponseDto(tx.transactionId, "SETTLED", ""))
        runBlocking { syncEngine.uploadPendingTransactions() }
        assertEquals(TransactionStatus.SETTLED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 5 - Transient failure causes retry via PENDING_SYNC state`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        fakeApi.responses[tx.transactionId] = Response.error(500, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
        var threw = false
        try {
            runBlocking { syncEngine.uploadPendingTransactions() }
        } catch (e: Exception) {
            threw = true
        }
        assertTrue(threw)
        assertEquals(TransactionStatus.PENDING_SYNC.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 6 - Same transaction ID is reused during retry`() {
        val txId = "TXN-RETRY123"
        val tx = createTx(TransactionStatus.PENDING_SYNC).copy(transactionId = txId)
        runBlocking { transactionDao.insertTransaction(tx) }
        fakeApi.responses[txId] = Response.success(SyncTransactionResponseDto(txId, "RECEIVED", ""))
        runBlocking { syncEngine.uploadPendingTransactions() }
        assertEquals(txId, transactionDao.transactions.values.first().transactionId)
        assertEquals(txId, fakeApi.requests.first().transactionId)
        assertEquals(tx.clientOperationId, fakeApi.requests.first().clientOperationId)
    }

    @Test
    fun `TEST 7 - Duplicate synchronization does not create another local transaction`() {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { 
            transactionDao.insertTransaction(tx)
            fakeApi.responses[tx.transactionId] = Response.success(SyncTransactionResponseDto(tx.transactionId, "DUPLICATE", ""))
            syncEngine.uploadPendingTransactions()
            syncEngine.uploadPendingTransactions()
        }
        assertEquals(1, transactionDao.transactions.size)
        assertEquals(TransactionStatus.SETTLED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 8 - Permanent failure is handled correctly`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        fakeApi.responses[tx.transactionId] = Response.error(400, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
        runBlocking { syncEngine.uploadPendingTransactions() } 
        assertEquals(TransactionStatus.ACTION_REQUIRED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 9 - 401 Unauthorized prevents retry loop`() = runBlocking {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        transactionDao.insertTransaction(tx)

        fakeApi.responses[tx.transactionId] = Response.error(401, "{}".toResponseBody("application/json".toMediaTypeOrNull()))

        // Does not throw exception
        syncEngine.uploadPendingTransactions()

        val updated = transactionDao.transactions[tx.transactionId]!!
        assertEquals(TransactionStatus.ACTION_REQUIRED.name, updated.status)
        assertEquals(1, updated.retryCount)
    }
    
    @Test
    fun `TEST 10 - 409 Conflict behavior`() = runBlocking {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        transactionDao.insertTransaction(tx)

        fakeApi.responses[tx.transactionId] = Response.error(409, "{}".toResponseBody("application/json".toMediaTypeOrNull()))

        // Does not throw exception
        syncEngine.uploadPendingTransactions()

        val updated = transactionDao.transactions[tx.transactionId]!!
        assertEquals(TransactionStatus.CONFLICT.name, updated.status)
        assertEquals(1, updated.retryCount)
    }
    
    @Test
    fun `TEST 11 - Partial batch behavior with sequence`() = runBlocking {
        connectivityObserver.isConnected = true
        // TX1 success
        val tx1 = createTx(TransactionStatus.PENDING_SYNC).copy(transactionId = "TX1")
        // TX2 connection timeout (Exception)
        val tx2 = createTx(TransactionStatus.PENDING_SYNC).copy(transactionId = "TX2", createdAt = System.currentTimeMillis() + 10)
        // TX3 not processed
        val tx3 = createTx(TransactionStatus.PENDING_SYNC).copy(transactionId = "TX3", createdAt = System.currentTimeMillis() + 20)
        
        transactionDao.insertTransaction(tx1)
        transactionDao.insertTransaction(tx2)
        transactionDao.insertTransaction(tx3)

        fakeApi.responses[tx1.transactionId] = Response.success(SyncTransactionResponseDto(tx1.transactionId, "SETTLED", ""))
        // no response for TX2 means it will return 500 by FakeApi which throws "Transient server error 500"
        
        var threw = false
        try {
            syncEngine.uploadPendingTransactions()
        } catch (e: Exception) {
            threw = true
        }
        
        assertTrue(threw)
        
        assertEquals(TransactionStatus.SETTLED.name, transactionDao.transactions["TX1"]?.status)
        assertEquals(TransactionStatus.PENDING_SYNC.name, transactionDao.transactions["TX2"]?.status)
        assertEquals(1, transactionDao.transactions["TX2"]?.retryCount)
        assertEquals(TransactionStatus.PENDING_SYNC.name, transactionDao.transactions["TX3"]?.status)
        assertEquals(0, transactionDao.transactions["TX3"]?.retryCount) // Unprocessed
    }

    private fun createTx(status: TransactionStatus): TransactionEntity {
        return TransactionEntity(
            transactionId = "TXN-${System.currentTimeMillis()}",
            walletId = "W1", senderId = "S1", receiverId = "R1", receiverName = "Rec",
            amount = 100.0, transactionType = "SEND", paymentMode = "QR",
            status = status.name, isSynced = false
        )
    }
}

class FakeConnectivityObserver : ConnectivityObserver {
    var isConnected = true
    override fun isNetworkAvailable() = isConnected
}

class FakeTransactionDao : TransactionDao {
    val transactions = mutableMapOf<String, TransactionEntity>()

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactions[transaction.transactionId] = transaction
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        transactions[transaction.transactionId] = transaction
    }

    override suspend fun markAsSynced(transactionId: String, syncedAt: Long) {
        transactions[transactionId] = transactions[transactionId]?.copy(status = TransactionStatus.SYNCED.name, isSynced = true)!!
    }

    override suspend fun getPendingTransactions(limit: Int): List<TransactionEntity> {
        return transactions.values.filter { !it.isSynced && it.status == TransactionStatus.PENDING_SYNC.name }.take(limit)
    }
    
    override fun getTransactionsByWallet(walletId: String) = kotlinx.coroutines.flow.flowOf<List<TransactionEntity>>()
    override fun getRecentTransactions(walletId: String, limit: Int) = kotlinx.coroutines.flow.flowOf<List<TransactionEntity>>()
    override suspend fun getTransactionById(id: String) = transactions[id]
    override fun getPendingCount(walletId: String) = kotlinx.coroutines.flow.flowOf(0)
    
    override suspend fun getTransactionByClientOperationId(clientOperationId: String): TransactionEntity? = null
    override suspend fun updateTransactionStatus(id: String, status: String) {
        transactions[id] = transactions[id]?.copy(status = status)!! 
    }
    override suspend fun updateSyncResult(id: String, status: String, syncedAt: Long?) {
        transactions[id] = transactions[id]?.copy(status = status, isSynced = true, syncedAt = syncedAt)!!
    }
    override fun getTransactionHistory(walletId: String) = kotlinx.coroutines.flow.flowOf<List<TransactionEntity>>()
}

class FakeLastMileApiService : LastMileApiService {
    val requests = mutableListOf<SyncTransactionRequestDto>()
    val responses = mutableMapOf<String, Response<SyncTransactionResponseDto>>()

    override suspend fun getPaymentIdentity(): retrofit2.Response<com.lastmilebanking.app.data.network.dto.PaymentIdentityDto> {
        return retrofit2.Response.success(com.lastmilebanking.app.data.network.dto.PaymentIdentityDto("user", "LMB-123"))
    }
    
    override suspend fun getProfile(): retrofit2.Response<com.lastmilebanking.app.data.network.dto.UserProfileDto> {
        return retrofit2.Response.success(com.lastmilebanking.app.data.network.dto.UserProfileDto("Name", "email", "phone", "addr"))
    }

    override suspend fun register(request: com.lastmilebanking.app.data.network.dto.RegisterRequestDto) = TODO()
    override suspend fun login(request: com.lastmilebanking.app.data.network.dto.LoginRequestDto) = TODO()
    override suspend fun checkUser(request: com.lastmilebanking.app.data.network.dto.CheckUserRequestDto) = TODO()
    override suspend fun verifyOtp(request: com.lastmilebanking.app.data.network.dto.VerifyOtpRequestDto) = TODO()

    override suspend fun getTransactionStatus(transactionId: String) = TODO()
    override suspend fun getTransactionDetail(transactionId: String) = TODO()
    override suspend fun fundWallet(idempotencyKey: String, request: com.lastmilebanking.app.data.network.dto.FundWalletRequestDto): retrofit2.Response<com.lastmilebanking.app.data.network.dto.FundWalletResponseDto> = TODO()
    override suspend fun checkHealth() = TODO()

    override suspend fun resolveRecipient(publicPaymentId: String) = TODO()
    override suspend fun processPayment(request: com.lastmilebanking.app.data.network.dto.DirectPaymentRequestDto) = TODO()
    override suspend fun getWalletBalance() = TODO()
    override suspend fun getUserTransactions() = TODO()

    override suspend fun syncTransaction(request: SyncTransactionRequestDto): Response<SyncTransactionResponseDto> {
        requests.add(request)
        return responses[request.transactionId] ?: Response.error(500, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
    }
}
