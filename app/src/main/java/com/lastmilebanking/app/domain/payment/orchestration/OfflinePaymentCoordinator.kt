package com.lastmilebanking.app.domain.payment.orchestration

import com.lastmilebanking.app.domain.payment.bluetooth.OfflinePaymentTransport
import com.lastmilebanking.app.domain.payment.bluetooth.TransportEnvelope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class TransportType { QR, BLUETOOTH }

enum class PaymentState {
    IDLE,
    WAITING_FOR_CONNECTION,
    RECEIVING_PAYMENT_REQUEST,
    VERIFYING_PAYMENT,
    PAYMENT_RECORDED_OFFLINE,
    PAYMENT_PROOF_VERIFIED,
    WAITING_FOR_SYNCHRONIZATION,
    PAYMENT_SETTLED,
    REQUIRES_ATTENTION
}

class OfflinePaymentCoordinator(
    private val bluetoothTransport: OfflinePaymentTransport,
    private val qrTransport: OfflinePaymentTransport // Adapter over the scanner
) {
    private val _paymentState = MutableStateFlow(PaymentState.IDLE)
    val paymentState = _paymentState.asStateFlow()

    private var currentTransport: OfflinePaymentTransport? = null

    suspend fun selectTransport(type: TransportType) {
        currentTransport = when (type) {
            TransportType.QR -> qrTransport
            TransportType.BLUETOOTH -> bluetoothTransport
        }
    }

    suspend fun merchantStartReceiving(amount: String, serviceUuid: UUID) {
        _paymentState.value = PaymentState.WAITING_FOR_CONNECTION
        currentTransport?.advertiseAndAccept("LMB_MERCHANT", serviceUuid)
        
        // Wait for connection and manage merchant flow:
        // 1. send request (LMB_PAYMENT_REQUEST)
        // 2. await proof
        // 3. verifying payment
        // 4. proof verified
        // 5. waiting for sync
    }

    suspend fun customerStartPaying(serviceUuid: UUID) {
        _paymentState.value = PaymentState.WAITING_FOR_CONNECTION
        currentTransport?.discoverAndConnect(serviceUuid)
        
        // Wait for connection and manage customer flow:
        // 1. await request
        // 2. verify request
        // 3. create transaction
        // 4. generate proof (LMB_PAYMENT_PROOF)
        // 5. send proof
        // 6. waiting for sync
    }

    fun handleProtocolError(reason: String) {
        _paymentState.value = PaymentState.REQUIRES_ATTENTION
        currentTransport?.disconnect()
    }
}
