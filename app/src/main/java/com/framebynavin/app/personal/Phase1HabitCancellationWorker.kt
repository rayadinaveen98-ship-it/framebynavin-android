package com.framebynavin.app.personal

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Retry Android cleanup after an already-committed archive or delete.
 * Never deletes owner data. Before canceling an OS alarm, re-check whether
 * that ID is active again (a later Restore/import must not lose its alarms).
 */
class Phase1HabitCancellationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("challengeId") ?: return Result.failure()
        val first = inputData.getString("startDate") ?: return Result.failure()
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,100}"))) return Result.failure()
        val start = runCatching { LocalDate.parse(first) }.getOrNull()
            ?: return Result.failure()
        return try {
            val records = Phase1HabitStore(applicationContext).load()
            if (records.any { it.id == id && !it.archived }) {
                // A user restored this challenge; preserve its new schedule.
                return Result.success()
            }
            Phase1HabitAlarmScheduler(applicationContext).cancelChallengePending(id)
            val notifications = applicationContext
                .getSystemService(NotificationManager::class.java)
            (0L..20L).forEach { index ->
                notifications.cancel("backlot:habit:" + id + ":" + start.plusDays(index),
                    id.hashCode())
            }
            Phase1HabitVoiceService.stopChallenge(id)
            Result.success()
        } catch (error: Exception) {
            Log.w("BacklotHabit", "Archived/deleted alarm cancellation retry needed", error)
            Result.retry()
        }
    }
}

internal object Phase1HabitCancellationSafetyNet {
    fun enqueue(context: Context, challenge: Phase1HabitChallenge) {
        val request = OneTimeWorkRequestBuilder<Phase1HabitCancellationWorker>()
            .setInputData(Data.Builder()
                .putString("challengeId", challenge.id)
                .putString("startDate", challenge.startDate.toString())
                .build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "backlot-habit-cancel-" + challenge.id,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
