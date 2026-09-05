package com.lastmilebanking.app.domain.payment.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OfflineQrValidatorTest {

    private val fakeVerifier = object : OfflineQrVerifier {
        override fun verify(canonicalPayload: String, signature: String, publicKeyIdentifier: String): Boolean {
            // Fake logic: Signature is valid if it equals "SIGNED_[" + canonicalPayload + "]"
            return signature == "SIGNED_[$canonicalPayload]"
        }
    }
    
    private val fakeSigner = object : OfflineQrSigner {
        override fun sign(canonicalPayload: String): String {
            return "SIGNED_[$canonicalPayload]"
        }
    }

    private val currentUserWalletId = "WALLET_CUSTOMER"
    private val validator = OfflineQrValidator(fakeVerifier, currentUserWalletId)

    private fun createValidRequest(): OfflineQrPaymentRequest {
        val req = OfflineQrPaymentRequest(
            version = 1,
            type = "LMB_PAYMENT_REQUEST",
            merchantId = "MERCHANT_123",
            merchantWalletId = "WALLET_MERCHANT",
            amount = "100.50",
            currency = "INR",
            clientOperationId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            nonce = "NONCE_ABC",
            paymentMode = "OFFLINE_QR"
        )
        return req.copy(signature = fakeSigner.sign(OfflineQrPayload.buildCanonicalPayload(req)))
    }

    @Test
    fun `1 & 18 - Valid QR Payload and Signature`() {
        val req = createValidRequest()
        assertEquals(QrValidationResult.Valid, validator.validate(req))
    }

    @Test
    fun `2 - Invalid QR version`() {
        val req = createValidRequest().copy(version = 2)
        assertEquals(QrValidationResult.Invalid("Unsupported version"), validator.validate(req))
    }

    @Test
    fun `3 - Invalid QR type`() {
        val req = createValidRequest().copy(type = "UNKNOWN")
        assertEquals(QrValidationResult.Invalid("Invalid QR type"), validator.validate(req))
    }

    @Test
    fun `4 - Missing merchant ID`() {
        val req = createValidRequest().copy(merchantId = "")
        assertEquals(QrValidationResult.Invalid("Missing merchant identity"), validator.validate(req))
    }

    @Test
    fun `5 - Missing merchant wallet`() {
        val req = createValidRequest().copy(merchantWalletId = "")
        assertEquals(QrValidationResult.Invalid("Missing merchant wallet"), validator.validate(req))
    }

    @Test
    fun `6 - Zero amount`() {
        val req = createValidRequest().copy(amount = "0.00")
        assertEquals(QrValidationResult.Invalid("Amount must be greater than zero"), validator.validate(req))
    }

    @Test
    fun `7 - Negative amount`() {
        val req = createValidRequest().copy(amount = "-10.0")
        assertEquals(QrValidationResult.Invalid("Amount must be greater than zero"), validator.validate(req))
    }

    @Test
    fun `8 - Unsupported currency`() {
        val req = createValidRequest().copy(currency = "USD")
        assertEquals(QrValidationResult.Invalid("Unsupported currency"), validator.validate(req))
    }

    @Test
    fun `9 - Missing operation ID`() {
        val req = createValidRequest().copy(clientOperationId = "")
        assertEquals(QrValidationResult.Invalid("Missing operation ID"), validator.validate(req))
    }

    @Test
    fun `10 - Missing nonce`() {
        val req = createValidRequest().copy(nonce = "")
        assertEquals(QrValidationResult.Invalid("Missing nonce"), validator.validate(req))
    }

    @Test
    fun `11 - Expired timestamp`() {
        // Expired > 1 hour
        val req = createValidRequest().copy(timestamp = System.currentTimeMillis() - 7200_000L)
        // Need to resign otherwise it'll fail signature validation first
        val signedReq = req.copy(signature = fakeSigner.sign(OfflineQrPayload.buildCanonicalPayload(req)))
        assertEquals(QrValidationResult.Invalid("QR code expired"), validator.validate(signedReq))
    }

    @Test
    fun `12 - Invalid signature`() {
        val req = createValidRequest().copy(signature = "TAMPERED_SIG")
        assertEquals(QrValidationResult.Invalid("Invalid signature"), validator.validate(req))
    }

    @Test
    fun `13 - Tampered modified amount`() {
        val original = createValidRequest()
        // Attacker changes amount without changing signature
        val malicious = original.copy(amount = "999.00")
        assertEquals(QrValidationResult.Invalid("Invalid signature"), validator.validate(malicious))
    }

    @Test
    fun `14 - Tampered modified merchant wallet`() {
        val original = createValidRequest()
        val malicious = original.copy(merchantWalletId = "ATTACKER_WALLET")
        assertEquals(QrValidationResult.Invalid("Invalid signature"), validator.validate(malicious))
    }

    @Test
    fun `15 - Tampered modified operation ID`() {
        val original = createValidRequest()
        val malicious = original.copy(clientOperationId = UUID.randomUUID().toString())
        assertEquals(QrValidationResult.Invalid("Invalid signature"), validator.validate(malicious))
    }

    @Test
    fun `16 - Tampered modified timestamp`() {
        val original = createValidRequest()
        val malicious = original.copy(timestamp = original.timestamp + 1000L)
        assertEquals(QrValidationResult.Invalid("Invalid signature"), validator.validate(malicious))
    }

    @Test
    fun `17 - Tampered modified currency`() {
        val original = createValidRequest()
        val malicious = original.copy(currency = "EUR")
        // Fails unsupported currency check FIRST because it checks structural before signature,
        // so to test signature failure explicitly, let's assume it bypasses or let's test it:
        // Wait, validator checks currency FIRST:
        assertEquals(QrValidationResult.Invalid("Unsupported currency"), validator.validate(malicious))
    }

    @Test
    fun `20 - Sender wallet == merchant wallet`() {
        val original = createValidRequest()
        // Customer accidentally scans their own QR
        val badValidator = OfflineQrValidator(fakeVerifier, original.merchantWalletId)
        assertEquals(QrValidationResult.Invalid("Sender cannot be the merchant"), badValidator.validate(original))
    }

    @Test
    fun `21 - Malformed QR amount formatting`() {
        val req = createValidRequest().copy(amount = "invalid_number")
        assertEquals(QrValidationResult.Invalid("Malformed amount"), validator.validate(req))
    }

    @Test
    fun `22 - Unsupported payment mode`() {
        val req = createValidRequest().copy(paymentMode = "CASH")
        assertEquals(QrValidationResult.Invalid("Unsupported payment mode"), validator.validate(req))
    }
}
