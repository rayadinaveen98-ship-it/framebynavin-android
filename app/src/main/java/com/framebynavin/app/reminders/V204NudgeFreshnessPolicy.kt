package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorContextNudge
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus

/**
 * V204: discretionary engagement must relate to something that is still
 * actionable. Real scheduled reminders are handled by a separate system.
 */
internal object V204NudgeFreshnessPolicy {
    const val MAX_OVERDUE_AGE_MS = 7L * 24L * 60L * 60L * 1000L
    const val MAX_MISSED_CHECKIN_AGE_MS = 24L * 60L * 60L * 1000L

    fun relevant(nudge: CreatorContextNudge, task: CreatorTask?, nowMillis: Long): Boolean {
        if (task == null || task.id != nudge.taskId ||
            task.archivedAtMillis > 0L ||
            (task.status != TaskStatus.PLANNED && task.status != TaskStatus.WORKING)
        ) return false

        return when (nudge.key.substringBefore(':')) {
            "overdue" -> isInPastWindow(task.dueAtMillis, nowMillis, MAX_OVERDUE_AGE_MS)
            "missed-reminder" ->
                task.reminderEnabled &&
                    isInPastWindow(task.reminderAtMillis, nowMillis, MAX_MISSED_CHECKIN_AGE_MS)
            "risk-6h", "risk-24h", "critical-48h" ->
                task.dueAtMillis > nowMillis && nudge.dueAtMillis == task.dueAtMillis
            else -> false // No unsolicited notifications for unknown or merely READY states.
        }
    }

    private fun isInPastWindow(eventMillis: Long, nowMillis: Long, windowMillis: Long): Boolean =
        eventMillis > 0L && nowMillis > eventMillis &&
            nowMillis - eventMillis <= windowMillis
}
