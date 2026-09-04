package com.framebynavin.app.reminders

import com.framebynavin.app.data.TaskPriority

/** Pure Smart timing/sequence rules. Reminder time is the final Smart target. */
data class SmartEscalationConfig(
    val notificationToVoiceMinutes: Int = 15,
    val voiceToAlarmMinutes: Int = 15,
    val alarmToCriticalMinutes: Int = 15,
) {
    fun normalized(): SmartEscalationConfig = copy(
        notificationToVoiceMinutes = notificationToVoiceMinutes.coerceIn(5, 30),
        voiceToAlarmMinutes = voiceToAlarmMinutes.coerceIn(5, 30),
        alarmToCriticalMinutes = alarmToCriticalMinutes.coerceIn(5, 30),
    )
}

object SmartEscalationPolicy {
    val allowedGapMinutes = listOf(5, 10, 15, 20, 30)

    fun requiredWindowMinutes(priority: TaskPriority, config: SmartEscalationConfig): Int {
        val c = config.normalized()
        return when (priority) {
            TaskPriority.NORMAL -> 0
            TaskPriority.IMPORTANT -> c.notificationToVoiceMinutes + c.voiceToAlarmMinutes
            TaskPriority.CRITICAL -> c.notificationToVoiceMinutes + c.voiceToAlarmMinutes + c.alarmToCriticalMinutes
        }
    }

    /** Minutes available from now until the creator-selected final reminder target. */
    fun availableWindowMinutes(nowMillis: Long, reminderAtMillis: Long): Int {
        if (nowMillis <= 0L || reminderAtMillis <= nowMillis) return 0
        return ((reminderAtMillis - nowMillis) / 60_000L).toInt().coerceAtLeast(0)
    }

    fun isWindowValid(
        priority: TaskPriority,
        nowMillis: Long,
        reminderAtMillis: Long,
        config: SmartEscalationConfig,
    ): Boolean {
        if (reminderAtMillis <= nowMillis) return false
        if (priority == TaskPriority.NORMAL) return true
        return availableWindowMinutes(nowMillis, reminderAtMillis) >= requiredWindowMinutes(priority, config)
    }

    /** Planned first Smart stage so the strongest stage lands at reminderAtMillis. */
    fun firstStageAtMillis(
        priority: TaskPriority,
        reminderAtMillis: Long,
        config: SmartEscalationConfig,
    ): Long = reminderAtMillis - requiredWindowMinutes(priority, config) * 60_000L

    fun nextStage(priority: TaskPriority, current: SmartEscalationScheduler.Stage): SmartEscalationScheduler.Stage? = when (priority) {
        TaskPriority.NORMAL -> null
        TaskPriority.IMPORTANT -> when (current) {
            SmartEscalationScheduler.Stage.SOFT -> SmartEscalationScheduler.Stage.VOICE
            SmartEscalationScheduler.Stage.VOICE -> SmartEscalationScheduler.Stage.ALARM
            else -> null
        }
        TaskPriority.CRITICAL -> when (current) {
            SmartEscalationScheduler.Stage.SOFT -> SmartEscalationScheduler.Stage.VOICE
            SmartEscalationScheduler.Stage.VOICE -> SmartEscalationScheduler.Stage.ALARM
            SmartEscalationScheduler.Stage.ALARM -> SmartEscalationScheduler.Stage.CRITICAL
            SmartEscalationScheduler.Stage.CRITICAL -> null
        }
    }

    fun gapAfterMinutes(
        priority: TaskPriority,
        current: SmartEscalationScheduler.Stage,
        config: SmartEscalationConfig,
    ): Int {
        val c = config.normalized()
        return when (current) {
            SmartEscalationScheduler.Stage.SOFT -> c.notificationToVoiceMinutes
            SmartEscalationScheduler.Stage.VOICE -> c.voiceToAlarmMinutes
            SmartEscalationScheduler.Stage.ALARM -> if (priority == TaskPriority.CRITICAL) c.alarmToCriticalMinutes else 0
            SmartEscalationScheduler.Stage.CRITICAL -> 0
        }
    }

    /**
     * Recovery must not keep postponing an already rebuilt pending stage every time the app resumes.
     * Prefer the authoritative future pending time, then the original planned time, and only then
     * apply the recovery fallback delay.
     */
    fun recoveredStageAtMillis(
        plannedAtMillis: Long,
        preservedPendingAtMillis: Long?,
        nowMillis: Long,
        fallbackDelayMillis: Long,
    ): Long = when {
        preservedPendingAtMillis != null && preservedPendingAtMillis > nowMillis -> preservedPendingAtMillis
        plannedAtMillis > nowMillis -> plannedAtMillis
        else -> nowMillis + fallbackDelayMillis.coerceAtLeast(1L)
    }
}
