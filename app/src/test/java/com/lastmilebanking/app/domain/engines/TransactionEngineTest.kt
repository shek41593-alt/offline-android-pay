package com.lastmilebanking.app.domain.engines

import com.lastmilebanking.app.domain.engines.impl.TransactionEngineImpl
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class TransactionEngineTest {

    private lateinit var engine: TransactionEngineImpl
    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var fakeValidationEngine: FakeValidationEngine

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        fakeValidationEngine = FakeValidationEngine()
        engine = TransactionEngineImpl(fakeRepository, fakeValidationEngine)
    }

    @Test
    fun `TEST 1 - Valid transaction is created and persisted`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "SEND")
        assertTrue(result.isSuccess)
        val txId = result.getOrNull()!!
        val saved = fakeRepository.transactions[txId]
        assertNotNull(saved)
        assertEquals("PENDING", saved?.status)
    }

    @Test
    fun `TEST 2 - transactionId is generated`() = runBlocking {
        val id = engine.generateTransactionId()
        assertTrue(id.startsWith("TXN-"))
    }

    @Test
    fun `TEST 3 - clientOperationId is generated`() = runBlocking {
        val id = engine.generateClientOperationId()
        assertTrue(id.startsWith("OPC-"))
    }

    @Test
    fun `TEST 4 - transactionId and clientOperationId are different`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "SEND")
        val txId = result.getOrNull()!!
        val saved = fakeRepository.transactions[txId]!!
        assertNotEquals(saved.transactionId, saved.clientOperationId)
    }

    @Test
    fun `TEST 5 - status is PENDING`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "SEND")
        val txId = result.getOrNull()!!
        assertEquals("PENDING", fakeRepository.transactions[txId]?.status)
    }

    @Test
    fun `TEST 6 - retryCount starts at 0`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "TOPUP")
        val txId = result.getOrNull()!!
        assertEquals(0, fakeRepository.transactions[txId]?.retryCount)
    }

    @Test
    fun `TEST 7 - syncedAt starts null`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "RECEIVE")
        val txId = result.getOrNull()!!
        assertEquals(null, fakeRepository.transactions[txId]?.syncedAt)
    }

    @Test
    fun `TEST 8 - invalid amount is rejected`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", -50.0, "SEND")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TransactionEngineImpl.ValidationException)
    }

    @Test
    fun `TEST 9 - invalid wallet identifiers are rejected`() = runBlocking {
        val r1 = engine.createOfflineTransaction("", "W2", 100.0, "SEND")
        val r2 = engine.createOfflineTransaction("W1", "W1", 100.0, "SEND")
        assertTrue(r1.isFailure)
        assertTrue(r2.isFailure)
    }

    @Test
    fun `TEST 10 - invalid transaction type is rejected`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "INVALID_TYPE")
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `TEST 11 - transaction is persisted through TransactionRepository`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "SEND")
        val txId = result.getOrNull()!!
        val saved = fakeRepository.getTransactionById(txId)
        assertNotNull(saved)
    }

    @Test
    fun `TEST 12 - transaction can be retrieved using clientOperationId`() = runBlocking {
        val result = engine.createOfflineTransaction("W1", "W2", 100.0, "SEND")
        val txId = result.getOrNull()!!
        val clientOpId = fakeRepository.getTransactionById(txId)!!.clientOperationId
        val retrieved = fakeRepository.getTransactionByClientOperationId(clientOpId)
        assertNotNull(retrieved)
        assertEquals(txId, retrieved?.transactionId)
    }

    @Test
    fun `TEST 13 - transaction hash is generated deterministically`() {
        val hash1 = engine.generateTransactionHash("payload1")
        val hash2 = engine.generateTransactionHash("payload1")
        assertEquals(hash1, hash2)
    }

    // A fake repository for tests
    class FakeTransactionRepository : TransactionRepository {
        val transactions = mutableMapOf<String, TransactionEntity>()

        override suspend fun saveTransaction(transaction: TransactionEntity) {
            transactions[transaction.transactionId] = transaction
        }

        override suspend fun getTransactionById(transactionId: String): TransactionEntity? {
            return transactions[transactionId]
        }

        override suspend fun getTransactionByClientOperationId(clientOperationId: String): TransactionEntity? {
            return transactions.values.find { it.clientOperationId == clientOperationId }
        }

        override fun observeTransactionHistory(walletId: String): Flow<List<TransactionEntity>> {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        override suspend fun getPendingTransactions(): List<TransactionEntity> {
            return transactions.values.filter { it.status == "PENDING" }
        }

        override suspend fun updateTransactionStatus(transactionId: String, status: String) {
            transactions[transactionId] = transactions[transactionId]?.copy(status = status)!!
        }

        override suspend fun updateSyncResult(transactionId: String, status: String, syncedAt: Long?) {
            transactions[transactionId] = transactions[transactionId]?.copy(status = status, syncedAt = syncedAt)!!
        }
    }

    // A fake validation engine for tests
    class FakeValidationEngine : ValidationEngine {
        override suspend fun hasSufficientBalance(walletId: String, amount: Double): Boolean = true
        override suspend fun isWithinDailyLimit(userId: String, amount: Double): Boolean = true
        override suspend fun isWithinOfflineLimit(userId: String, amount: Double): Boolean = true
        override suspend fun isDuplicateTransaction(transactionId: String): Boolean = false
        override suspend fun isValidMerchant(merchantId: String): Boolean = true
    }
}
