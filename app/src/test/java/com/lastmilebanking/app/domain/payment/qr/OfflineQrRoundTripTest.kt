package com.lastmilebanking.app.domain.payment.qr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQrRoundTripTest {

    @Test
    fun `Test 22 - Signature Round Trip Evaluation`() {
        // Deterministic fixture without hitting Android Keystore in JVM tests
        val signer = object : OfflineQrSigner {
            override fun sign(canonicalPayload: String): String {
                return "MOCK_SIG:[$canonicalPayload]"
            }
        }
        val verifier = object : OfflineQrVerifier {
            override fun verify(canonicalPayload: String, signature: String, publicKeyIdentifier: String): Boolean {
                return signature == "MOCK_SIG:[$canonicalPayload]"
            }
        }

        val request = OfflineQrPaymentRequest(
            version = 1,
            type = "LMB_PAYMENT_REQUEST",
            merchantId = "MERCHANT_TEST",
            merchantWalletId = "WALLET_MERCHANT",
            amount = "100.00",
            currency = "INR",
            clientOperationId = "OPC-TEST-001",
            timestamp = 1700000000000L,
            nonce = "deterministic test nonce",
            paymentMode = "OFFLINE_QR"
        )
        
        val canonicalPayload = OfflineQrPayload.buildCanonicalPayload(request)
        val signature = signer.sign(canonicalPayload)
        
        val signedRequest = request.copy(signature = signature)
        val signedCanonical = OfflineQrPayload.buildCanonicalPayload(signedRequest)
        
        // Ensure successful roundtrip
        assertTrue(verifier.verify(signedCanonical, signature, "MERCHANT_TEST"))

        // Malicious attempt: Tamper Amount
        val maliciousRequest = signedRequest.copy(amount = "1000.00")
        val maliciousCanonical = OfflineQrPayload.buildCanonicalPayload(maliciousRequest)
        
        // Original signature should fail with malicious canonical payload
        assertFalse(verifier.verify(maliciousCanonical, signature, "MERCHANT_TEST"))
    }
}
