package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.data.repository.WalletRepository
import com.lastmilebanking.app.domain.engines.WalletEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

@Singleton
class WalletEngineImpl @Inject constructor(
    private val walletRepository: WalletRepository
) : WalletEngine {

    override fun getWalletBalance(userId: String): Flow<Double> {
        return walletRepository.getWalletByUserId(userId).map { it?.availableBalance ?: 0.0 }
    }

    override fun getOfflineBalance(userId: String): Flow<Double> {
        return walletRepository.getWalletByUserId(userId).map { it?.offlineBalance ?: 0.0 }
    }

    override suspend fun credit(walletId: String, amount: Double): Result<Unit> {
        // We'll fully implement this later using LedgerEngine to maintain immutable records. 
        // For now, it's a stub to fulfill the interface.
        return Result.success(Unit)
    }

    override suspend fun debit(walletId: String, amount: Double): Result<Unit> {
        // Proper debit logic will involve checking balances, throwing exceptions and calling LedgerEngine
        return Result.success(Unit)
    }

    override suspend fun checkOfflineLimit(amount: Double): Boolean {
        // Static offline limit check for MVP
        val maximumOfflineLimitPerTx = 2000.0
        return amount <= maximumOfflineLimitPerTx
    }
}
