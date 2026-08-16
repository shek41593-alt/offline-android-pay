package com.lastmilebanking.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Index

@Entity(
    tableName = "transactions",
    indices = [
        Index("walletId", "createdAt"),
        Index("isSynced", "createdAt")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String,
    val walletId: String,
    val senderId: String,
    val receiverId: String,
    val receiverName: String,
    val amount: Double,
    val currency: String = "INR",
    val transactionType: String,     // SEND, RECEIVE, TOPUP, WITHDRAW
    val paymentMode: String,         // QR, BLUETOOTH, SMS, ONLINE
    val status: String,              // PENDING, COMPLETED, FAILED, SYNCED
    val encryptedPayload: String = "",
    val transactionHash: String = "",
    val note: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long = 0L
)
