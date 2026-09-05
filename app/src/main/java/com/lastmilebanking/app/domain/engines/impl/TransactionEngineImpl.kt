package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.domain.repository.TransactionRepository
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.domain.engines.TransactionEngine
import com.lastmilebanking.app.domain.engines.ValidationEngine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionEngineImpl @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val validationEngine: ValidationEngine
) : TransactionEngine {

    companion object {
        const val STATUS_PENDING = "PENDING"
    }

    class ValidationException(msg: String) : Exception(msg)
    class PersistenceException(msg: String, cause: Throwable? = null) : Exception(msg, cause)

    override suspend fun createOfflineTransaction(
        senderWalletId: String,
        receiverWalletId: String,
        amount: Double,
        transactionType: String,
        clientOperationId: String?
    ): Result<String> {
        return performTransactionCreation(senderWalletId, receiverWalletId, amount, transactionType, "OFFLINE", clientOperationId)
    }

    override suspend fun createTransaction(
        senderId: String,
        receiverId: String,
        amount: Double,
        type: String,
        paymentMode: String
    ): Result<String> {
        return performTransactionCreation(senderId, receiverId, amount, type, paymentMode)
    }

    private suspend fun performTransactionCreation(
        senderWalletId: String,
        receiverWalletId: String,
        amount: Double,
        transactionType: String,
        paymentMode: String,
        externalClientOperationId: String? = null
    ): Result<String> {
        // 1. Validation
        if (amount <= 0.0) {
            return Result.failure(ValidationException("Amount must be greater than zero."))
        }
        if (senderWalletId.isBlank() || receiverWalletId.isBlank()) {
            return Result.failure(ValidationException("Wallet identifiers cannot be empty."))
        }
        if (senderWalletId == receiverWalletId) {
            return Result.failure(ValidationException("Sender and receiver cannot be the same."))
        }
        if (transactionType != "SEND" && transactionType != "RECEIVE" && transactionType != "TOPUP") {
            return Result.failure(ValidationException("Invalid transaction type."))
        }
        
        // Use existing validation engine to check balance only if user is sender
        if (transactionType == "SEND") {
            if (!validationEngine.hasSufficientBalance(senderWalletId, amount)) {
                return Result.failure(ValidationException("Insufficient local available balance."))
            }
        }

        // 2. ID Generation
        val transactionId = generateTransactionId()
        val finalClientOperationId = externalClientOperationId ?: generateClientOperationId()
        val createdAt = System.currentTimeMillis()

        // 3. Hashing
        val payloadToHash = "$finalClientOperationId|$senderWalletId|$receiverWalletId|$amount|$transactionType|$createdAt"
        val transactionHash = generateTransactionHash(payloadToHash)

        // 4. Persistence Entity
        val entity = TransactionEntity(
            transactionId = transactionId,
            clientOperationId = finalClientOperationId,
            senderWalletId = senderWalletId,
            receiverWalletId = receiverWalletId,
            walletId = senderWalletId, // For compatibility
            senderId = senderWalletId,
            receiverId = receiverWalletId,
            receiverName = "Unknown", // Placeholder, will be synced from server
            amount = amount,
            transactionType = transactionType,
            paymentMode = paymentMode,
            status = STATUS_PENDING,
            encryptedPayload = "",
            transactionHash = transactionHash,
            isSynced = false,
            createdAt = createdAt,
            syncedAt = null,
            retryCount = 0, // Starts at 0
            failureReason = null
        )

        // 5. Durable persistence
        return try {
            transactionRepository.saveTransaction(entity)
            // Note: Wallet balance mutation is omitted intentionally. Double-entry accounting requires 
            // a robust Ledger structure or synced coordination to prevent inconsistent app state.
            Result.success(transactionId)
        } catch (e: Exception) {
            Result.failure(PersistenceException("Failed to persist offline transaction to Room", e))
        }
    }

    override fun validateTransaction(transactionId: String): Boolean {
        return transactionId.isNotEmpty()
    }

    override fun generateTransactionId(): String {
        return "TXN-" + UUID.randomUUID().toString().uppercase().replace("-", "").take(12)
    }

    override fun generateClientOperationId(): String {
        return "OPC-" + UUID.randomUUID().toString()
    }

    override fun generateTransactionHash(payload: String): String {
        var hash = 0L
        for (char in payload) {
            hash = 31 * hash + char.code.toLong()
        }
        return hash.toString()
    }

    override fun generateDigitalSignature(payload: String): String {
        return "SIG-" + generateTransactionHash(payload)
    }
}
