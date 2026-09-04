package com.framebynavin.app.reminders

import com.framebynavin.app.data.TaskPriority

/** Pure Smart V2 timing/sequence rules. Kept Android-free so CI can unit-test it. */
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

    fun availableWindowMinutes(reminderAtMillis: Long, dueAtMillis: Long): Int {
        if (reminderAtMillis <= 0L || dueAtMillis <= reminderAtMillis) return 0
        return ((dueAtMillis - reminderAtMillis) / 60_000L).toInt().coerceAtLeast(0)
    }

    fun isWindowValid(
        priority: TaskPriority,
        reminderAtMillis: Long,
        dueAtMillis: Long,
        config: SmartEscalationConfig,
    ): Boolean {
        if (reminderAtMillis <= 0L) return false
        if (priority == TaskPriority.NORMAL) return true
        return availableWindowMinutes(reminderAtMillis, dueAtMillis) >= requiredWindowMinutes(priority, config)
    }

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
}
