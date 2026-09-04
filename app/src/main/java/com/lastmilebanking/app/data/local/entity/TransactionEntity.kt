package com.lastmilebanking.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index("walletId", "createdAt"),
        Index("isSynced", "createdAt"),
        Index(value = ["clientOperationId"], unique = true),
        Index("status"),
        Index("createdAt")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String,
    
    // Legacy/UI fields kept for compatibility
    val walletId: String,
    val senderId: String,
    val receiverId: String,
    val receiverName: String,
    val currency: String = "INR",
    val paymentMode: String,
    val encryptedPayload: String = "",
    val note: String = "",
    val isSynced: Boolean = false,
    
    // Phase 2.1 OFFLINE TRANSACTION DATA MODEL fields
    val clientOperationId: String = UUID.randomUUID().toString(),
    val senderWalletId: String = "",
    val receiverWalletId: String = "",
    val amount: Double,
    val transactionType: String,     // SEND, RECEIVE, TOPUP, WITHDRAW
    val status: String,              // PENDING, COMPLETED, FAILED, SYNCED
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val retryCount: Int = 0,
    val transactionHash: String = "",
    val failureReason: String? = null
)
