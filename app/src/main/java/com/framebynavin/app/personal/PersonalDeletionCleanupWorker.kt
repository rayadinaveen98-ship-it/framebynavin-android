package com.framebynavin.app.personal

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.framebynavin.app.widget.V224WidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Post-commit Android cleanup, never an owner-data delete.
 * The DataStore tombstones survive a crash after a bulk delete; WorkManager
 * and the existing periodic reminder health lane can both retry this safely.
 */
internal class PersonalDeletionCleanup(context: Context) {
    private val app = context.applicationContext
    private val store = PersonalReminderStore(app)

    suspend fun reconcile(): Boolean {
        val cleaned = mutableSetOf<String>()
        var failed = false
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            val pending = store.pendingDeletionCleanupIds()
            if (pending.isEmpty()) return@withStableSnapshot
            val existingIds = store.load().mapTo(HashSet()) { it.id }
            val delivery = PersonalDeliveryCoordinator(app)
            pending.forEach { id ->
                if (id in existingIds) {
                    // A future explicit restore/import may reuse an ID: never
                    // cancel the new live record because of an old tombstone.
                    cleaned.add(id)
                } else {
                    try {
                        delivery.cancelPending(id)
                        PersonalPlaybackService.stopForReminder(id)
                        PersonalReminderNotifications.cancel(app, id)
                        cleaned.add(id)
                    } catch (error: Exception) {
                        failed = true
                        Log.w(TAG, "Personal reminder alert cleanup needs retry for ID", error)
                    }
                }
            }
        }
        if (cleaned.isNotEmpty()) {
            store.markDeletionCleanupFinished(cleaned)
            runCatching { V224WidgetUpdater.refresh(app) }
                .onFailure { Log.w(TAG, "Widgets need another refresh", it) }
        }
        return !failed
    }

    companion object { private const val TAG = "BacklotPersonalDelete" }
}

class PersonalDeletionCleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        if (PersonalDeletionCleanup(applicationContext).reconcile()) Result.success()
        else Result.retry()
    } catch (error: Exception) {
        Log.w("BacklotPersonalDelete", "Personal delete cleanup will retry", error)
        Result.retry()
    }
}

internal object PersonalDeletionCleanupScheduler {
    private const val WORK_NAME = "backlot-personal-deletion-cleanup-v1"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<PersonalDeletionCleanupWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_NAME, ExistingWorkPolicy.KEEP, request,
        )
    }
}
