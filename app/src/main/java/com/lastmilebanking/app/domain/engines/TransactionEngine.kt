package com.lastmilebanking.app.domain.engines

interface TransactionEngine {
    suspend fun createTransaction(senderId: String, receiverId: String, amount: Double, type: String): Result<String>
    fun validateTransaction(transactionId: String): Boolean
    fun generateTransactionId(): String
    fun generateTransactionHash(payload: String): String
    fun generateDigitalSignature(payload: String): String
}
