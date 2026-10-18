package com.framebynavin.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStore
import java.util.concurrent.TimeUnit

/**
 * WorkManager provides deferrable (not exact-time) quiet check-ins.
 * Jobs never hold creator/project details and never issue a new occurrence token.
 */
internal object V179ReminderFollowUps {
    private const val PREFIX = "backlot-v179-no-response"
    private const val TASK_ID = "task"
    private const val TOKEN = "token"
    private const val ATTEMPT = "attempt"
    private const val SMART_TERMINAL = "smart_terminal"

    fun armForPostedNotification(context: Context, task: CreatorTask, token: String) {
        if (!enabled(context) || !V179FollowUpPolicy.eligible(task, finalSmartStage = false)) return
        enqueue(context, task, token, attempt = 1, terminalSmart = false, postedGrace = true)
    }

    /** Call only after the original voice/alarm occurrence was marked unanswered. */
    fun armAfterUnanswered(
        context: Context,
        task: CreatorTask,
        token: String,
        finalSmartStage: Boolean = false,
    ) {
        if (!enabled(context) || !V179FollowUpPolicy.eligible(task, finalSmartStage)) return
        enqueue(context, task, token, attempt = 1, terminalSmart = finalSmartStage, postedGrace = false)
    }

    private fun enqueue(
        context: Context,
        task: CreatorTask,
        token: String,
        attempt: Int,
        terminalSmart: Boolean,
        postedGrace: Boolean,
    ) {
        if (token.isBlank() || attempt !in 1..V179FollowUpPolicy.MAX_FOLLOW_UPS) return
        val delayMillis = V179FollowUpPolicy.delayMillis(task, attempt, postedGrace)
        val data = Data.Builder()
            .putString(TASK_ID, task.id)
            .putString(TOKEN, token)
            .putInt(ATTEMPT, attempt)
            .putBoolean(SMART_TERMINAL, terminalSmart)
            .build()
        val request = OneTimeWorkRequestBuilder<V179NoResponseWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(taskTag(task.id))
            .addTag(PREFIX)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(task.id, token, attempt),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(PREFIX)
    }

    private fun enabled(context: Context): Boolean =
        CreatorOsSettingsStore(context.applicationContext).snapshot().unansweredFollowUpsEnabled

    fun cancel(context: Context, taskId: String) {
        WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(taskTag(taskId))
    }

    private fun taskTag(taskId: String): String = "$PREFIX-$taskId"
    private fun workName(taskId: String, token: String, attempt: Int) =
        "$PREFIX-${taskId.hashCode()}-${token.hashCode()}-$attempt"

    /** Rechecks stored creator state and token at delivery time; never uses stale task snapshots. */
    class V179NoResponseWorker(
        appContext: Context,
        workerParams: WorkerParameters,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val taskId = inputData.getString(TASK_ID).orEmpty()
            val token = inputData.getString(TOKEN).orEmpty()
            val attempt = inputData.getInt(ATTEMPT, 0)
            val finalSmartStage = inputData.getBoolean(SMART_TERMINAL, false)
            if (taskId.isBlank() || token.isBlank() ||
                attempt !in 1..V179FollowUpPolicy.MAX_FOLLOW_UPS) return Result.success()
            if (!enabled(applicationContext)) return Result.success()

            // A failed worker may be rerun by WorkManager. No retry() is requested here:
            // "at most two" matters more than guaranteeing delivery under OS restrictions.
            return runCatching {
                CreatorDataGate.readyTransaction(applicationContext) {
                    // Race with the Settings switch: check again while inside the
                    // serialized creator-data gate before attempting any notification.
                    if (!enabled(applicationContext)) return@readyTransaction
                    val task = TaskStore(applicationContext).load()
                        .firstOrNull { it.id == taskId } ?: return@readyTransaction
                    val occurrences = ReminderOccurrenceStore(applicationContext)
                    if (!occurrences.matches(task, token)) return@readyTransaction

                    val acks = ReminderAcknowledgementStore(applicationContext)
                    // A regular posted alert becomes unanswered after its grace; never a read receipt.
                    acks.observeUnansweredNotification(task)
                    val record = acks.awaitingResponse(task)
                    if (!V179FollowUpPolicy.canDeliver(task, record, token, finalSmartStage)) {
                        return@readyTransaction
                    }

                    // WorkManager can rerun after a crash. Claim the attempt on disk before
                    // posting so it cannot create another alert for the same occurrence.
                    if (!ReminderNotifications.canPost(applicationContext, discreet = true)) return@readyTransaction
                    if (!V179FollowUpClaimStore(applicationContext).claim(task.id, token, attempt)) {
                        return@readyTransaction
                    }
                    val posted = ReminderNotifications.show(
                        context = applicationContext,
                        task = task,
                        stageLabel = "Unanswered · gentle follow-up $attempt of ${V179FollowUpPolicy.MAX_FOLLOW_UPS}",
                        occurrenceId = token,
                        discreet = true,
                        isFollowUp = true,
                    )
                    // No notification permission/channel? Leave unresolved; do not hammer Android
                    // with more attempts or escalate to voice/alarm.
                    if (posted) {
                        V179FollowUpPolicy.next(attempt)?.let { next ->
                            enqueue(applicationContext, task, token, next,
                                terminalSmart = finalSmartStage, postedGrace = false)
                        }
                    }
                }
                Result.success()
            }.onFailure {
                android.util.Log.w("BacklotFollowUp", "Skipped a failed quiet follow-up")
            }.getOrDefault(Result.success())
        }
    }
}
