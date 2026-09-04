package com.lastmilebanking.app.features.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.local.entity.WalletEntity
import com.lastmilebanking.app.data.repository.UserRepository
import com.lastmilebanking.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(val wallet: WalletEntity) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadWallet()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadWallet() {
        viewModelScope.launch {
            userRepository.getActiveUser()
                .filterNotNull()
                .flatMapLatest { user ->
                    try {
                        walletRepository.refreshWalletFromBackend(user.userId)
                    } catch (e: Exception) {}
                    walletRepository.getWalletByUserId(user.userId)
                }
                .filterNotNull()
                .catch { e ->
                    _uiState.value = WalletUiState.Error(e.message ?: "Unknown error")
                }
                .collect { wallet ->
                    _uiState.value = WalletUiState.Success(wallet)
                }
        }
    }
}
