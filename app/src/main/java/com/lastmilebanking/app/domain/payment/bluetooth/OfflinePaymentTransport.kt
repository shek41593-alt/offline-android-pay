package com.lastmilebanking.app.domain.payment.bluetooth

import kotlinx.coroutines.flow.Flow

interface OfflinePaymentTransport {
    val incomingMessages: Flow<TransportEnvelope>
    val connectionState: Flow<ConnectionState>
    
    suspend fun advertiseAndAccept(serviceName: String, serviceUuid: java.util.UUID)
    suspend fun discoverAndConnect(serviceUuid: java.util.UUID)
    suspend fun send(message: TransportEnvelope)
    fun disconnect()
}

enum class ConnectionState {
    DISCONNECTED,
    ADVERTISING,
    DISCOVERING,
    CONNECTED
}
