package com.lastmilebanking.app.domain.payment.qr

data class OfflineQrPaymentProof(
    val version: Int = 1,
    val type: String = "LMB_PAYMENT_PROOF",
    val clientOperationId: String,
    val transactionId: String,
    val merchantId: String,
    val merchantWalletId: String,
    val customerWalletId: String,
    val amount: String,
    val currency: String,
    val timestamp: Long,
    val nonce: String,
    val paymentMode: String = "OFFLINE_QR",
    val signature: String? = null
)
