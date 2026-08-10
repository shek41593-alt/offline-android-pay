package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.repository.WalletRepository
import com.lastmilebanking.app.domain.engines.ValidationEngine
import com.lastmilebanking.app.domain.engines.WalletEngine
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ValidationEngineImpl @Inject constructor(
    private val walletRepository: WalletRepository,
    private val walletEngine: WalletEngine,
    private val transactionDao: TransactionDao
) : ValidationEngine {

    override suspend fun hasSufficientBalance(walletId: String, amount: Double): Boolean {
        if (amount <= 0.0) return false
        val wallet = walletRepository.getWalletByUserId(walletId).firstOrNull()
        return if (wallet != null) {
            wallet.availableBalance >= amount
        } else {
            false
        }
    }

    override suspend fun isWithinDailyLimit(userId: String, amount: Double): Boolean {
        // MVP logic: max 100,000 per day
        return amount <= 100000.0
    }

    override suspend fun isWithinOfflineLimit(userId: String, amount: Double): Boolean {
        return walletEngine.checkOfflineLimit(amount)
    }

    override suspend fun isDuplicateTransaction(transactionId: String): Boolean {
        // Find if transaction already exists in DB
        val t = transactionDao.getTransactionById(transactionId)
        return t != null
    }

    override suspend fun isValidMerchant(merchantId: String): Boolean {
        return merchantId.isNotEmpty()
    }
}
