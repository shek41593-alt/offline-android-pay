package com.lastmilebanking.app.domain.engines

interface OfflinePaymentEngine {
    fun generateEncryptedPayload(userId: String, amount: Double, timestamp: Long): String
    fun parseEncryptedPayload(payload: String): Map<String, String>
    fun validateTransactionAuthenticity(payload: String, merchantId: String): Boolean
}
