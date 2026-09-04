package com.lastmilebanking.app.domain.repository

import com.lastmilebanking.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun saveTransaction(transaction: TransactionEntity)
    suspend fun getTransactionById(transactionId: String): TransactionEntity?
    suspend fun getTransactionByClientOperationId(clientOperationId: String): TransactionEntity?
    fun observeTransactionHistory(walletId: String): Flow<List<TransactionEntity>>
    suspend fun getPendingTransactions(): List<TransactionEntity>
    suspend fun updateTransactionStatus(transactionId: String, status: String)
    suspend fun updateSyncResult(transactionId: String, status: String, syncedAt: Long?)
}
