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
 * Deferrable repair only. AlarmManager remains authoritative for the exact
 * reminder time. WorkManager must never claim to be a precise alarm clock.
 */
class PersonalSnoozeRearmWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        PersonalDeliveryCoordinator(applicationContext).reconcile()
        Result.success()
    } catch (error: Exception) {
        Log.w("BacklotPersonalSnooze", "Android schedule re-arm still needs retry", error)
        Result.retry()
    }
}

internal object PersonalSnoozeRearmSafetyNet {
    private const val NAME = "backlot-personal-snooze-rearm-v1"

    fun enqueue(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            NAME, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PersonalSnoozeRearmWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }
}
