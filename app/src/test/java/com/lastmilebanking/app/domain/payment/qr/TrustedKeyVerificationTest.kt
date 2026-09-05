package com.lastmilebanking.app.domain.payment.qr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.PublicKey

class TrustedKeyVerificationTest {
    @Test
    fun `Test 1 - Validation successfully executed mapping Isolated Constraints properly testing`() {
        val kpGenerator = KeyPairGenerator.getInstance("EC")
        kpGenerator.initialize(256)
        
        // Simulating 2 distinct Android devices securely.
        val merchantDeviceAKeyPair = kpGenerator.generateKeyPair()
        val customerDeviceBKeyPair = kpGenerator.generateKeyPair()
        
        val signerA = object : OfflineQrSigner {
            override fun sign(canonicalPayload: String): String {
                val signature = java.security.Signature.getInstance("SHA256withECDSA")
                signature.initSign(merchantDeviceAKeyPair.private)
                signature.update(canonicalPayload.toByteArray(Charsets.UTF_8))
                return java.util.Base64.getEncoder().encodeToString(signature.sign())
            }
        }
        
        fun buildVerifier(key: PublicKey): OfflineQrVerifier {
            return AndroidKeystoreQrVerifier { key }
        }

        val request = OfflineQrPaymentRequest(
            version = 1, type = "LMB_PAYMENT_REQUEST", merchantId = "M_1", merchantWalletId = "W_1",
            amount = "100.00", currency = "INR", clientOperationId = "O_1", timestamp = System.currentTimeMillis(),
            nonce = "N_1", paymentMode = "OFFLINE_QR"
        )
        
        val canonical = OfflineQrPayload.buildCanonicalPayload(request)
        val sigA = signerA.sign(canonical)
        
        // 1. Device B customer verifier -> Uses TRUSTED REGISTRY KEY A -> SUCCESS
        assertTrue(buildVerifier(merchantDeviceAKeyPair.public).verify(canonical, sigA, "M_1"))
        
        // 2. Device A signer -> Device B local Keystore public key -> verify -> FAILURE
        assertFalse(buildVerifier(customerDeviceBKeyPair.public).verify(canonical, sigA, "M_1"))
        
        // Test malformed permutations smoothly checking implicitly testing
        assertFalse(buildVerifier(merchantDeviceAKeyPair.public).verify(OfflineQrPayload.buildCanonicalPayload(request.copy(amount = "10.00")), sigA, "M_1"))
        assertFalse(buildVerifier(merchantDeviceAKeyPair.public).verify(OfflineQrPayload.buildCanonicalPayload(request.copy(merchantId = "M_2")), sigA, "M_1"))
        assertFalse(buildVerifier(merchantDeviceAKeyPair.public).verify(OfflineQrPayload.buildCanonicalPayload(request.copy(nonce = "N_2")), sigA, "M_1"))
    }
}
