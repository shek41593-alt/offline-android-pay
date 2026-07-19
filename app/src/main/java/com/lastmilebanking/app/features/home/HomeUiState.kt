package com.lastmilebanking.app.features.home

import com.lastmilebanking.app.data.local.entity.TransactionEntity
import com.lastmilebanking.app.data.local.entity.WalletEntity

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val userName: String,
        val wallet: WalletEntity,
        val recentTransactions: List<TransactionEntity>,
        val pendingSyncCount: Int,
        val isOffline: Boolean = false
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
