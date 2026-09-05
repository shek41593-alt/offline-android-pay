package com.lastmilebanking.app.domain.payment.qr

interface OfflineQrSigner {
    fun sign(canonicalPayload: String): String
}

interface OfflineQrVerifier {
    fun verify(canonicalPayload: String, signature: String, publicKeyIdentifier: String): Boolean
}
