package com.lastmilebanking.app.features.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.domain.engines.AuthenticationEngine
import com.lastmilebanking.app.domain.engines.OfflinePaymentEngine
import com.lastmilebanking.app.domain.engines.TransactionEngine
import com.lastmilebanking.app.domain.engines.SynchronizationEngine
import com.lastmilebanking.app.domain.engines.ValidationEngine
import com.lastmilebanking.app.domain.engines.WalletEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SmsPayState {
    object Idle : SmsPayState()
    object Loading : SmsPayState()
    data class Verify(val recipientPhone: String, val amount: Double) : SmsPayState()
    data class ReadyToSend(val recipientPhone: String, val amount: Double, val payload: String) : SmsPayState()
    data class Success(val transactionId: String, val payload: String) : SmsPayState()
    data class Error(val message: String) : SmsPayState()
}

@HiltViewModel
class SmsPayViewModel @Inject constructor(
    private val validationEngine: ValidationEngine,
    private val transactionEngine: TransactionEngine,
    private val walletEngine: WalletEngine,
    private val offlinePaymentEngine: OfflinePaymentEngine,
    private val synchronizationEngine: SynchronizationEngine,
    private val authenticationEngine: AuthenticationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmsPayState>(SmsPayState.Idle)
    val uiState: StateFlow<SmsPayState> = _uiState

    fun verifySmsPayment(recipientPhone: String, amount: Double) {
        if (amount <= 0) {
            _uiState.value = SmsPayState.Error("Invalid amount")
            return
        }
        _uiState.value = SmsPayState.Verify(recipientPhone, amount)
    }

    fun prepareSmsPayment(recipientPhone: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = SmsPayState.Loading
            
            try {
                val userId = "USER_01" 
                
                val isWithinLimit = validationEngine.isWithinOfflineLimit(userId, amount)
                if (!isWithinLimit) {
                    _uiState.value = SmsPayState.Error("Amount exceeds offline limits")
                    return@launch
                }

                val hasBalance = validationEngine.hasSufficientBalance(userId, amount)
                if (!hasBalance) {
                    _uiState.value = SmsPayState.Error("Insufficient balance")
                    return@launch
                }
                
                val payload = offlinePaymentEngine.generateEncryptedPayload(userId, amount, System.currentTimeMillis())
                _uiState.value = SmsPayState.ReadyToSend(recipientPhone, amount, payload)
            } catch (e: Exception) {
                _uiState.value = SmsPayState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun completeSmsPayment(recipientPhone: String, amount: Double, payload: String) {
        viewModelScope.launch {
            _uiState.value = SmsPayState.Loading
            try {
                val userId = "USER_01"

                val transactionResult = transactionEngine.createTransaction(
                    senderId = userId,
                    receiverId = recipientPhone,
                    amount = amount,
                    type = "SEND",
                    paymentMode = "SMS"
                )

                if (transactionResult.isSuccess) {
                    val transactionId = transactionResult.getOrNull() ?: ""
                    
                    walletEngine.debit(userId, amount)
                    synchronizationEngine.enqueueTransaction(transactionId)

                    _uiState.value = SmsPayState.Success(transactionId, payload)
                } else {
                    _uiState.value = SmsPayState.Error("Failed to create transaction record")
                }
            } catch (e: Exception) {
                _uiState.value = SmsPayState.Error(e.message ?: "Failed to finalize payment")
            }
        }
    }

    fun abortSmsPayment(errorMsg: String) {
        _uiState.value = SmsPayState.Error(errorMsg)
    }
    
    fun resetState() {
        _uiState.value = SmsPayState.Idle
    }
}
