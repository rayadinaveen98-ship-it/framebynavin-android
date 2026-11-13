package com.framebynavin.app.personal

import android.content.Context
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class Phase1HabitManagementOutcome(
    val records: List<Phase1HabitChallenge>,
    /** Non-null only if the owner mutation persisted but Android cleanup needs attention. */
    val deliveryWarning: String? = null,
)

/**
 * DataStore commits first, followed by best-effort cancellation and re-arming.
 * If Android cancellation fails, a stale receiver refuses deleted/archived IDs;
 * the owner still sees a warning instead of a false "all clear" success.
 */
internal class Phase1HabitManagementCoordinator(
    context: Context,
    private val store: Phase1HabitStore = Phase1HabitStore(context),
) {
    private val app = context.applicationContext

    suspend fun edit(
        before: Phase1HabitChallenge,
        activity: String,
        startDate: LocalDate,
        alertTime: LocalTime?,
        alertKind: Phase1HabitAlertKind?,
        completionMode: Phase1HabitCompletionMode,
    ): Phase1HabitManagementOutcome = withContext(Dispatchers.IO) {
        val records = store.editReviewed(
            before.id, before, activity, startDate, alertTime, alertKind, completionMode)
        val after = records.first { it.id == before.id }
        Phase1HabitManagementOutcome(records,
            cleanupAfterSave(before, shouldDismissPosted =
                before.activity != after.activity ||
                before.alertTime != after.alertTime ||
                before.alertKind != after.alertKind ||
                before.startDate != after.startDate))
    }

    suspend fun archive(
        before: Phase1HabitChallenge,
        archived: Boolean,
    ): Phase1HabitManagementOutcome = withContext(Dispatchers.IO) {
        val records = store.setArchivedReviewed(before.id, before, archived)
        Phase1HabitManagementOutcome(records,
            cleanupAfterSave(before, shouldDismissPosted = archived))
    }

    suspend fun delete(
        before: Phase1HabitChallenge,
    ): Phase1HabitManagementOutcome = withContext(Dispatchers.IO) {
        val records = store.deleteReviewed(before.id, before)
        Phase1HabitManagementOutcome(records,
            cleanupAfterSave(before, shouldDismissPosted = true,
                mustCancelPending = true))
    }

    private suspend fun cleanupAfterSave(
        before: Phase1HabitChallenge,
        shouldDismissPosted: Boolean,
        mustCancelPending: Boolean = false,
    ): String? {
        val failures = mutableListOf<String>()
        if (shouldDismissPosted) {
            runCatching { Phase1HabitVoiceService.stopChallenge(before.id) }
                .onFailure { failures.add("voice"); Log.w(TAG, "Voice stop failed", it) }
            runCatching { Phase1HabitNotification.cancelAllPosted(app, before) }
                .onFailure { failures.add("notification"); Log.w(TAG, "Posted cleanup failed", it) }
        }
        if (mustCancelPending) {
            runCatching { Phase1HabitAlarmScheduler(app).cancelChallengePending(before.id) }
                .onFailure { failures.add("alarm cancellation")
                    Log.w(TAG, "Deleted challenge alarm cancellation failed", it) }
        }
        runCatching { Phase1HabitAlarmScheduler(app).reconcile() }
            .onFailure { failures.add("alarm scheduling")
                Log.w(TAG, "Post-management challenge rearm failed", it) }
        return if (failures.isEmpty()) null
        else "Challenge change saved, but Android " +
            failures.distinct().joinToString(", ") +
            " needs attention. Use Alert Health to recheck; unrelated records are unchanged."
    }

    companion object {
        private const val TAG = "BacklotHabitManage"
    }
}
