package com.framebynavin.app.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val manager = CloudSyncManager(applicationContext)
        return when (val result = manager.syncNow(force = false)) {
            is CloudOperationResult.Success -> Result.success()
            is CloudOperationResult.Skipped -> Result.success()
            is CloudOperationResult.Failure -> if (result.retryable && runAttemptCount < 3) Result.retry() else Result.success()
        }
    }
}

object CloudSyncScheduler {
    private const val PERIODIC_NAME = "framebynavin-cloud-periodic-v13"
    private const val NOW_NAME = "framebynavin-cloud-now-v13"

    fun cancelAll(context: Context) {
        val work = WorkManager.getInstance(context.applicationContext)
        work.cancelUniqueWork(PERIODIC_NAME)
        work.cancelUniqueWork(NOW_NAME)
    }

    fun ensurePeriodic(context: Context) {
        cancelAll(context)
    }

    fun enqueueNow(context: Context) {
        // No automatic uploads. Existing background workers will return Skipped.
        cancelAll(context)
    }
}
