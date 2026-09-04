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

    fun ensurePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            NOW_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
