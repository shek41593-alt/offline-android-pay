package com.lastmilebanking.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

import androidx.room.Index

@Entity(
    tableName = "wallet",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class WalletEntity(
    @PrimaryKey
    val walletId: String,
    val userId: String,
    val availableBalance: Double = 0.0,
    val offlineBalance: Double = 0.0,      // Balance reserved for offline use
    val pendingBalance: Double = 0.0,       // Pending sync amount
    val dailyOfflineLimit: Double = 5000.0, // Max offline spend per day
    val usedOfflineLimitToday: Double = 0.0,
    val currency: String = "INR",
    val lastSyncedAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
