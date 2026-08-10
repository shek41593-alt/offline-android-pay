package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.domain.engines.TransactionEngine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionEngineImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionEngine {

    override suspend fun createTransaction(
        senderId: String,
        receiverId: String,
        amount: Double,
        type: String
    ): Result<String> {
        return try {
            val transactionId = generateTransactionId()
            val entity = TransactionEntity(
                transactionId = transactionId,
                walletId = senderId, // MVP assuming user's walletId == senderId
                senderId = senderId,
                receiverId = receiverId,
                receiverName = "Unknown",
                amount = amount,
                transactionType = type,
                paymentMode = "SMS",
                status = "PENDING_SYNC",
                encryptedPayload = "",
                transactionHash = generateTransactionHash("$senderId$receiverId$amount$type"),
                isSynced = false
            )
            transactionDao.insertTransaction(entity)
            Result.success(transactionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun validateTransaction(transactionId: String): Boolean {
        return transactionId.isNotEmpty()
    }

    override fun generateTransactionId(): String {
        return "TXN-" + UUID.randomUUID().toString().uppercase().take(12)
    }

    override fun generateTransactionHash(payload: String): String {
        // Simplified hash for MVP
        return payload.hashCode().toString()
    }

    override fun generateDigitalSignature(payload: String): String {
        // Simplified signature for MVP
        return "SIG-" + payload.hashCode().toString()
    }
}
