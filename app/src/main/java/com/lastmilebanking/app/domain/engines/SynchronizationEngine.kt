package com.lastmilebanking.app.domain.engines

interface SynchronizationEngine {
    suspend fun enqueueTransaction(transactionId: String)
    suspend fun retryFailedSyncs()
    suspend fun uploadPendingTransactions()
    suspend fun resolveConflicts()
    suspend fun settleLedger()
}
