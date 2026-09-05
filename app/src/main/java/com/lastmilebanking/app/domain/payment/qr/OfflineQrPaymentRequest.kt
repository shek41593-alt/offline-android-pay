package com.lastmilebanking.app.domain.payment.qr

/**
 * Represents a cryptographically verifiable offline payment request.
 * Not a final settled transaction.
 */
data class OfflineQrPaymentRequest(
    val version: Int,
    val type: String,
    val merchantId: String,
    val merchantWalletId: String,
    val amount: String, // String to avoid floating point precision loss
    val currency: String,
    val clientOperationId: String,
    val timestamp: Long,
    val nonce: String,
    val paymentMode: String,
    val signature: String? = null
)
