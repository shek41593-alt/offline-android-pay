package com.lastmilebanking.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.local.entity.UserEntity
import com.lastmilebanking.app.data.repository.AuthenticationRepository
import com.lastmilebanking.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val _logoutEvent = MutableStateFlow(false)
    val logoutEvent: StateFlow<Boolean> = _logoutEvent.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                // Fetch from local DB
                val localUser = userRepository.getActiveUser().firstOrNull()
                
                if (localUser == null) {
                    _uiState.value = ProfileUiState.Error("Profile not found")
                } else {
                    _uiState.value = ProfileUiState.Success(
                        name = localUser.name ?: "Unknown",
                        email = "No Email",
                        phone = localUser.phoneNumber ?: "No Phone",
                        userId = localUser.userId ?: "Unknown ID",
                        publicPaymentId = localUser.publicPaymentId ?: "Not assigned"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutEvent.value = true
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val name: String, val email: String, val phone: String, val userId: String, val publicPaymentId: String) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
