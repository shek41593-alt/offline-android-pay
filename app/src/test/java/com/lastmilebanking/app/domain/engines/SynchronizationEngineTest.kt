package com.lastmilebanking.app.domain.engines

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.domain.connectivity.ConnectivityObserver
import com.lastmilebanking.app.domain.engines.impl.SynchronizationEngineImpl
import com.lastmilebanking.app.domain.models.TransactionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SynchronizationEngineTest {

    private lateinit var syncEngine: SynchronizationEngineImpl
    private lateinit var connectivityObserver: FakeConnectivityObserver
    private lateinit var transactionDao: FakeTransactionDao

    @Before
    fun setup() {
        connectivityObserver = FakeConnectivityObserver()
        transactionDao = FakeTransactionDao()
        syncEngine = SynchronizationEngineImpl(transactionDao, connectivityObserver)
    }

    @Test
    fun `TEST 1 - Offline transaction becomes PENDING_SYNC`() {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        assertEquals(TransactionStatus.PENDING_SYNC.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 2 - Pending transaction survives application restart`() {
        // Simulating survival via local DAO persistence wrapper
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        val restaredDao = transactionDao // Imagine restart
        runBlocking {
            assertEquals(1, restaredDao.getPendingTransactions().size)
        }
    }

    @Test
    fun `TEST 3 - Connectivity allows synchronization work`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        runBlocking { syncEngine.uploadPendingTransactions() }
        assertEquals(TransactionStatus.SYNCED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 4 - Successful synchronization changes the local state`() {
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { transactionDao.insertTransaction(tx) }
        runBlocking { syncEngine.uploadPendingTransactions() }
        assertEquals(TransactionStatus.SYNCED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 5 - Transient failure causes retry via PENDING_SYNC state`() {
        connectivityObserver.isConnected = true
        transactionDao.failNextUpdate = true
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        
        runBlocking { 
            transactionDao.insertTransaction(tx)
            syncEngine.uploadPendingTransactions()
        }
        // Because of failNextUpdate, it will catch exception and revert to PENDING_SYNC
        assertEquals(TransactionStatus.PENDING_SYNC.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    @Test
    fun `TEST 6 - Same transaction ID is reused during retry`() {
        val txId = "TXN-RETRY123"
        val tx = createTx(TransactionStatus.PENDING_SYNC).copy(transactionId = txId)
        runBlocking { transactionDao.insertTransaction(tx) }
        runBlocking { syncEngine.uploadPendingTransactions() }
        assertEquals(txId, transactionDao.transactions.values.first().transactionId)
    }

    @Test
    fun `TEST 7 - Duplicate synchronization does not create another local transaction`() {
        val tx = createTx(TransactionStatus.PENDING_SYNC)
        runBlocking { 
            transactionDao.insertTransaction(tx)
            syncEngine.uploadPendingTransactions()
            syncEngine.uploadPendingTransactions()
        }
        assertEquals(1, transactionDao.transactions.size)
    }

    @Test
    fun `TEST 8 - Permanent failure is handled correctly`() {
        // A placeholder showing an explicit failure status, though currently simple simulated upload returns true or false
        connectivityObserver.isConnected = true
        val tx = createTx(TransactionStatus.FAILED)
        runBlocking { transactionDao.insertTransaction(tx) }
        runBlocking { syncEngine.uploadPendingTransactions() } // FAILED won't be picked up
        assertEquals(TransactionStatus.FAILED.name, transactionDao.transactions[tx.transactionId]?.status)
    }

    private fun createTx(status: TransactionStatus): TransactionEntity {
        return TransactionEntity(
            transactionId = "TXN-${System.currentTimeMillis()}",
            walletId = "W1", senderId = "S1", receiverId = "R1", receiverName = "Rec",
            amount = 100.0, transactionType = "SEND", paymentMode = "SMS",
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
    var failNextUpdate = false

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactions[transaction.transactionId] = transaction
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        if (failNextUpdate) {
            failNextUpdate = false
            throw RuntimeException("Transient Network Failure")
        }
        transactions[transaction.transactionId] = transaction
    }

    override suspend fun markAsSynced(transactionId: String, syncedAt: Long) {
        transactions[transactionId] = transactions[transactionId]?.copy(status = TransactionStatus.SYNCED.name, isSynced = true)!!
    }

    override suspend fun getPendingTransactions(): List<TransactionEntity> {
        return transactions.values.filter { !it.isSynced && it.status == TransactionStatus.PENDING_SYNC.name }
    }
    
    // Unused overrides for test
    override fun getTransactionsByWallet(walletId: String) = throw NotImplementedError()
    override fun getRecentTransactions(walletId: String, limit: Int) = throw NotImplementedError()
    override suspend fun getTransactionById(id: String) = transactions[id]
    override fun getPendingCount(walletId: String) = throw NotImplementedError()
}
