package com.lastmilebanking.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_merchant_keys")
data class TrustedMerchantKeyEntity(
    @PrimaryKey
    val merchantId: String,
    val merchantWalletId: String,
    val publicKeyBase64: String,
    val keyAlgorithm: String = "EC",
    val signatureAlgorithm: String = "SHA256withECDSA",
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
