package com.lastmilebanking.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyQrViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyQrUiState>(MyQrUiState.Loading)
    val uiState: StateFlow<MyQrUiState> = _uiState.asStateFlow()

    init {
        loadQrData()
    }

    fun loadQrData() {
        viewModelScope.launch {
            _uiState.value = MyQrUiState.Loading
            try {
                val localUser = userRepository.getActiveUser().firstOrNull()
                val paymentId = localUser?.publicPaymentId
                
                if (paymentId == null) {
                    _uiState.value = MyQrUiState.Error("Payment ID unavailable")
                } else {
                    val qrPayload = "LMBPAY:$paymentId"
                    _uiState.value = MyQrUiState.Success(
                        paymentId = paymentId,
                        qrContent = qrPayload
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MyQrUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class MyQrUiState {
    object Loading : MyQrUiState()
    data class Success(val paymentId: String, val qrContent: String) : MyQrUiState()
    data class Error(val message: String) : MyQrUiState()
}
