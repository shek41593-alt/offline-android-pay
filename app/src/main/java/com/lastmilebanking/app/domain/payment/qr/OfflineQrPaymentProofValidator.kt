package com.lastmilebanking.app.domain.payment.qr

import java.math.BigDecimal

class OfflineQrPaymentProofValidator(
    private val verifier: OfflineQrVerifier,
    private val originalRequest: OfflineQrPaymentRequest?
) {
    private val EXPIRATION_MILLIS = 3600_000L

    fun validate(proof: OfflineQrPaymentProof): QrValidationResult {
        if (proof.version != 1) return QrValidationResult.Invalid("Unsupported version")
        if (proof.type != "LMB_PAYMENT_PROOF") return QrValidationResult.Invalid("Invalid proof type")
        
        if (proof.clientOperationId.isBlank()) return QrValidationResult.Invalid("Missing operation ID")
        if (proof.merchantId.isBlank()) return QrValidationResult.Invalid("Missing merchant identity")
        if (proof.merchantWalletId.isBlank()) return QrValidationResult.Invalid("Missing merchant wallet")
        if (proof.customerWalletId.isBlank()) return QrValidationResult.Invalid("Missing customer wallet")

        val parsedAmount = try { BigDecimal(proof.amount) } catch (e: Exception) { return QrValidationResult.Invalid("Malformed amount") }
        if (parsedAmount <= BigDecimal.ZERO) return QrValidationResult.Invalid("Amount must be greater than zero")
        if (proof.currency != "INR") return QrValidationResult.Invalid("Unsupported currency")
        
        if (proof.nonce.isBlank()) return QrValidationResult.Invalid("Missing nonce")
        if (proof.paymentMode != "OFFLINE_QR") return QrValidationResult.Invalid("Unsupported payment mode")
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - proof.timestamp > EXPIRATION_MILLIS) return QrValidationResult.Invalid("Proof expired")
        if (proof.timestamp > currentTime + 300_000L) return QrValidationResult.Invalid("Timestamp in the future")

        if (originalRequest != null) {
            if (proof.clientOperationId != originalRequest.clientOperationId) return QrValidationResult.Invalid("Payment proof does not match payment request")
            if (proof.merchantWalletId != originalRequest.merchantWalletId) return QrValidationResult.Invalid("Payment proof does not match payment request")
            if (proof.merchantId != originalRequest.merchantId) return QrValidationResult.Invalid("Payment proof does not match payment request")
            if (proof.amount != originalRequest.amount) return QrValidationResult.Invalid("Payment proof does not match payment request")
            if (proof.currency != originalRequest.currency) return QrValidationResult.Invalid("Payment proof does not match payment request")
            if (proof.paymentMode != originalRequest.paymentMode) return QrValidationResult.Invalid("Payment proof does not match payment request")
        }

        if (proof.signature.isNullOrBlank()) return QrValidationResult.Invalid("Missing signature")
        val canonical = OfflineQrPaymentProofPayload.buildCanonicalPayload(proof)
        if (!verifier.verify(canonical, proof.signature, proof.customerWalletId)) {
            return QrValidationResult.Invalid("Invalid signature")
        }

        return QrValidationResult.Valid
    }
}
