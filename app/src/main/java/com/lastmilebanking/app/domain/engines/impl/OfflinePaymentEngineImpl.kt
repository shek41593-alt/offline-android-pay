package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.domain.engines.OfflinePaymentEngine
import com.lastmilebanking.app.domain.engines.TransactionEngine
import com.lastmilebanking.app.domain.engines.ValidationEngine
import com.lastmilebanking.app.domain.engines.WalletEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflinePaymentEngineImpl @Inject constructor(
    private val validationEngine: ValidationEngine,
    private val transactionEngine: TransactionEngine,
    private val walletEngine: WalletEngine
) : OfflinePaymentEngine {

    override fun generateEncryptedPayload(userId: String, amount: Double, timestamp: Long): String {
        return "LMB:PAY:USER=$userId:AMT=$amount:TIME=$timestamp:HASH=${transactionEngine.generateTransactionHash("$userId$amount$timestamp")}"
    }

    override fun parseEncryptedPayload(payload: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parts = payload.split(":")
        for (part in parts) {
            val kv = part.split("=")
            if (kv.size == 2) {
                map[kv[0]] = kv[1]
            }
        }
        return map
    }

    override fun validateTransactionAuthenticity(payload: String, merchantId: String): Boolean {
        // MVP basic validation
        return payload.contains("LMB:PAY")
    }
}
