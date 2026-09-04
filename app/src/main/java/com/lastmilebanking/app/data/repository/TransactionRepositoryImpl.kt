package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun saveTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    override suspend fun getTransactionById(transactionId: String): TransactionEntity? {
        return transactionDao.getTransactionById(transactionId)
    }

    override suspend fun getTransactionByClientOperationId(clientOperationId: String): TransactionEntity? {
        return transactionDao.getTransactionByClientOperationId(clientOperationId)
    }

    override fun observeTransactionHistory(walletId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionHistory(walletId)
    }

    override suspend fun getPendingTransactions(): List<TransactionEntity> {
        return transactionDao.getPendingTransactions()
    }

    override suspend fun updateTransactionStatus(transactionId: String, status: String) {
        transactionDao.updateTransactionStatus(transactionId, status)
    }

    override suspend fun updateSyncResult(transactionId: String, status: String, syncedAt: Long?) {
        transactionDao.updateSyncResult(transactionId, status, syncedAt)
    }
}
