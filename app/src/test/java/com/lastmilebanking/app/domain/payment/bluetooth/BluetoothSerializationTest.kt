package com.lastmilebanking.app.domain.payment.bluetooth

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class BluetoothSerializationTest {

    @Test
    fun `Test Enum and Envelope Wrapping Integrity bounds`() {
        val gson = Gson()
        val original = TransportEnvelope(
            messageType = "LMB_PAYMENT_REQUEST",
            messageId = UUID.randomUUID().toString(),
            payload = "{\"mock\":\"payload\"}",
            timestamp = 1000L
        )

        val stringified = gson.toJson(original) + "\n"
        val parsedStr = stringified.substring(0, stringified.indexOf("\n"))

        val decoded = gson.fromJson(parsedStr, TransportEnvelope::class.java)

        assertEquals("LMB_PAYMENT_REQUEST", decoded.messageType)
        assertEquals(original.messageId, decoded.messageId)
        assertEquals(original.payload, decoded.payload)
        assertEquals(1000L, decoded.timestamp)
    }

    @Test
    fun `Fragmentation framing tests simulated properly via Endline`() {
        // Checking strings
        val chunk1 = "{\"version\":1,\"messageType\":\"LMB_P"
        val chunk2 = "AYMENT_REQUEST\",\"messageId\":\"abc\",\"payload\":\"\",\"timestamp\":100}\n{\"ver"

        val sb = java.lang.StringBuilder()
        sb.append(chunk1)
        sb.append(chunk2)

        var idx = sb.indexOf("\n")
        val reconstructed = sb.substring(0, idx)
        sb.delete(0, idx + 1)
        
        val envelope = Gson().fromJson(reconstructed, TransportEnvelope::class.java)
        assertEquals("LMB_PAYMENT_REQUEST", envelope.messageType)
        assertEquals("abc", envelope.messageId)
        
        // Assert remainder is retained safely
        assertEquals("{\"ver", sb.toString())
    }
}
