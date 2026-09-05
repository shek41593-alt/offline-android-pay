package com.lastmilebanking.app.domain.payment.qr

import com.google.gson.Gson

class OfflineQrPaymentParser {
    fun parse(rawValue: String): Result<OfflineQrPaymentRequest> {
        return try {
            val gson = Gson()
            val request = gson.fromJson(rawValue, OfflineQrPaymentRequest::class.java)
            if (request == null || request.version == 0 || request.type != "LMB_PAYMENT_REQUEST") {
                Result.failure(IllegalArgumentException("Invalid QR structure"))
            } else {
                Result.success(request)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Malformed JSON payload"))
        }
    }
}
