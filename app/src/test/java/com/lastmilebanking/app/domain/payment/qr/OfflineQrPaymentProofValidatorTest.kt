package com.lastmilebanking.app.domain.payment.qr

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class OfflineQrPaymentProofValidatorTest {

    private val fakeVerifier = object : OfflineQrVerifier {
        override fun verify(canonicalPayload: String, signature: String, publicKeyIdentifier: String): Boolean {
            return signature == "MOCK_SIG:[$canonicalPayload]"
        }
    }
    
    private val fakeSigner = object : OfflineQrSigner {
        override fun sign(canonicalPayload: String): String {
            return "MOCK_SIG:[$canonicalPayload]"
        }
    }

    private val originalReq = OfflineQrPaymentRequest(
        version = 1,
        type = "LMB_PAYMENT_REQUEST",
        merchantId = "MERCHANT_A",
        merchantWalletId = "WALLET_M",
        amount = "500",
        currency = "INR",
        clientOperationId = "OPC-123",
        timestamp = System.currentTimeMillis() - 10000,
        nonce = "REQ_NONCE",
        paymentMode = "OFFLINE_QR"
    )

    private val validator = OfflineQrPaymentProofValidator(fakeVerifier, originalReq)

    private fun createValidProof(): OfflineQrPaymentProof {
        val p = OfflineQrPaymentProof(
            version = 1,
            type = "LMB_PAYMENT_PROOF",
            clientOperationId = "OPC-123",
            transactionId = "TXN-001",
            merchantId = "MERCHANT_A",
            merchantWalletId = "WALLET_M",
            customerWalletId = "WALLET_C",
            amount = "500",
            currency = "INR",
            timestamp = System.currentTimeMillis(),
            nonce = "PROOF_NONCE",
            paymentMode = "OFFLINE_QR"
        )
        return p.copy(signature = fakeSigner.sign(OfflineQrPaymentProofPayload.buildCanonicalPayload(p)))
    }

    @Test
    fun `1 - Valid payment proof`() {
        assertEquals(QrValidationResult.Valid, validator.validate(createValidProof()))
    }

    @Test
    fun `4 - Invalid signature`() {
        val p = createValidProof().copy(signature = "BAD_SIG")
        assertEquals(QrValidationResult.Invalid("Invalid signature"), validator.validate(p))
    }

    @Test
    fun `5 - Amount tampering`() {
        val p = createValidProof().copy(amount = "1000")
        assertEquals(QrValidationResult.Invalid("Payment proof does not match payment request"), validator.validate(p))
    }

    @Test
    fun `16 - Wrong merchant`() {
        val p = createValidProof().copy(merchantId = "MERCHANT_B")
        assertEquals(QrValidationResult.Invalid("Payment proof does not match payment request"), validator.validate(p))
    }

    @Test
    fun `18 - Wrong payment request matcher`() {
        val p = createValidProof().copy(clientOperationId = "OPC-456")
        assertEquals(QrValidationResult.Invalid("Payment proof does not match payment request"), validator.validate(p))
    }

    @Test
    fun `15 - Expired proof`() {
        val p = createValidProof().copy(timestamp = System.currentTimeMillis() - 4000_000L)
        val signed = p.copy(signature = fakeSigner.sign(OfflineQrPaymentProofPayload.buildCanonicalPayload(p)))
        assertEquals(QrValidationResult.Invalid("Proof expired"), validator.validate(signed))
    }
}
