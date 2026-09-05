package com.lastmilebanking.app.domain.payment.qr

import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQrPaymentParserTest {

    private val parser = OfflineQrPaymentParser()

    @Test
    fun `Malformed JSON Payload`() {
        val result = parser.parse("{bad_json")
        assertTrue(result.isFailure)
    }

    @Test
    fun `Missing Version`() {
        val json = """{"type": "LMB_PAYMENT_REQUEST", "merchantId": "test"}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `Unknown Payload Type`() {
        val json = """{"version": 1, "type": "UNKNOWN"}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `Valid Payload`() {
        val json = """{"version": 1, "type": "LMB_PAYMENT_REQUEST", "merchantId": "M1", "merchantWalletId": "W1", "amount": "100", "currency": "INR", "clientOperationId": "123", "timestamp": 12345, "nonce": "abc", "paymentMode": "OFFLINE_QR", "signature": "xyz"}"""
        val result = parser.parse(json)
        assertTrue(result.isSuccess)
    }
}
