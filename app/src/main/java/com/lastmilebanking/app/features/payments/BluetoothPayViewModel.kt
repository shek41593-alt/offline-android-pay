package com.lastmilebanking.app.features.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.domain.engines.AuthenticationEngine
import com.lastmilebanking.app.domain.engines.OfflinePaymentEngine
import com.lastmilebanking.app.domain.engines.SynchronizationEngine
import com.lastmilebanking.app.domain.engines.TransactionEngine
import com.lastmilebanking.app.domain.engines.ValidationEngine
import com.lastmilebanking.app.domain.engines.WalletEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BluetoothPayState {
    object Idle : BluetoothPayState()
    object Loading : BluetoothPayState()
    data class Verify(val receiverId: String, val amount: Double, val payload: String) : BluetoothPayState()
    data class Success(val transactionId: String) : BluetoothPayState()
    data class Error(val message: String) : BluetoothPayState()
}

@HiltViewModel
class BluetoothPayViewModel @Inject constructor(
    private val validationEngine: ValidationEngine,
    private val transactionEngine: TransactionEngine,
    private val walletEngine: WalletEngine,
    private val offlinePaymentEngine: OfflinePaymentEngine,
    private val synchronizationEngine: SynchronizationEngine,
    private val authenticationEngine: AuthenticationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<BluetoothPayState>(BluetoothPayState.Idle)
    val uiState: StateFlow<BluetoothPayState> = _uiState

    fun verifyPayment(receiverId: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = BluetoothPayState.Loading
            try {
                if (amount <= 0) {
                    _uiState.value = BluetoothPayState.Error("Invalid amount")
                    return@launch
                }

                val userId = "USER_01"
                val isWithinLimit = validationEngine.isWithinOfflineLimit(userId, amount)
                if (!isWithinLimit) {
                    _uiState.value = BluetoothPayState.Error("Amount exceeds offline limits")
                    return@launch
                }

                val hasBalance = validationEngine.hasSufficientBalance(userId, amount)
                if (!hasBalance) {
                    _uiState.value = BluetoothPayState.Error("Insufficient balance")
                    return@launch
                }
                
                val payload = offlinePaymentEngine.generateEncryptedPayload(userId, amount, System.currentTimeMillis())
                _uiState.value = BluetoothPayState.Verify(receiverId, amount, payload)
            } catch (e: Exception) {
                _uiState.value = BluetoothPayState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun completePayment(receiverId: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = BluetoothPayState.Loading
            try {
                val userId = "USER_01"
                val transactionResult = transactionEngine.createTransaction(
                    senderId = userId,
                    receiverId = receiverId,
                    amount = amount,
                    type = "SEND",
                    paymentMode = "BLUETOOTH"
                )

                if (transactionResult.isSuccess) {
                    val transactionId = transactionResult.getOrNull() ?: ""
                    walletEngine.debit(userId, amount)
                    synchronizationEngine.enqueueTransaction(transactionId)
                    _uiState.value = BluetoothPayState.Success(transactionId)
                } else {
                    _uiState.value = BluetoothPayState.Error("Failed to create transaction record")
                }
            } catch (e: Exception) {
                _uiState.value = BluetoothPayState.Error(e.message ?: "Failed to finalize payment")
            }
        }
    }

    fun abortPayment(errorMsg: String) {
        _uiState.value = BluetoothPayState.Error(errorMsg)
    }

    fun resetState() {
        _uiState.value = BluetoothPayState.Idle
    }
}
