package com.lastmilebanking.app.data.repository

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.dao.WalletDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val apiService: com.lastmilebanking.app.data.network.api.LastMileApiService
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

    suspend fun refreshWalletFromBackend(userId: String) {
        try {
            android.util.Log.e("FUND_DEBUG", "refreshWalletFromBackend calling API")
            val balanceRes = apiService.getWalletBalance()
            android.util.Log.e("FUND_DEBUG", "refreshWalletFromBackend code=${balanceRes.code()} body=${balanceRes.body()}")
            var currentWalletId = ""
            if (balanceRes.isSuccessful) {
                val dto = balanceRes.body()
                if (dto != null && dto.walletId != null) {
                    currentWalletId = dto.walletId
                    val existing = walletDao.getWalletByUserId(userId).firstOrNull()
                    val newWallet = existing?.copy(
                        walletId = dto.walletId,
                        availableBalance = dto.balance.toDouble(),
                        currency = dto.currency ?: "INR",
                        updatedAt = System.currentTimeMillis()
                    ) ?: WalletEntity(
                        walletId = dto.walletId,
                        userId = userId,
                        availableBalance = dto.balance.toDouble(),
                        currency = dto.currency ?: "INR"
                    )
                    walletDao.insertWallet(newWallet)
                    walletDao.clearOldWallets(userId, dto.walletId)
                    android.util.Log.e("FUND_DEBUG", "refreshWalletFromBackend updated DB")
                }
            }
            
            val txRes = apiService.getUserTransactions()
            if (txRes.isSuccessful) {
                val transactions = txRes.body()
                val walletId = currentWalletId
                if (transactions != null && walletId.isNotEmpty()) {
                    transactions.forEach { tx ->
                        if (tx.transactionId != null) {
                            val ent = TransactionEntity(
                                transactionId = tx.transactionId,
                                walletId = walletId,
                                senderId = tx.senderId ?: "",
                                receiverId = tx.receiverId ?: "",
                                receiverName = if (tx.senderId == userId) (tx.receiverId ?: "") else (tx.senderId ?: ""),
                                amount = tx.amount?.toDouble() ?: 0.0,
                                currency = tx.currency ?: "INR",
                                transactionType = if (tx.senderId == userId) "SEND" else "RECEIVE",
                                paymentMode = tx.paymentMode ?: "ONLINE",
                                status = tx.status ?: "UNKNOWN",
                                encryptedPayload = "",
                                transactionHash = "",
                                note = "",
                                isSynced = true,
                                createdAt = if (tx.transactionTimestamp != null) try { java.time.Instant.parse(tx.transactionTimestamp).toEpochMilli() } catch (e: Exception) { System.currentTimeMillis() } else System.currentTimeMillis(),
                                syncedAt = System.currentTimeMillis()
                            )
                            transactionDao.insertTransaction(ent)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
