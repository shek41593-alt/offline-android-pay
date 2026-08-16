package com.lastmilebanking.app.features.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber = _phoneNumber.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState = _loginState.asStateFlow()

    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }

    fun loginWithOtp(otp: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val success = authRepository.login(_phoneNumber.value, otp)
            if (success) {
                _loginState.value = LoginState.Success
            } else {
                _loginState.value = LoginState.Error("Login failed")
            }
        }
    }
    
    fun resetState() {
        _loginState.value = LoginState.Initial
    }
}

sealed class LoginState {
    object Initial : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
