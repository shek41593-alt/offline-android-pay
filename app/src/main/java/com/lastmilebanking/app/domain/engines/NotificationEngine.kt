package com.lastmilebanking.app.domain.engines

interface NotificationEngine {
    suspend fun sendPaymentAlert(transactionId: String, amount: Double, receiverName: String)
    suspend fun sendFraudAlert(transactionId: String, reason: String)
    suspend fun sendSyncNotification(status: String)
}
