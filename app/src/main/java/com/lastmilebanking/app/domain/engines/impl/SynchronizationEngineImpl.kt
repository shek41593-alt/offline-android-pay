package com.lastmilebanking.app.domain.engines.impl

import com.lastmilebanking.app.data.local.dao.TransactionDao
import com.lastmilebanking.app.domain.connectivity.ConnectivityObserver
import com.lastmilebanking.app.domain.engines.SynchronizationEngine
import com.lastmilebanking.app.domain.models.TransactionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.lastmilebanking.app.data.network.api.LastMileApiService
import com.lastmilebanking.app.data.network.dto.SyncTransactionRequestDto
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Singleton
class SynchronizationEngineImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val connectivityObserver: ConnectivityObserver,
    private val apiService: LastMileApiService
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

                    val formatter = DateTimeFormatter.ISO_INSTANT
                    val timestampStr = formatter.format(Instant.ofEpochMilli(tx.createdAt))

                    val requestDto = SyncTransactionRequestDto(
                        transactionId = tx.transactionId,
                        senderId = tx.senderId,
                        receiverId = tx.receiverId, // Room uses receiverId
                        amount = BigDecimal.valueOf(tx.amount), // Preserve precision
                        currency = tx.currency,
                        paymentMode = tx.paymentMode, // QR, BLUETOOTH, SMS
                        timestamp = timestampStr,
                        signature = tx.encryptedPayload // Signature mapped to encryptedPayload or hash
                    )

                    val response = apiService.syncTransaction(requestDto)

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "RECEIVED" || body?.status == "DUPLICATE") {
                            transactionDao.markAsSynced(syncingTx.transactionId)
                        } else if (body?.status == "PROCESSING" || body?.status == "SETTLED") {
                            transactionDao.markAsSynced(syncingTx.transactionId)
                        } else {
                            // FAILED inside 200 OK
                            transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.FAILED.name))
                        }
                    } else {
                        when (response.code()) {
                            401, 403 -> {
                                // Auth error, do not retry automatically.
                                // We keep it PENDING_SYNC but we don't throw to avoid WorkManager looping immediately.
                                transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.PENDING_SYNC.name))
                            }
                            400 -> {
                                // Bad request, do not retry
                                transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.FAILED.name))
                            }
                            409 -> {
                                // Idempotency conflict. Treat as synced if true duplicate.
                                transactionDao.markAsSynced(syncingTx.transactionId)
                            }
                            else -> {
                                // 5xx or other transient
                                transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.PENDING_SYNC.name))
                                throw Exception("Transient server error ${response.code()}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    transactionDao.updateTransaction(tx.copy(status = TransactionStatus.PENDING_SYNC.name))
                    throw e // Bubble up to trigger WorkManager retry
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
        // Obsolete
        return true
    }
}
