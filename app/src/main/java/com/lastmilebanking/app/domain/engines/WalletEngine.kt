package com.lastmilebanking.app.domain.engines

import kotlinx.coroutines.flow.Flow

interface WalletEngine {
    fun getWalletBalance(userId: String): Flow<Double>
    fun getOfflineBalance(userId: String): Flow<Double>
    suspend fun credit(walletId: String, amount: Double): Result<Unit>
    suspend fun debit(walletId: String, amount: Double): Result<Unit>
    suspend fun checkOfflineLimit(amount: Double): Boolean
}
