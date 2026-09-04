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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QrPayState {
    object Idle : QrPayState()
    object Loading : QrPayState()
    data class RecipientFound(val publicPaymentId: String, val name: String, val phone: String, val userId: String, val balance: java.math.BigDecimal) : QrPayState()
    data class PaymentAmount(val publicPaymentId: String, val name: String, val phone: String, val userId: String, val balance: java.math.BigDecimal) : QrPayState()
    data class PaymentReview(val publicPaymentId: String, val name: String, val phone: String, val userId: String, val amount: java.math.BigDecimal, val idempotencyKey: String) : QrPayState()
    data class Success(val transactionId: String, val amount: java.math.BigDecimal, val name: String) : QrPayState()
    data class Error(val message: String) : QrPayState()
}

@HiltViewModel
class QrPayViewModel @Inject constructor(
    private val userRepository: com.lastmilebanking.app.data.repository.UserRepository,
    private val walletRepository: com.lastmilebanking.app.data.repository.WalletRepository,
    private val apiService: com.lastmilebanking.app.data.network.api.LastMileApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrPayState>(QrPayState.Idle)
    val uiState: StateFlow<QrPayState> = _uiState

    fun onScanResult(qrData: String?) {
        if (qrData.isNullOrBlank()) {
            _uiState.value = QrPayState.Error("Invalid Last Mile Banking QR")
            return
        }

        try {
            if (qrData.startsWith("LMBPAY:")) {
                val publicPaymentId = qrData.removePrefix("LMBPAY:")
                if (publicPaymentId.isBlank()) {
                    _uiState.value = QrPayState.Error("Invalid Last Mile Banking QR")
                } else {
                    resolveRecipient(publicPaymentId)
                }
            } else {
                _uiState.value = QrPayState.Error("Invalid Last Mile Banking QR")
            }
        } catch (e: Exception) {
            _uiState.value = QrPayState.Error("Invalid Last Mile Banking QR")
        }
    }

    private fun resolveRecipient(publicPaymentId: String) {
        viewModelScope.launch {
            _uiState.value = QrPayState.Loading
            
            try {
                val user = userRepository.getActiveUser().firstOrNull()
                val currentUserId = user?.userId
                if (currentUserId == null) {
                    _uiState.value = QrPayState.Error("User session not found")
                    return@launch
                } 
                
                // Make API call. We need to inject LastMileApiService here or use it via repository.
                // Assuming we can simply use the injected api service here to quickly resolve. Wait! We didn't inject api service yet.
                // We will add it below. Let's just assume we inject `apiService`.
                val response = apiService.resolveRecipient(publicPaymentId)
                if (response.isSuccessful) {
                    val dto = response.body()
                    if (dto != null) {
                        if (dto.userId == currentUserId) {
                            _uiState.value = QrPayState.Error("You cannot pay yourself.")
                        } else {
                            val balanceResponse = apiService.getWalletBalance()
                            val balance = balanceResponse.body()?.balance ?: java.math.BigDecimal.ZERO
                            
                            _uiState.value = QrPayState.RecipientFound(
                                publicPaymentId = dto.publicPaymentId ?: "",
                                name = dto.name ?: "Unknown",
                                phone = dto.phone ?: "",
                                userId = dto.userId ?: "",
                                balance = balance
                            )
                        }
                    } else {
                        _uiState.value = QrPayState.Error("User not found")
                    }
                } else if (response.code() == 404) {
                    _uiState.value = QrPayState.Error("User not found")
                } else if (response.code() == 400 && response.errorBody()?.string()?.contains("pay yourself") == true) {
                    _uiState.value = QrPayState.Error("You cannot pay yourself.")
                } else {
                    _uiState.value = QrPayState.Error("User not found")
                }
            } catch (e: Exception) {
                _uiState.value = QrPayState.Error("Network error: User not found")
            }
        }
    }

    fun proceedToAmount() {
        val currentState = _uiState.value
        if (currentState is QrPayState.RecipientFound) {
            _uiState.value = QrPayState.PaymentAmount(
                publicPaymentId = currentState.publicPaymentId,
                name = currentState.name,
                phone = currentState.phone,
                userId = currentState.userId,
                balance = currentState.balance
            )
        }
    }

    fun submitAmount(amount: java.math.BigDecimal) {
        val currentState = _uiState.value
        if (currentState is QrPayState.PaymentAmount) {
            if (amount <= java.math.BigDecimal.ZERO) {
                _uiState.value = QrPayState.Error("Amount must be greater than zero")
                return
            }
            if (amount > currentState.balance) {
                _uiState.value = QrPayState.Error("Insufficient balance")
                return
            }
            val idempotencyKey = java.util.UUID.randomUUID().toString()
            _uiState.value = QrPayState.PaymentReview(
                publicPaymentId = currentState.publicPaymentId,
                name = currentState.name,
                phone = currentState.phone,
                userId = currentState.userId,
                amount = amount,
                idempotencyKey = idempotencyKey
            )
        }
    }

    fun confirmPayment(idempotencyKey: String) {
        val currentState = _uiState.value
        if (currentState is QrPayState.PaymentReview && currentState.idempotencyKey == idempotencyKey) {
            viewModelScope.launch {
                _uiState.value = QrPayState.Loading
                try {
                    val request = com.lastmilebanking.app.data.network.dto.DirectPaymentRequestDto(
                        recipientPaymentId = currentState.publicPaymentId,
                        amount = currentState.amount,
                        idempotencyKey = currentState.idempotencyKey
                    )
                    val response = apiService.processPayment(request)
                    if (response.isSuccessful) {
                        val dto = response.body()
                        try {
                            walletRepository.refreshWalletFromBackend(currentState.userId)
                        } catch (e: Exception) {
                            // Ignored
                        }
                        _uiState.value = QrPayState.Success(
                            transactionId = dto?.transactionId ?: currentState.idempotencyKey,
                            amount = currentState.amount,
                            name = currentState.name
                        )
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        if (response.code() == 409) {
                            _uiState.value = QrPayState.Error("Transaction is in progress or already processed.")
                        } else if (response.code() == 400 && errorBody.contains("INSUFFICIENT_BALANCE")) {
                            _uiState.value = QrPayState.Error("Insufficient balance")
                        } else {
                            _uiState.value = QrPayState.Error("Payment failed. Please try again.")
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = QrPayState.Error("Network error: Payment failed")
                }
            }
        }
    }
    
    fun resetState() {
        _uiState.value = QrPayState.Idle
    }
}
