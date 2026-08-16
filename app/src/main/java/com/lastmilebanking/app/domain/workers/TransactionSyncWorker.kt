package com.lastmilebanking.app.domain.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lastmilebanking.app.domain.engines.SynchronizationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val synchronizationEngine: SynchronizationEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i("TransactionSync", "SYNC_STARTED retryAttempt=$runAttemptCount timestamp=${System.currentTimeMillis()}")
            synchronizationEngine.uploadPendingTransactions()
            Log.i("TransactionSync", "SYNC_SUCCESS retryAttempt=$runAttemptCount")
            Result.success()
        } catch (e: Exception) {
            val status = if (e.message?.contains("Transient server error") == true) "5xx" else "NETWORK_ERROR"
            if (runAttemptCount >= 5) {
                // Retry exhaustion. Do not delete transaction, keep it recoverable.
                Log.e("TransactionSync", "SYNC_FAILED status=$status retryAttempt=$runAttemptCount category=EXHAUSTION")
                Result.failure()
            } else {
                Log.w("TransactionSync", "SYNC_FAILED status=$status retryAttempt=$runAttemptCount category=RETRYING")
                Result.retry()
            }
        }
    }
}
