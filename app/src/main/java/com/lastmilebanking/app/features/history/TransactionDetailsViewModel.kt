package com.lastmilebanking.app.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.dto.TransactionDetailResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TransactionDetailUiState {
    object Loading : TransactionDetailUiState()
    data class Success(val detail: TransactionDetailResponseDto, val isSent: Boolean) : TransactionDetailUiState()
    data class Error(val message: String) : TransactionDetailUiState()
}

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val apiService: LastMileApiService,
    private val userRepository: com.lastmilebanking.app.data.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = TransactionDetailUiState.Loading
                val response = apiService.getTransactionDetail(transactionId)
                if (response.isSuccessful && response.body() != null) {
                    val detail = response.body()!!
                    
                    // Determine if current user is sender
                    val myProfile = userRepository.getActiveUser().firstOrNull()
                    val myPublicId = myProfile?.publicPaymentId
                    
                    // The user is the sender if their publicId matches sender's publicId or if it was deduced another way.
                    // The backend could return the actual public ID of sender.
                    val isSent = detail.sender?.publicPaymentId == myPublicId
                    
                    _uiState.value = TransactionDetailUiState.Success(detail, isSent)
                } else {
                    _uiState.value = TransactionDetailUiState.Error("Transaction not found")
                }
            } catch (e: Exception) {
                _uiState.value = TransactionDetailUiState.Error("Unable to load transaction")
            }
        }
    }
}
