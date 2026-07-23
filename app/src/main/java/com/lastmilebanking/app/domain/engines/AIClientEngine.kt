package com.lastmilebanking.app.domain.engines

interface AIClientEngine {
    suspend fun checkFraudProbability(transactionDetails: Map<String, Any>): Result<Double>
    suspend fun getSavingsRecommendations(userId: String): Result<List<String>>
    suspend fun parseVoiceCommand(audioData: ByteArray): Result<String>
}
