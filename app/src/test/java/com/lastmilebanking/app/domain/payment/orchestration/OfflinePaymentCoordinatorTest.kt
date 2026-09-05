package com.lastmilebanking.app.domain.payment.orchestration

import com.lastmilebanking.app.domain.payment.bluetooth.OfflinePaymentTransport
import com.lastmilebanking.app.domain.payment.bluetooth.TransportEnvelope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class OfflinePaymentCoordinatorTest {

    class MockTransport : OfflinePaymentTransport {
        private val _incoming = MutableSharedFlow<TransportEnvelope>()
        override val incomingMessages = _incoming.asSharedFlow()
        
        private val _state = MutableStateFlow(com.lastmilebanking.app.domain.payment.bluetooth.ConnectionState.DISCONNECTED)
        override val connectionState = _state.asStateFlow()
        
        override suspend fun advertiseAndAccept(serviceName: String, serviceUuid: UUID) {
            _state.value = com.lastmilebanking.app.domain.payment.bluetooth.ConnectionState.CONNECTED
        }
        
        override suspend fun discoverAndConnect(serviceUuid: UUID) {
            _state.value = com.lastmilebanking.app.domain.payment.bluetooth.ConnectionState.CONNECTED
        }
        
        override suspend fun send(message: TransportEnvelope) {}
        override fun disconnect() {
            _state.value = com.lastmilebanking.app.domain.payment.bluetooth.ConnectionState.DISCONNECTED
        }
    }

    @Test
    fun `Test QR transport produces correct initial validation semantics`() = runTest {
        val qrTransport = MockTransport()
        val btTransport = MockTransport()
        val coordinator = OfflinePaymentCoordinator(btTransport, qrTransport)
        
        coordinator.selectTransport(TransportType.QR)
        assertEquals(PaymentState.IDLE, coordinator.paymentState.value)
        
        coordinator.customerStartPaying(UUID.randomUUID())
        assertEquals(PaymentState.WAITING_FOR_CONNECTION, coordinator.paymentState.value)
    }

    @Test
    fun `Test Bluetooth transport fails safely maintaining idempotency gracefully`() = runTest {
        val qrTransport = MockTransport()
        val btTransport = MockTransport()
        val coordinator = OfflinePaymentCoordinator(btTransport, qrTransport)
        
        coordinator.selectTransport(TransportType.BLUETOOTH)
        
        coordinator.handleProtocolError("Signature Invalid")
        assertEquals(PaymentState.REQUIRES_ATTENTION, coordinator.paymentState.value)
    }
}
