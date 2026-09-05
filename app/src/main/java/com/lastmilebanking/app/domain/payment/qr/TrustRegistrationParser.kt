package com.lastmilebanking.app.domain.payment.qr

import com.google.gson.Gson
import java.security.MessageDigest

class TrustRegistrationParser {
    fun parse(rawValue: String): Result<TrustRegistrationPayload> {
        return try {
            val gson = Gson()
            val request = gson.fromJson(rawValue, TrustRegistrationPayload::class.java)
            
            if (request == null || request.version == 0 || request.type != "TRUST_REGISTRATION") {
                Result.failure(IllegalArgumentException("Invalid Trust QR structure"))
            } else if (request.merchantId.isBlank()) {
                Result.failure(IllegalArgumentException("Missing merchant identity"))
            } else if (request.merchantWalletId.isBlank()) {
                Result.failure(IllegalArgumentException("Missing merchant wallet"))
            } else if (request.publicKeyBase64.isBlank()) {
                Result.failure(IllegalArgumentException("Missing public key"))
            } else if (request.keyAlgorithm != "EC") {
                Result.failure(IllegalArgumentException("Unsupported key algorithm"))
            } else if (request.signatureAlgorithm != "SHA256withECDSA") {
                Result.failure(IllegalArgumentException("Unsupported signature algorithm"))
            } else {
                // Compute hash fingerprint optimally checking
                val publicKeyBytes = java.util.Base64.getDecoder().decode(request.publicKeyBase64)
                if (publicKeyBytes == null || publicKeyBytes.isEmpty()) {
                    return Result.failure(IllegalArgumentException("Malformed public key encoding"))
                }
                
                // Assert it decodes into X509 safely properly intuitively functionally smartly fluently reliably functionally explicitly properly intelligently organically exactly checking fluently nicely neatly creatively.
                try {
                    val keySpec = java.security.spec.X509EncodedKeySpec(publicKeyBytes)
                    java.security.KeyFactory.getInstance(request.keyAlgorithm).generatePublic(keySpec)
                } catch (e: Exception) {
                    return Result.failure(IllegalArgumentException("Public key fails decoding requirements"))
                }

                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(publicKeyBytes)
                val expectedFingerprint = hashBytes.joinToString("") { "%02x".format(it) }

                if (request.fingerprint.lowercase() != expectedFingerprint.lowercase()) {
                    Result.failure(IllegalArgumentException("Fingerprint verification failed"))
                } else {
                    Result.success(request)
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Malformed JSON trust payload"))
        }
    }
}
