package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus

/**
 * Bounded and non-intrusive. These intervals are MINIMUM WorkManager delays, not exact alarms.
 * Two follow-ups maximum. Never escalate an unanswered notification into a new loud alert.
 */
internal object V179FollowUpPolicy {
    const val MAX_FOLLOW_UPS = 2
    const val POSTED_GRACE_MILLIS = 20L * 60_000L

    fun eligible(task: CreatorTask, finalSmartStage: Boolean): Boolean =
        task.reminderEnabled &&
            task.reminderMode != ReminderMode.NONE &&
            task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED &&
            task.archivedAtMillis == 0L &&
            task.workingUntilMillis <= System.currentTimeMillis() &&
            ((task.reminderMode != ReminderMode.SMART && !task.smartEscalationEnabled) || finalSmartStage)

    fun delayMillis(task: CreatorTask, attempt: Int, awaitingPostedNotification: Boolean = false): Long {
        require(attempt in 1..MAX_FOLLOW_UPS)
        if (attempt == 1 && awaitingPostedNotification) return POSTED_GRACE_MILLIS
        val minutes = when (attempt) {
            1 -> when (task.priority) {
                TaskPriority.NORMAL -> 20L
                TaskPriority.IMPORTANT -> 10L
                TaskPriority.CRITICAL -> 5L
            }
            else -> when (task.priority) {
                TaskPriority.NORMAL -> 30L
                TaskPriority.IMPORTANT -> 25L
                TaskPriority.CRITICAL -> 15L
            }
        }
        return minutes * 60_000L
    }

    fun next(attempt: Int): Int? = if (attempt < MAX_FOLLOW_UPS) attempt + 1 else null

    fun canDeliver(
        task: CreatorTask,
        record: ReminderAckRecord?,
        token: String,
        finalSmartStage: Boolean,
    ): Boolean = eligible(task, finalSmartStage) &&
        record != null &&
        record.occurrenceId == token &&
        record.state == ReminderAckState.UNANSWERED
}
