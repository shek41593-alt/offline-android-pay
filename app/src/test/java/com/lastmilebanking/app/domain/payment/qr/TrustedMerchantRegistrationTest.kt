package com.lastmilebanking.app.domain.payment.qr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.google.gson.Gson
import java.security.KeyPairGenerator
import java.security.MessageDigest

class TrustedMerchantRegistrationTest {

    @Test
    fun `Evaluate Trust Registration Parser flawlessly intelligently dependably tracking logically comfortably efficiently successfully`() {
        val kpGenerator = KeyPairGenerator.getInstance("EC")
        kpGenerator.initialize(256)
        
        val merchantKeyPair = kpGenerator.generateKeyPair()
        val publicKeyEncoded = merchantKeyPair.public.encoded
        val publicKeyBase64 = java.util.Base64.getEncoder().encodeToString(publicKeyEncoded)
        
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(publicKeyEncoded)
        val fingerprint = hashBytes.joinToString("") { "%02x".format(it) }
        
        val validPayload = TrustRegistrationPayload(
            version = 1,
            type = "TRUST_REGISTRATION",
            merchantId = "MERCHANT_A",
            merchantWalletId = "WALLET_A",
            publicKeyBase64 = publicKeyBase64,
            keyAlgorithm = "EC",
            signatureAlgorithm = "SHA256withECDSA",
            fingerprint = fingerprint,
            createdAt = System.currentTimeMillis()
        )
        
        val parser = TrustRegistrationParser()
        val gson = Gson()
        
        // TEST 1: Valid parsed successfully
        val result1 = parser.parse(gson.toJson(validPayload))
        assertTrue(result1.isSuccess)
        
        // TEST 2: Malformed publicKeyRejected
        val maliciousKey = validPayload.copy(publicKeyBase64 = "NOT_BASE64_ABC")
        assertTrue(parser.parse(gson.toJson(maliciousKey)).isFailure)
        
        // TEST 3: Wrong fingerprint rejected
        val maliciousFingerprint = validPayload.copy(fingerprint = "00e7ff910")
        assertTrue(parser.parse(gson.toJson(maliciousFingerprint)).isFailure)
        
        // TEST 4: Wrong payload type rejected
        val wrongType = validPayload.copy(type = "LMB_PAYMENT_REQUEST")
        assertTrue(parser.parse(gson.toJson(wrongType)).isFailure)
        
        // TEST 13: Customer limits -> merchant validates gracefully
        val request = OfflineQrPaymentRequest(
            version = 1, type = "LMB_PAYMENT_REQUEST", merchantId = "MERCHANT_A", merchantWalletId = "WALLET_A",
            amount = "100.00", currency = "INR", clientOperationId = "OPC-123", timestamp = System.currentTimeMillis(),
            nonce = "NONCE_TEST", paymentMode = "OFFLINE_QR"
        )
        
        val signer = object : OfflineQrSigner {
            override fun sign(canonicalPayload: String): String {
                val sig = java.security.Signature.getInstance("SHA256withECDSA")
                sig.initSign(merchantKeyPair.private)
                sig.update(canonicalPayload.toByteArray(Charsets.UTF_8))
                return java.util.Base64.getEncoder().encodeToString(sig.sign())
            }
        }
        
        val canonical = OfflineQrPayload.buildCanonicalPayload(request)
        val signature = signer.sign(canonical)
        
        val verifierValid = AndroidKeystoreQrVerifier { merchantKeyPair.public }
        assertTrue(verifierValid.verify(canonical, signature, "MERCHANT_A"))
        
        val kpGeneratorB = KeyPairGenerator.getInstance("EC")
        kpGeneratorB.initialize(256)
        val verifierWrong = AndroidKeystoreQrVerifier { kpGeneratorB.generateKeyPair().public }
        assertFalse(verifierWrong.verify(canonical, signature, "MERCHANT_A"))
    }
}
