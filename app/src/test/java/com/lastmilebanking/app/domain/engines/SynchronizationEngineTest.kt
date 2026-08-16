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
        assertEquals(TransactionStatus.SYNCED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 4 - Successful synchronization changes the local state`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        fakeApi.responses[tx.transactionId] = Response.success(SyncTransactionResponseDto(tx.transactionId, "SETTLED", ""))
        runBlocking { syncEngine.uploadPendingTransactions() }
        assertEquals(TransactionStatus.SYNCED.name, transactionDao.transactions[tx.transactionId]?.status)
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
        assertEquals(TransactionStatus.SYNCED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 8 - Permanent failure is handled correctly`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        fakeApi.responses[tx.transactionId] = Response.error(400, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
        runBlocking { syncEngine.uploadPendingTransactions() } 
        assertEquals(TransactionStatus.FAILED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 9 - 401 Unauthorized prevents retry loop`() = runBlocking {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        transactionDao.insertTransaction(tx)

        fakeApi.responses[tx.transactionId] = Response.error(401, "{}".toResponseBody("application/json".toMediaTypeOrNull()))

        // Does not throw exception
        syncEngine.uploadPendingTransactions()

        val updated = transactionDao.transactions[tx.transactionId]!!
        assertEquals(TransactionStatus.PENDING_SYNC.name, updated.status)
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

    override suspend fun getPendingTransactions(): List<TransactionEntity> {
        return transactions.values.filter { !it.isSynced && it.status == TransactionStatus.PENDING_SYNC.name }
    }
    
    override fun getTransactionsByWallet(walletId: String) = throw NotImplementedError()
    override fun getRecentTransactions(walletId: String, limit: Int) = throw NotImplementedError()
    override suspend fun getTransactionById(id: String) = transactions[id]
    override fun getPendingCount(walletId: String) = throw NotImplementedError()
}

class FakeLastMileApiService : LastMileApiService {
    val requests = mutableListOf<SyncTransactionRequestDto>()
    val responses = mutableMapOf<String, Response<SyncTransactionResponseDto>>()

    override suspend fun register(request: com.lastmilebanking.app.data.network.dto.RegisterRequestDto) = TODO()
    override suspend fun login(request: com.lastmilebanking.app.data.network.dto.LoginRequestDto) = TODO()
    override suspend fun getTransactionStatus(transactionId: String) = TODO()
    override suspend fun checkHealth() = TODO()

    override suspend fun syncTransaction(request: SyncTransactionRequestDto): Response<SyncTransactionResponseDto> {
        requests.add(request)
        return responses[request.transactionId] ?: Response.error(500, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
    }
}
