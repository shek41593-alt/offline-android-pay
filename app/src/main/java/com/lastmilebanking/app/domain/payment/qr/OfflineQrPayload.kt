package com.lastmilebanking.app.domain.payment.qr

object OfflineQrPayload {

    /**
     * Creates a deterministic canonical representation used for signing.
     * Rules:
     * - Fixed field order: version|type|merchantId|merchantWalletId|amount|currency|clientOperationId|timestamp|nonce|paymentMode
     * - UTF-8 explicit encoding assumed downstream
     * - Pipe delimited
     * - No extra spaces
     */
    fun buildCanonicalPayload(request: OfflineQrPaymentRequest): String {
        return buildString {
            append(request.version)
            append("|")
            append(request.type)
            append("|")
            append(request.merchantId)
            append("|")
            append(request.merchantWalletId)
            append("|")
            append(request.amount)
            append("|")
            append(request.currency)
            append("|")
            append(request.clientOperationId)
            append("|")
            append(request.timestamp)
            append("|")
            append(request.nonce)
            append("|")
            append(request.paymentMode)
        }
    }
}
