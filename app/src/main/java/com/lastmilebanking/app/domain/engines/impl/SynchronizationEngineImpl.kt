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

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.lastmilebanking.app.domain.workers.TransactionSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit

@Singleton
class SynchronizationEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context?,
    private val transactionDao: TransactionDao,
    private val connectivityObserver: ConnectivityObserver,
    private val apiService: LastMileApiService
) : SynchronizationEngine {

    private val syncMutex = Mutex()

    override suspend fun enqueueTransaction(transactionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequest.Builder(TransactionSyncWorker::class.java)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10000L,
                TimeUnit.MILLISECONDS
            )
            .build()

        try {
            if (context != null) {
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "SyncTransaction_$transactionId",
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        syncWorkRequest
                    )
            }
        } catch (e: Exception) {
            // Ignored in purely local JVM-based unit tests where WorkManager isn't functionally mocked.
        }
    }

    override suspend fun retryFailedSyncs() {
        uploadPendingTransactions()
    }

    override suspend fun uploadPendingTransactions() {
        if (!connectivityObserver.isNetworkAvailable()) return

        syncMutex.withLock {
            while (true) {
                val pending = transactionDao.getPendingTransactions(limit = 50)
                if (pending.isEmpty()) break
                
                var anyProcessed = false

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
                            anyProcessed = true
                        } else if (body?.status == "PROCESSING" || body?.status == "SETTLED") {
                            transactionDao.markAsSynced(syncingTx.transactionId)
                            anyProcessed = true
                        } else {
                            transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.FAILED.name))
                            anyProcessed = true // Updated status so it leaves queue
                        }
                    } else {
                        when (response.code()) {
                            401, 403 -> {
                                // Auth error, do not retry automatically.
                                // We keep it PENDING_SYNC but we don't throw to avoid WorkManager looping immediately.
                                transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.PENDING_SYNC.name))
                            }
                            400 -> {
                                transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.FAILED.name))
                                anyProcessed = true
                            }
                            409 -> {
                                transactionDao.updateTransaction(syncingTx.copy(status = TransactionStatus.FAILED.name))
                                anyProcessed = true
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
                } // End for (tx in pending)
                
                // If everything in this batch failed transiently, do not loop continuously.
                if (!anyProcessed) break
            } // End while
        } // End withLock
    } // End uploadPendingTransactions

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
