package com.framebynavin.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Deferrable safety net only. Exact user-facing delivery remains AlarmManager's job.
 */
class ReminderHealthWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        ReminderRecoveryEngine.reconcile(applicationContext)
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )
}

object ReminderHealthScheduler {
    private const val UNIQUE_WORK = "framebynavin-reminder-health-v132"

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderHealthWorker>(1, TimeUnit.HOURS)
            .addTag(UNIQUE_WORK)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
