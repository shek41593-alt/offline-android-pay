package com.lastmilebanking.app.domain.engines

interface SecurityEngine {
    fun encrypt(data: String): String
    fun decrypt(encryptedData: String): String
    fun hash(data: String): String
    fun sign(data: String): String
    fun verifySignature(data: String, signature: String): Boolean
    fun secureStore(key: String, value: String)
    fun secureRetrieve(key: String): String?
}
