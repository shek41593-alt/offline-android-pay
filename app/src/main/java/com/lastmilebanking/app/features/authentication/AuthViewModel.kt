package com.lastmilebanking.app.features.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastmilebanking.app.data.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
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
    
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    
    // Onboarding data
    private var password = ""
    private var name = ""
    private var dateOfBirth = ""
    private var addressLine = ""
    private var city = ""
    private var state = ""
    private var pinCode = ""

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState = _loginState.asStateFlow()
    
    var isLoginFlow = true

    fun setPhoneNumberAndEmail(phone: String, emailAddr: String) {
        _phoneNumber.value = phone
        _email.value = emailAddr
    }

    fun setPassword(pwd: String) { password = pwd }
    fun setPersonalInfo(fullName: String, dob: String) {
        name = fullName
        dateOfBirth = dob
    }
    fun setAddress(line: String, c: String, s: String, pin: String) {
        addressLine = line
        city = c
        state = s
        pinCode = pin
    }

    fun requestOtp() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val normalizedPhone = _phoneNumber.value.replace("+91", "")
                val userExists = authRepository.checkUser(normalizedPhone)
                
                if (isLoginFlow) {
                    if (!userExists) {
                        _loginState.value = LoginState.Error("Account not found. Please register.")
                        return@launch
                    }
                } else {
                    if (userExists) {
                        _loginState.value = LoginState.Error("Account already exists. Please sign in.")
                        return@launch
                    }
                }
                
                _loginState.value = LoginState.OtpSent
            } catch (e: retrofit2.HttpException) {
                if (e.code() in 500..599) {
                    _loginState.value = LoginState.Error("Server error. Please try again.")
                } else {
                    _loginState.value = LoginState.Error("Unable to connect to server.")
                }
            } catch (e: java.io.IOException) {
                _loginState.value = LoginState.Error("Unable to connect to server.")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Failed to send OTP")
            }
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                if (otp == "123456") {
                    _loginState.value = LoginState.RequiresRegistration
                } else {
                    _loginState.value = LoginState.Error("Invalid OTP. Please enter 123456.")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Invalid OTP. Please enter 123456.")
            }
        }
    }

    fun createAccountAndSync() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val parts = name.split(" ")
            val firstName = parts.firstOrNull() ?: ""
            val lastName = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""
            
            // Backend registration requires mobileNumber without +91 country code internally maybe?
            val normalizedPhone = _phoneNumber.value.replace("+91", "")

            try {
                if (!isLoginFlow) {
                    // Registration
                    when (val result = authRepository.register(
                        firstName, lastName, normalizedPhone, password, 
                        _email.value, dateOfBirth, addressLine, city, state, pinCode
                    )) {
                        is com.lastmilebanking.app.data.repository.RegistrationResult.Success -> {
                            // Proceed to login below
                        }
                        is com.lastmilebanking.app.data.repository.RegistrationResult.Conflict -> {
                            _loginState.value = LoginState.Error("Account already exists. Please sign in instead.")
                            return@launch
                        }
                        is com.lastmilebanking.app.data.repository.RegistrationResult.ValidationError -> {
                            _loginState.value = LoginState.Error("Invalid details: ${result.message}")
                            return@launch
                        }
                        is com.lastmilebanking.app.data.repository.RegistrationResult.ServerError -> {
                            _loginState.value = LoginState.Error("Server temporarily unavailable. Please try again.")
                            return@launch
                        }
                        is com.lastmilebanking.app.data.repository.RegistrationResult.NetworkError -> {
                            _loginState.value = LoginState.Error("Unable to connect to server.")
                            return@launch
                        }
                    }
                }
                
                // Login
                val loginSuccess = authRepository.login(normalizedPhone, password)
                if (loginSuccess) {
                    _loginState.value = LoginState.ExistingUserAuthenticated
                } else {
                    _loginState.value = LoginState.Error("Backend Authentication Failed")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Failed to synchronize profile")
            }
        }
    }

    fun loginOnly() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val normalizedPhone = _phoneNumber.value.replace("+91", "")
            try {
                val loginSuccess = authRepository.login(normalizedPhone, password)
                if (loginSuccess) {
                    _loginState.value = LoginState.ExistingUserAuthenticated
                } else {
                    _loginState.value = LoginState.Error("Incorrect password")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network failure or server unavailable.")
            }
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState = _authState.asStateFlow()

    fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.value = AuthState.Checking
            val isValid = authRepository.isValidSession()
            if (isValid) {
                _authState.value = AuthState.Authenticated
            } else {
                authRepository.logout()
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Initial
    }
}

sealed class AuthState {
    object Checking : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}

sealed class LoginState {
    object Initial : LoginState()
    object Loading : LoginState()
    object OtpSent : LoginState()
    object RequiresRegistration : LoginState() // Formerly VerifiedContinue & RequiresPassword
    object ExistingUserAuthenticated : LoginState() // Formerly Success
    data class Error(val message: String) : LoginState()
}
