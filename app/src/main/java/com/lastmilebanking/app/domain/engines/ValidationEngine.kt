package com.lastmilebanking.app.domain.engines

interface ValidationEngine {
    suspend fun hasSufficientBalance(walletId: String, amount: Double): Boolean
    suspend fun isWithinDailyLimit(userId: String, amount: Double): Boolean
    suspend fun isWithinOfflineLimit(userId: String, amount: Double): Boolean
    suspend fun isDuplicateTransaction(transactionId: String): Boolean
    suspend fun isValidMerchant(merchantId: String): Boolean
}
