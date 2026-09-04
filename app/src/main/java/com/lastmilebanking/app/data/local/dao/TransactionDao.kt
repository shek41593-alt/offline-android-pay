package com.lastmilebanking.app.data.local.dao

import androidx.room.*
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY createdAt DESC")
    fun getTransactionsByWallet(walletId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTransactions(walletId: String, limit: Int = 10): Flow<List<TransactionEntity>>

    // Phase 2.1 requested methods
    @Query("SELECT * FROM transactions WHERE transactionId = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE clientOperationId = :clientOperationId")
    suspend fun getTransactionByClientOperationId(clientOperationId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' OR status = 'PENDING_SYNC' OR isSynced = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingTransactions(limit: Int = 50): List<TransactionEntity>

    @Query("UPDATE transactions SET status = :status WHERE transactionId = :id")
    suspend fun updateTransactionStatus(id: String, status: String)

    @Query("UPDATE transactions SET isSynced = 1, status = :status, syncedAt = :syncedAt WHERE transactionId = :id")
    suspend fun updateSyncResult(id: String, status: String, syncedAt: Long?)

    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY createdAt DESC")
    fun getTransactionHistory(walletId: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId AND isSynced = 0")
    fun getPendingCount(walletId: String): Flow<Int>

    // Legacy method but kept for compatibility
    @Query("UPDATE transactions SET isSynced = 1, status = 'SYNCED', syncedAt = :syncedAt WHERE transactionId = :transactionId")
    suspend fun markAsSynced(transactionId: String, syncedAt: Long = System.currentTimeMillis())
}
