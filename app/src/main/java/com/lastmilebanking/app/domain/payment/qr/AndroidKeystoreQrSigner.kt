package com.lastmilebanking.app.domain.payment.qr

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

class AndroidKeystoreQrSigner : OfflineQrSigner {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "LMB_QR_SIGNING_KEY"

    init {
        generateKeyIfNotExists()
    }

    private fun generateKeyIfNotExists() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false) 
                .build()

            keyPairGenerator.initialize(parameterSpec)
            keyPairGenerator.generateKeyPair()
        }
    }

    override fun sign(canonicalPayload: String): String {
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Signing key not found")
        
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        signature.update(canonicalPayload.toByteArray(Charsets.UTF_8))
        
        val signedBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signedBytes)
    }

    // For testing/mocking where we need to expose corresponding public key externally.
    fun getPublicKey(): PublicKey {
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Signing key not found")
        return entry.certificate.publicKey
    }
}

class AndroidKeystoreQrVerifier(
    // In actual production, public keys would be fetched from backend or local trusted registry
    // using the publicKeyIdentifier (merchantId). For tests/demo, inject it locally or simulate.
    private val publicKeyRegistry: (String) -> PublicKey?
) : OfflineQrVerifier {
    override fun verify(canonicalPayload: String, signature: String, publicKeyIdentifier: String): Boolean {
        try {
            val publicKey = publicKeyRegistry(publicKeyIdentifier) ?: return false
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(canonicalPayload.toByteArray(Charsets.UTF_8))
            val signatureBytes = Base64.getDecoder().decode(signature)
            return sig.verify(signatureBytes)
        } catch (e: Exception) {
            return false
        }
    }
}
