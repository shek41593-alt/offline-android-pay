package com.lastmilebanking.app.data.local.dao

import androidx.room.*
import com.lastmilebanking.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("SELECT * FROM wallet WHERE userId = :userId")
    fun getWalletByUserId(userId: String): Flow<WalletEntity?>

    @Query("SELECT * FROM wallet WHERE walletId = :walletId")
    suspend fun getWalletById(walletId: String): WalletEntity?

    @Query("UPDATE wallet SET availableBalance = :balance, updatedAt = :updatedAt WHERE walletId = :walletId")
    suspend fun updateBalance(walletId: String, balance: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE wallet SET offlineBalance = :offlineBalance, usedOfflineLimitToday = :usedLimit, updatedAt = :updatedAt WHERE walletId = :walletId")
    suspend fun updateOfflineBalance(walletId: String, offlineBalance: Double, usedLimit: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT availableBalance FROM wallet WHERE userId = :userId")
    suspend fun getBalanceForUser(userId: String): Double?
}
