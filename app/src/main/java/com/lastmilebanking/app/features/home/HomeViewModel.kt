package com.lastmilebanking.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.local.entity.UserEntity
import com.lastmilebanking.app.data.local.entity.WalletEntity
import com.lastmilebanking.app.data.repository.UserRepository
import com.lastmilebanking.app.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isBalanceVisible = MutableStateFlow(true)
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible.asStateFlow()

    init {
        loadDashboard()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDashboard() {
        viewModelScope.launch {
            // Ensure demo user exists
            userRepository.seedDemoUserIfNeeded()

            userRepository.getActiveUser()
                .filterNotNull()
                .flatMapLatest { user ->
                    walletRepository.getWalletByUserId(user.userId)
                        .filterNotNull()
                        .flatMapLatest { wallet ->
                            combine(
                                walletRepository.getRecentTransactions(wallet.walletId, 5),
                                walletRepository.getPendingTransactionCount(wallet.walletId)
                            ) { transactions, pendingCount ->
                                HomeUiState.Success(
                                    userName = user.name,
                                    wallet = wallet,
                                    recentTransactions = transactions,
                                    pendingSyncCount = pendingCount,
                                    isOffline = false
                                )
                            }
                        }
                }
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun toggleBalanceVisibility() {
        _isBalanceVisible.value = !_isBalanceVisible.value
    }

    fun refresh() {
        _uiState.value = HomeUiState.Loading
        loadDashboard()
    }
}
