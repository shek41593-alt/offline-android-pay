package com.lastmilebanking.app.domain.payment.qr

object OfflineQrPaymentProofPayload {
    fun buildCanonicalPayload(proof: OfflineQrPaymentProof): String {
        return "${proof.version}|${proof.type}|${proof.clientOperationId}|${proof.transactionId}|${proof.merchantId}|${proof.merchantWalletId}|${proof.customerWalletId}|${proof.amount}|${proof.currency}|${proof.timestamp}|${proof.nonce}|${proof.paymentMode}"
    }
}
