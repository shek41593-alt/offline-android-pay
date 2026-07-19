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

    @Query("SELECT * FROM transactions WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1, status = 'SYNCED', syncedAt = :syncedAt WHERE transactionId = :transactionId")
    suspend fun markAsSynced(transactionId: String, syncedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE transactionId = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId AND isSynced = 0")
    fun getPendingCount(walletId: String): Flow<Int>
}
