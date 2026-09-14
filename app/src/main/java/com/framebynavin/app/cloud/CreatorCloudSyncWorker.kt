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

class CreatorCloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = when (val result = CreatorCloudSyncManager(applicationContext).syncNow()) {
        is CreatorCloudSyncResult.Failure -> if (result.retryable) Result.retry() else Result.success()
        else -> Result.success()
    }

    companion object {
        private const val PERIODIC_WORK = "creator-cloud-sync-periodic-v123"
        private const val SOON_WORK = "creator-cloud-sync-soon-v123"

        private fun constraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun ensurePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CreatorCloudSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun enqueueSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<CreatorCloudSyncWorker>()
                .setConstraints(constraints())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                SOON_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
