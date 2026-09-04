package com.lastmilebanking.app.features.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.dto.FundWalletRequestDto
import com.lastmilebanking.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

sealed class AddMoneyUiState {
    object Input : AddMoneyUiState()
    data class Review(val amount: BigDecimal) : AddMoneyUiState()
    object Loading : AddMoneyUiState()
    data class Success(val amount: BigDecimal, val newBalance: BigDecimal) : AddMoneyUiState()
    data class Error(val message: String) : AddMoneyUiState()
}

@HiltViewModel
class AddMoneyViewModel @Inject constructor(
    private val apiService: LastMileApiService,
    private val walletRepository: WalletRepository,
    private val userRepository: com.lastmilebanking.app.data.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddMoneyUiState>(AddMoneyUiState.Input)
    val uiState: StateFlow<AddMoneyUiState> = _uiState.asStateFlow()

    private var currentAmount: BigDecimal = BigDecimal.ZERO
    private var idempotencyKey = UUID.randomUUID().toString()

    fun submitAmount(amountStr: String) {
        val amount = amountStr.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _uiState.value = AddMoneyUiState.Error("Invalid amount")
            return
        }
        currentAmount = amount
        _uiState.value = AddMoneyUiState.Review(amount)
    }

    fun confirmFunding() {
        viewModelScope.launch {
            _uiState.value = AddMoneyUiState.Loading
            try {
                val user = userRepository.getActiveUser().firstOrNull()
                val userId = user?.userId ?: return@launch
                
                val response = apiService.fundWallet(
                    idempotencyKey = idempotencyKey,
                    request = FundWalletRequestDto(currentAmount)
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    android.util.Log.d("FUND_DEBUG", "Success! Body: $body")
                    
                    val newBalance = body.walletBalance ?: BigDecimal.ZERO
                    
                    // Trigger wallet refresh
                    try {
                        walletRepository.refreshWalletFromBackend(userId)
                    } catch (e: Exception) {}

                    _uiState.value = AddMoneyUiState.Success(currentAmount, newBalance)
                    idempotencyKey = UUID.randomUUID().toString() // Generate new key for next transaction
                } else {
                    idempotencyKey = UUID.randomUUID().toString()
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("FUND_DEBUG", "Failed natively: HTTP ${response.code()}, errorBody: $errorBody")
                    _uiState.value = AddMoneyUiState.Error("Funding failed (HTTP ${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("FUND_DEBUG", "Exception thrown: ${e.message}", e)
                _uiState.value = AddMoneyUiState.Error("Funding failed natively: ${e.message}")
            }
        }
    }

    fun resetState() {
        if (_uiState.value is AddMoneyUiState.Error) {
            _uiState.value = AddMoneyUiState.Input
        }
    }
}
