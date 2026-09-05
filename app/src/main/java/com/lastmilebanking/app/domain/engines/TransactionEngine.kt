package com.lastmilebanking.app.domain.engines

interface TransactionEngine {
    suspend fun createOfflineTransaction(
        senderWalletId: String,
        receiverWalletId: String,
        amount: Double,
        transactionType: String,
        clientOperationId: String? = null
    ): Result<String>
    suspend fun createTransaction(senderId: String, receiverId: String, amount: Double, type: String, paymentMode: String = "SMS"): Result<String>
    fun validateTransaction(transactionId: String): Boolean
    fun generateTransactionId(): String
    fun generateClientOperationId(): String
    fun generateTransactionHash(payload: String): String
    fun generateDigitalSignature(payload: String): String
}
