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

@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val synchronizationEngine: SynchronizationEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            synchronizationEngine.uploadPendingTransactions()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= 5) {
                // Retry exhaustion. Do not delete transaction, keep it recoverable.
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}
