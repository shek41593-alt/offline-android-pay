package com.lastmilebanking.app.domain.payment.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothClassicTransport(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) : OfflinePaymentTransport {

    private val gson = Gson()
    
    private var serverSocket: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    private val _incomingMessages = kotlinx.coroutines.flow.MutableSharedFlow<TransportEnvelope>(extraBufferCapacity = 10)
    override val incomingMessages = _incomingMessages.asSharedFlow()

    override suspend fun advertiseAndAccept(serviceName: String, serviceUuid: UUID) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            throw IllegalStateException("Bluetooth not enabled")
        }
        withContext(Dispatchers.IO) {
            _connectionState.value = ConnectionState.ADVERTISING
            try {
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(serviceName, serviceUuid)
                val socket = serverSocket?.accept(60000) // 60 sec timeout
                socket?.let {
                    serverSocket?.close()
                    manageConnectedSocket(it)
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    override suspend fun discoverAndConnect(serviceUuid: UUID) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            throw IllegalStateException("Bluetooth not enabled")
        }
        _connectionState.value = ConnectionState.DISCOVERING
        withContext(Dispatchers.IO) {
            // Simplified discovery: find the first bonded device that successfully connects to the UUID.
            var connected = false
            for (device in bluetoothAdapter.bondedDevices) {
                try {
                    val socket = device.createInsecureRfcommSocketToServiceRecord(serviceUuid)
                    // bluetoothAdapter.cancelDiscovery() // Cancel discovery before connecting for better speed
                    socket.connect()
                    manageConnectedSocket(socket)
                    connected = true
                    break
                } catch (e: Exception) {
                    continue
                }
            }
            if (!connected) {
                _connectionState.value = ConnectionState.DISCONNECTED
                throw IllegalStateException("No compatible Merchant device found.")
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        activeSocket = socket
        inputStream = socket.inputStream
        outputStream = socket.outputStream
        _connectionState.value = ConnectionState.CONNECTED

        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            receiveLoop()
        }
    }

    private suspend fun receiveLoop() {
        val buffer = ByteArray(4096)
        val sb = java.lang.StringBuilder()
        try {
            while (true) {
                val bytes = inputStream?.read(buffer) ?: break
                if (bytes == -1) break
                if (bytes > 0) {
                    val chunk = String(buffer, 0, bytes, Charsets.UTF_8)
                    sb.append(chunk) // We assume a simplistic EOF delimiter '\n' for framing
                    
                    var newlineIdx = sb.indexOf("\n")
                    while (newlineIdx != -1) {
                        val messageStr = sb.substring(0, newlineIdx)
                        sb.delete(0, newlineIdx + 1)
                        if (messageStr.isNotBlank()) {
                            try {
                                val envelope = gson.fromJson(messageStr, TransportEnvelope::class.java)
                                _incomingMessages.tryEmit(envelope)
                            } catch (e: Exception) {
                                // Ignore malformed frames
                            }
                        }
                        newlineIdx = sb.indexOf("\n")
                    }
                }
            }
        } catch (e: Exception) {
            // Disconnected
        } finally {
            disconnect()
        }
    }

    override suspend fun send(message: TransportEnvelope) {
        withContext(Dispatchers.IO) {
            if (_connectionState.value != ConnectionState.CONNECTED) throw IllegalStateException("Not connected")
            try {
                val jsonStr = gson.toJson(message) + "\n"
                outputStream?.write(jsonStr.toByteArray(Charsets.UTF_8))
                outputStream?.flush()
            } catch (e: Exception) {
                disconnect()
                throw e
            }
        }
    }

    override fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            activeSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore closing exceptions
        }
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
