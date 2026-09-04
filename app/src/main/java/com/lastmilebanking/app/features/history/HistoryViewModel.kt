package com.lastmilebanking.app.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.repository.UserRepository
import com.lastmilebanking.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(val transactions: List<TransactionEntity>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            userRepository.getActiveUser()
                .filterNotNull()
                .flatMapLatest { user ->
                    try {
                        walletRepository.refreshWalletFromBackend(user.userId)
                    } catch (e: Exception) {
                    }
                    walletRepository.getWalletByUserId(user.userId)
                        .filterNotNull()
                        .flatMapLatest { wallet ->
                            walletRepository.getRecentTransactions(wallet.walletId, 100)
                        }
                }
                .catch { e ->
                    _uiState.value = HistoryUiState.Error(e.message ?: "Failed to load history")
                }
                .collect { transactions ->
                    _uiState.value = HistoryUiState.Success(transactions)
                }
        }
    }
}
