package com.lastmilebanking.app.domain.payment.qr

data class TrustRegistrationPayload(
    val version: Int,
    val type: String,
    val merchantId: String,
    val merchantWalletId: String,
    val publicKeyBase64: String,
    val keyAlgorithm: String,
    val signatureAlgorithm: String,
    val fingerprint: String,
    val createdAt: Long
)
