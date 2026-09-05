package com.lastmilebanking.app.features.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.lastmilebanking.app.data.repository.UserRepository
import com.lastmilebanking.app.data.repository.WalletRepository
import com.lastmilebanking.app.domain.payment.qr.AndroidKeystoreQrSigner
import com.lastmilebanking.app.domain.payment.qr.OfflineQrPayload
import com.lastmilebanking.app.domain.payment.qr.OfflineQrPaymentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import android.util.Base64

sealed class MerchantQrState {
    object Idle : MerchantQrState()
    object Generating : MerchantQrState()
    data class QrReady(val canonicalPayload: String, val amount: String, val merchantName: String) : MerchantQrState()
    data class TrustQrReady(val payload: String) : MerchantQrState()
    data class Error(val message: String) : MerchantQrState()
}

@HiltViewModel
class MerchantQrViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MerchantQrState>(MerchantQrState.Idle)
    val uiState: StateFlow<MerchantQrState> = _uiState

    // Signer operates securely generating canonical payloads internally via Android KeyStore ECDSA.
    private val qrSigner = AndroidKeystoreQrSigner()
    private val gson = Gson()
    
    // Secure random for nonce
    private val secureRandom = SecureRandom()

    fun resetState() {
        _uiState.value = MerchantQrState.Idle
    }

    fun generatePaymentRequest(amountStr: String) {
        viewModelScope.launch {
            _uiState.value = MerchantQrState.Generating

            try {
                val parsedAmount = BigDecimal(amountStr)
                if (parsedAmount <= BigDecimal.ZERO) {
                    _uiState.value = MerchantQrState.Error("Amount must be greater than zero")
                    return@launch
                }

                // 2. Fetch authenticated Merchant User & Wallet (No network dependent call)
                val user = userRepository.getActiveUser().firstOrNull()
                if (user == null) {
                    _uiState.value = MerchantQrState.Error("Missing merchant identity context")
                    return@launch
                }

                val wallet = walletRepository.getWalletByUserId(user.userId).firstOrNull()
                if (wallet == null) {
                    _uiState.value = MerchantQrState.Error("Missing merchant wallet context")
                    return@launch
                }
                
                val decimalAmount = parsedAmount.setScale(2, java.math.RoundingMode.HALF_UP).toString()

                val nonceBytes = ByteArray(16)
                secureRandom.nextBytes(nonceBytes)
                val nonceBase64 = Base64.encodeToString(nonceBytes, Base64.NO_WRAP)

                val request = OfflineQrPaymentRequest(
                    version = 1,
                    type = "LMB_PAYMENT_REQUEST",
                    merchantId = user.userId,
                    merchantWalletId = wallet.walletId,
                    amount = decimalAmount,
                    currency = "INR",
                    clientOperationId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    nonce = nonceBase64,
                    paymentMode = "OFFLINE_QR"
                )

                // The canonical form without signature determines the byte order layout
                val canonical = OfflineQrPayload.buildCanonicalPayload(request)
                
                // Then the signature is generated over the canonical bytes
                val signature = qrSigner.sign(canonical)
                
                // Then we wrap the entire package cleanly into a JSON for generating the actual QR payload.
                // Note: The signature guarantees authenticity over the canonical representation.
                val finalRequest = request.copy(signature = signature)
                val jsonPayload = gson.toJson(finalRequest)

                _uiState.value = MerchantQrState.QrReady(
                    canonicalPayload = jsonPayload,
                    amount = decimalAmount,
                    merchantName = user.name
                )
            } catch (e: Exception) {
                // Return user-readable message, not raw exceptions with logic pointers
                _uiState.value = MerchantQrState.Error("Unable to create secure payment request. Error parsing amount or executing security bindings.")
            }
        }
    }

    fun generateTrustRegistrationQr() {
        viewModelScope.launch {
            _uiState.value = MerchantQrState.Generating
            
            try {
                val user = userRepository.getActiveUser().firstOrNull()
                val wallet = user?.let { walletRepository.getWalletByUserId(it.userId).firstOrNull() }
                
                if (user == null || wallet == null) {
                    _uiState.value = MerchantQrState.Error("Missing merchant identity context")
                    return@launch
                }
                
                val publicKeyEncoded = qrSigner.getPublicKey().encoded
                val publicKeyBase64 = Base64.encodeToString(publicKeyEncoded, Base64.NO_WRAP)
                
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(publicKeyEncoded)
                val fingerprint = hashBytes.joinToString("") { "%02x".format(it) }
                
                val payload = com.lastmilebanking.app.domain.payment.qr.TrustRegistrationPayload(
                    version = 1,
                    type = "TRUST_REGISTRATION",
                    merchantId = user.userId,
                    merchantWalletId = wallet.walletId,
                    publicKeyBase64 = publicKeyBase64,
                    keyAlgorithm = "EC",
                    signatureAlgorithm = "SHA256withECDSA",
                    fingerprint = fingerprint,
                    createdAt = System.currentTimeMillis()
                )
                
                _uiState.value = MerchantQrState.TrustQrReady(gson.toJson(payload))
            } catch (e: Exception) {
                _uiState.value = MerchantQrState.Error("Unable to create Trust Registration QR.")
            }
        }
    }
}
