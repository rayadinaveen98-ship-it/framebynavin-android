package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode

/**
 * V171 Project editor guardrails. An unrelated project edit must not silently reschedule
 * an overdue deadline, clear an active snooze or restart a managed reminder sequence.
 */
internal object V171ProjectEditSafety {
    fun initialDeadline(existingMillis: Long?, fallbackMillis: Long): Long =
        existingMillis?.takeIf { it > 0L } ?: fallbackMillis

    fun deadlineReady(selectedMillis: Long, nowMillis: Long, existingMillis: Long?): Boolean =
        selectedMillis > nowMillis ||
            (selectedMillis > 0L && existingMillis != null && selectedMillis == existingMillis)

    fun customReminderReady(
        plan: ProjectAttentionPlan,
        selectedMillis: Long,
        deadlineMillis: Long,
        nowMillis: Long,
        existingPlan: ProjectAttentionPlan?,
        existingReminderMillis: Long?,
        existingDeadlineMillis: Long?,
    ): Boolean {
        if (plan != ProjectAttentionPlan.CUSTOM) return true
        if (selectedMillis > nowMillis && selectedMillis <= deadlineMillis) return true
        // The creator may edit unrelated fields on an overdue project without
        // being forced to reset its existing (possibly elapsed) custom reminder.
        return existingPlan == ProjectAttentionPlan.CUSTOM &&
            selectedMillis > 0L &&
            selectedMillis == existingReminderMillis &&
            deadlineMillis == existingDeadlineMillis
    }

    /**
     * requested represents the form values before applying automatic Project Pulse rules.
     * For LIGHT/GUIDED/URGENT the submitted mode is intentionally NONE: the engine owns the
     * effective mode and schedule, so it is not a change when compared to the old runtime mode.
     */
    fun remindersNeedRefresh(current: CreatorTask, requested: CreatorTask, formatChanged: Boolean): Boolean =
        formatChanged ||
            current.dueAtMillis != requested.dueAtMillis ||
            current.attentionPlan != requested.attentionPlan ||
            current.deliveryPreference != requested.deliveryPreference ||
            current.priority != requested.priority ||
            current.alarmSoundUri != requested.alarmSoundUri ||
            current.voicePersona != requested.voicePersona ||
            current.voiceRepeatCount != requested.voiceRepeatCount ||
            current.voiceRepeatIntervalSeconds != requested.voiceRepeatIntervalSeconds ||
            current.alarmTimeoutSeconds != requested.alarmTimeoutSeconds ||
            (requested.attentionPlan == ProjectAttentionPlan.CUSTOM &&
                (current.reminderMode != requested.reminderMode ||
                    current.reminderAtMillis != if (requested.reminderMode == ReminderMode.NONE) 0L else requested.reminderAtMillis))
}
