package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.dao.WalletDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao
) {
    fun getWalletByUserId(userId: String): Flow<WalletEntity?> =
        walletDao.getWalletByUserId(userId)

    fun getRecentTransactions(walletId: String, limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(walletId, limit)

    fun getPendingTransactionCount(walletId: String): Flow<Int> =
        transactionDao.getPendingCount(walletId)

    suspend fun debitWallet(wallet: WalletEntity, amount: Double): Result<WalletEntity> {
        return try {
            if (wallet.availableBalance < amount) {
                Result.failure(Exception("Insufficient balance"))
            } else {
                val updated = wallet.copy(
                    availableBalance = wallet.availableBalance - amount,
                    updatedAt = System.currentTimeMillis()
                )
                walletDao.updateWallet(updated)
                Result.success(updated)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun creditWallet(wallet: WalletEntity, amount: Double): Result<WalletEntity> {
        return try {
            val updated = wallet.copy(
                availableBalance = wallet.availableBalance + amount,
                updatedAt = System.currentTimeMillis()
            )
            walletDao.updateWallet(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun getPendingTransactions(): List<TransactionEntity> =
        transactionDao.getPendingTransactions()

    suspend fun markTransactionSynced(transactionId: String) {
        transactionDao.markAsSynced(transactionId)
    }
}
