package com.lastmilebanking.app.domain.payment.qr

import com.google.gson.Gson

class OfflineQrPaymentProofParser {
    fun parse(rawValue: String): Result<OfflineQrPaymentProof> {
        return try {
            val gson = Gson()
            val proof = gson.fromJson(rawValue, OfflineQrPaymentProof::class.java)
            if (proof == null || proof.version == 0 || proof.type != "LMB_PAYMENT_PROOF") {
                Result.failure(IllegalArgumentException("Invalid proof QR structure"))
            } else {
                Result.success(proof)
            }
        } catch (e: Exception) {
             Result.failure(IllegalArgumentException("Malformed JSON payload"))
        }
    }
}
