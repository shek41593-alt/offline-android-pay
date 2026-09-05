package com.lastmilebanking.app.domain.payment.qr

import java.math.BigDecimal

sealed class QrValidationResult {
    object Valid : QrValidationResult()
    data class Invalid(val reason: String) : QrValidationResult()
}

class OfflineQrValidator(
    private val verifier: OfflineQrVerifier,
    private val currentUserWalletId: String
) {

    // 1 hour expiration for demo
    private val EXPIRATION_MILLIS = 3600_000L

    fun validate(request: OfflineQrPaymentRequest): QrValidationResult {
        if (request.version != 1) return QrValidationResult.Invalid("Unsupported version")
        if (request.type != "LMB_PAYMENT_REQUEST") return QrValidationResult.Invalid("Invalid QR type")
        if (request.merchantId.isBlank()) return QrValidationResult.Invalid("Missing merchant identity")
        if (request.merchantWalletId.isBlank()) return QrValidationResult.Invalid("Missing merchant wallet")
        
        val parsedAmount = try {
            BigDecimal(request.amount)
        } catch (e: Exception) {
            return QrValidationResult.Invalid("Malformed amount")
        }
        if (parsedAmount <= BigDecimal.ZERO) return QrValidationResult.Invalid("Amount must be greater than zero")
        
        if (request.currency != "INR") return QrValidationResult.Invalid("Unsupported currency")
        
        if (request.clientOperationId.isBlank()) return QrValidationResult.Invalid("Missing operation ID")
        if (request.nonce.isBlank()) return QrValidationResult.Invalid("Missing nonce")
        
        if (request.paymentMode != "OFFLINE_QR") return QrValidationResult.Invalid("Unsupported payment mode")
        
        if (request.merchantWalletId == currentUserWalletId) {
            return QrValidationResult.Invalid("Sender cannot be the merchant")
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - request.timestamp > EXPIRATION_MILLIS) {
            return QrValidationResult.Invalid("QR code expired")
        }
        
        // Minor clock drift buffer (5 mins)
        if (request.timestamp > currentTime + 300_000L) {
            return QrValidationResult.Invalid("Timestamp in the future")
        }

        if (request.signature.isNullOrBlank()) {
            return QrValidationResult.Invalid("Missing signature")
        }

        val canonical = OfflineQrPayload.buildCanonicalPayload(request)
        if (!verifier.verify(canonical, request.signature, request.merchantId)) {
            return QrValidationResult.Invalid("Invalid signature")
        }

        return QrValidationResult.Valid
    }
}
