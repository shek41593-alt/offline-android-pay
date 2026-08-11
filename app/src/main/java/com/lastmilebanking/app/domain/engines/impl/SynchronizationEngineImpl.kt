package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.domain.connectivity.ConnectivityObserver
import com.lastmilebanking.app.domain.engines.SynchronizationEngine
import com.lastmilebanking.app.domain.models.TransactionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SynchronizationEngineImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val connectivityObserver: ConnectivityObserver
) : SynchronizationEngine {

    private val syncMutex = Mutex()

    override suspend fun enqueueTransaction(transactionId: String) {
        // Generally WorkManager would be triggered here from the UI or Engine level.
    }

    override suspend fun retryFailedSyncs() {
        uploadPendingTransactions()
    }

    override suspend fun uploadPendingTransactions() {
        if (!connectivityObserver.isNetworkAvailable()) return

        syncMutex.withLock {
            val pending = transactionDao.getPendingTransactions()
            for (tx in pending) {
                if (tx.status == TransactionStatus.SYNCING.name) continue
                
                try {
                    // Mark as SYNCING
                    val syncingTx = tx.copy(status = TransactionStatus.SYNCING.name)
                    transactionDao.updateTransaction(syncingTx)

                    // Simulate network upload for Phase 13
                    // Retrofit usage will be replacing this in Phase 14
                    val isSuccess = simulateNetworkUpload(syncingTx.transactionId)
                    if (isSuccess) {
                        transactionDao.markAsSynced(syncingTx.transactionId)
                    } else {
                        // Transient failure -> revert to PENDING_SYNC for retry
                        transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.PENDING_SYNC.name))
                    }
                } catch (e: Exception) {
                    // Exception -> revert to PENDING_SYNC for retry
                    transactionDao.updateTransaction(tx.copy(status = TransactionStatus.PENDING_SYNC.name))
                }
            }
        }
    }

    override suspend fun resolveConflicts() {
        // Future Phase
    }

    override suspend fun settleLedger() {
        // Future Phase
    }

    private suspend fun simulateNetworkUpload(transactionId: String): Boolean {
        kotlinx.coroutines.delay(1000)
        return true // For Phase 13 testing
    }
}
