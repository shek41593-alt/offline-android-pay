package com.lastmilebanking.app.features.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.domain.engines.AuthenticationEngine
import com.lastmilebanking.app.domain.engines.OfflinePaymentEngine
import com.lastmilebanking.app.domain.engines.TransactionEngine
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
    data class Success(val transactionId: String, val payload: String) : SmsPayState()
    data class Error(val message: String) : SmsPayState()
}

@HiltViewModel
class SmsPayViewModel @Inject constructor(
    private val validationEngine: ValidationEngine,
    private val transactionEngine: TransactionEngine,
    private val walletEngine: WalletEngine,
    private val offlinePaymentEngine: OfflinePaymentEngine,
    private val authenticationEngine: AuthenticationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmsPayState>(SmsPayState.Idle)
    val uiState: StateFlow<SmsPayState> = _uiState

    fun processSmsPayment(recipientPhone: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = SmsPayState.Loading
            
            try {
                if (amount <= 0) {
                    _uiState.value = SmsPayState.Error("Invalid amount")
                    return@launch
                }

                // Get current user id
                // MVP assumption: user ID is present in auth engine or session
                // We'll use a mocked "USER_01" if session fetching is not fully set up
                val userId = "USER_01" 
                
                // 1. ValidationEngine
                val isWithinLimit = validationEngine.isWithinOfflineLimit(userId, amount)
                if (!isWithinLimit) {
                    _uiState.value = SmsPayState.Error("Amount exceeds offline limits")
                    return@launch
                }

                // We assume hasSufficientBalance handles checking the wallet properly
                // For MVP, we might mock a wallet or assume sufficient funds if not completely synced
                val hasBalance = validationEngine.hasSufficientBalance(userId, amount)
                if (!hasBalance) {
                    // For the sake of the hackathon offline demo, we might allow it if offline limit is respected
                    // but according to the phase 10 req, we should handle "insufficient balance" error state
                    _uiState.value = SmsPayState.Error("Insufficient balance")
                    return@launch
                }
                
                // 2. TransactionEngine
                val transactionResult = transactionEngine.createTransaction(
                    senderId = userId,
                    receiverId = recipientPhone,
                    amount = amount,
                    type = "SEND"
                )

                if (transactionResult.isSuccess) {
                    val transactionId = transactionResult.getOrNull() ?: ""
                    
                    // 3. WalletEngine (Debit sender)
                    walletEngine.debit(userId, amount)

                    // 4. Generate Payload via OfflinePaymentEngine
                    val payload = offlinePaymentEngine.generateEncryptedPayload(userId, amount, System.currentTimeMillis())

                    _uiState.value = SmsPayState.Success(transactionId, payload)
                } else {
                    _uiState.value = SmsPayState.Error("Failed to create transaction record")
                }

            } catch (e: Exception) {
                _uiState.value = SmsPayState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
