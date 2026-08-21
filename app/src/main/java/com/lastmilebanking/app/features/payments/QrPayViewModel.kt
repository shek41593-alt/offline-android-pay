package com.lastmilebanking.app.features.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.domain.engines.AuthenticationEngine
import com.lastmilebanking.app.domain.engines.SynchronizationEngine
import com.lastmilebanking.app.domain.engines.TransactionEngine
import com.lastmilebanking.app.domain.engines.ValidationEngine
import com.lastmilebanking.app.domain.engines.WalletEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QrPayState {
    object Idle : QrPayState()
    object Loading : QrPayState()
    data class Verify(val receiverId: String, val amount: Double) : QrPayState()
    data class Success(val transactionId: String) : QrPayState()
    data class Error(val message: String) : QrPayState()
}

@HiltViewModel
class QrPayViewModel @Inject constructor(
    private val validationEngine: ValidationEngine,
    private val transactionEngine: TransactionEngine,
    private val walletEngine: WalletEngine,
    private val synchronizationEngine: SynchronizationEngine,
    private val authenticationEngine: AuthenticationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrPayState>(QrPayState.Idle)
    val uiState: StateFlow<QrPayState> = _uiState

    fun onScanResult(qrData: String?) {
        if (qrData.isNullOrBlank()) {
            _uiState.value = QrPayState.Error("Invalid QR code")
            return
        }

        try {
            // expected format: LMB:OFFLINE_TXN:UID1234:SECURE_HASH:500.00
            val parts = qrData.split(":")
            if (parts.size >= 5 && parts[0] == "LMB") {
                val receiverId = parts[2]
                val amount = parts[4].toDoubleOrNull() ?: 0.0
                
                if (amount <= 0) {
                    _uiState.value = QrPayState.Error("Invalid amount in QR")
                } else {
                    _uiState.value = QrPayState.Verify(receiverId, amount)
                }
            } else {
                _uiState.value = QrPayState.Error("Unrecognized QR format")
            }
        } catch (e: Exception) {
            _uiState.value = QrPayState.Error("Failed to parse QR code")
        }
    }

    fun processQrPayment(receiverId: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = QrPayState.Loading
            
            try {
                if (amount <= 0) {
                    _uiState.value = QrPayState.Error("Invalid amount")
                    return@launch
                }

                // Get current user id
                // MVP assumption: user ID is present in auth engine or session
                // We'll use a mocked "USER_01" if session fetching is not fully set up
                val userId = "USER_01" 
                
                // 1. ValidationEngine
                val isWithinLimit = validationEngine.isWithinOfflineLimit(userId, amount)
                if (!isWithinLimit) {
                    _uiState.value = QrPayState.Error("Amount exceeds offline limits")
                    return@launch
                }

                val hasBalance = validationEngine.hasSufficientBalance(userId, amount)
                if (!hasBalance) {
                    _uiState.value = QrPayState.Error("Insufficient balance")
                    return@launch
                }
                
                // 2. TransactionEngine
                val transactionResult = transactionEngine.createTransaction(
                    senderId = userId,
                    receiverId = receiverId,
                    amount = amount,
                    type = "SEND",
                    paymentMode = "QR"
                )

                if (transactionResult.isSuccess) {
                    val transactionId = transactionResult.getOrNull() ?: ""
                    
                    // 3. WalletEngine (Debit sender)
                    walletEngine.debit(userId, amount)

                    // 4. Enqueue synchronization
                    synchronizationEngine.enqueueTransaction(transactionId)

                    _uiState.value = QrPayState.Success(transactionId)
                } else {
                    _uiState.value = QrPayState.Error("Failed to create transaction record")
                }

            } catch (e: Exception) {
                _uiState.value = QrPayState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = QrPayState.Idle
    }
}
