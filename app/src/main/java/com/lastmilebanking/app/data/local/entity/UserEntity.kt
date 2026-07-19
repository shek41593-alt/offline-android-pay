package com.lastmilebanking.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val accountNumber: String,
    val ifscCode: String,
    val bankName: String,
    val kycStatus: String = "PENDING",  // PENDING, VERIFIED
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
