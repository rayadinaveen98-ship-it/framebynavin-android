package com.framebynavin.app.personal

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Deferrable safety net if the broadcast receiver times out before it can
 * re-arm tomorrow's alarm. WorkManager is NOT a substitute for exact alarms
 * and cannot promise immediate user-facing delivery.
 */
class Phase1HabitRecoveryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        Phase1HabitAlarmScheduler(applicationContext).reconcile()
        Result.success()
    } catch (error: Exception) {
        Log.w("BacklotHabit", "One-shot challenge recovery will retry", error)
        Result.retry()
    }
}

internal object Phase1HabitRecoverySafetyNet {
    private const val WORK_NAME = "backlot-habit-rearm-after-failed-broadcast-v1"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<Phase1HabitRecoveryWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_NAME, ExistingWorkPolicy.KEEP, request,
        )
    }
}
