package com.lastmilebanking.app.domain.payment.bluetooth

data class TransportEnvelope(
    val version: Int = 1,
    val messageType: String,
    val messageId: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)
